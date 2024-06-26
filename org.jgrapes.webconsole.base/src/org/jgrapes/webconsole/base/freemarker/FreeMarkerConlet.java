/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.base.freemarker;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferWriter;
import org.jgrapes.webconsole.base.AbstractConlet;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.RenderSupport;
import org.jgrapes.webconsole.base.ResourceByProducer;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.ConletResourceRequest;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;

/**
 * 
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public abstract class FreeMarkerConlet<S> extends AbstractConlet<S> {

    @SuppressWarnings({ "PMD.VariableNamingConventions",
        "PMD.FieldNamingConventions" })
    private static final Pattern templatePattern
        = Pattern.compile(".*\\.ftl\\.[a-z]+$");

    private Configuration fmConfig;
    private Map<String, Object> fmModel;

    /**
     * Creates a new component that listens for new events
     * on the given channel.
     * 
     * @param componentChannel
     */
    public FreeMarkerConlet(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Like {@link #FreeMarkerConlet(Channel)}, but supports
     * the specification of channel replacements.
     * 
     * @param componentChannel
     * @param channelReplacements the channel replacements (see
     * {@link Component})
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public FreeMarkerConlet(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

    /**
     * Create the base freemarker configuration.
     *
     * @return the configuration
     */
    protected Configuration freemarkerConfig() {
        if (fmConfig == null) {
            fmConfig = new Configuration(Configuration.VERSION_2_3_26);
            fmConfig.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), getClass().getPackage()
                    .getName().replace('.', '/'));
            fmConfig.setDefaultEncoding("utf-8");
            fmConfig.setTemplateExceptionHandler(
                TemplateExceptionHandler.RETHROW_HANDLER);
            fmConfig.setLogTemplateExceptions(false);
        }
        return fmConfig;
    }

    /**
     * Creates the request independent part of the freemarker model. The
     * result is cached as unmodifiable map as it can safely be assumed
     * that the render support does not change for a given web console.
     * 
     * This model provides:
     *  * The function `conletResource` that makes 
     *    {@link RenderSupport#conletResource(String, java.net.URI)}
     *    available in the template. The first argument is set to the name
     *    of the web console component, only the second must be supplied 
     *    when the function is invoked in a template.
     * 
     * @param renderSupport the render support from the web console
     * @return the result
     */
    protected Map<String, Object> fmTypeModel(RenderSupport renderSupport) {
        if (fmModel == null) {
            fmModel = new HashMap<>();
            fmModel.put("conletResource", new TemplateMethodModelEx() {
                @Override
                @SuppressWarnings("PMD.AvoidDuplicateLiterals")
                public Object exec(@SuppressWarnings("rawtypes") List arguments)
                        throws TemplateModelException {
                    @SuppressWarnings("unchecked")
                    List<TemplateModel> args = (List<TemplateModel>) arguments;
                    if (!(args.get(0) instanceof SimpleScalar)) {
                        throw new TemplateModelException("Not a string.");
                    }
                    return renderSupport.conletResource(
                        FreeMarkerConlet.this.getClass().getName(),
                        ((SimpleScalar) args.get(0)).getAsString())
                        .getRawPath();
                }
            });
            fmModel = Collections.unmodifiableMap(fmModel);
        }
        return fmModel;
    }

    /**
     * Build a freemarker model holding the information associated 
     * with the session.
     * 
     * This model provides:
     *  * The `locale` (of type {@link Locale}).
     *  * The `resourceBundle` (of type {@link ResourceBundle}).
     *  * A function `_` that looks up the given key in the web console 
     *    component's resource bundle.
     *  * A function `supportedLanguages` that returns a {@link Map}
     *    with language identifiers as keys and {@link LanguageInfo}
     *    instances as values (derived from 
     *    {@link AbstractConlet#supportedLocales()}).
     *    
     * @param session the session
     * @return the model
     */
    protected Map<String, Object> fmSessionModel(Session session) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, Object> model = new HashMap<>();
        Locale locale = session.locale();
        model.put("locale", locale);
        final ResourceBundle resourceBundle = resourceBundle(locale);
        model.put("resourceBundle", resourceBundle);
        model.put("_", new TemplateMethodModelEx() {
            @Override
            public Object exec(@SuppressWarnings("rawtypes") List arguments)
                    throws TemplateModelException {
                @SuppressWarnings("unchecked")
                List<TemplateModel> args = (List<TemplateModel>) arguments;
                if (!(args.get(0) instanceof SimpleScalar)) {
                    throw new TemplateModelException("Not a string.");
                }
                String key = ((SimpleScalar) args.get(0)).getAsString();
                try {
                    return resourceBundle.getString(key);
                } catch (MissingResourceException e) { // NOPMD
                    // no luck
                }
                return key;
            }
        });
        // Add supported languages
        model.put("supportedLanguages", new TemplateMethodModelEx() {
            private Object cachedResult;

            @Override
            public Object exec(@SuppressWarnings("rawtypes") List arguments)
                    throws TemplateModelException {
                if (cachedResult == null) {
                    cachedResult = supportedLocales().entrySet().stream().map(
                        entry -> new LanguageInfo(entry.getKey(),
                            entry.getValue()))
                        .toArray(size -> new LanguageInfo[size]);
                }
                return cachedResult;
            }
        });
        return model;
    }

    /**
     * Build a freemarker model for the current request.
     * 
     * This model provides:
     *  * The `event` property (of type {@link RenderConletRequest}).
     *  * The `conletType` property (of type {@link String}).
     *  * The `conletId` property (of type {@link String}).
     *  * The `conlet` property with the conlet's state (if not `null`).
     *  * The function `_Id(String base)` that creates a unique
     *    id for an HTML element by appending the web console component 
     *    id to the provided base.
     *  * The `conletProperties` which are the properties from 
     *    an {@link AddConletRequest}, or an empty map.
     *
     * @param event the event
     * @param channel the channel
     * @param conletId the conlet id
     * @param conletState the conlet's state information
     * @return the model
     */
    protected Map<String, Object> fmConletModel(Event<?> event,
            IOSubchannel channel, String conletId, Object conletState) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, Object> model = new HashMap<>();
        model.put("event", event);
        model.put("conletType", type());
        model.put("conletId", conletId);
        if (conletState != null) {
            model.put("conlet", conletState);
        }
        model.put("_id", new TemplateMethodModelEx() {
            @Override
            public Object exec(@SuppressWarnings("rawtypes") List arguments)
                    throws TemplateModelException {
                @SuppressWarnings("unchecked")
                List<TemplateModel> args = (List<TemplateModel>) arguments;
                if (!(args.get(0) instanceof SimpleScalar)) {
                    throw new TemplateModelException("Not a string.");
                }
                return ((SimpleScalar) args.get(0)).getAsString()
                    + "-" + conletId;
            }
        });
        if (event instanceof AddConletRequest) {
            model.put("conletProperties",
                ((AddConletRequest) event).properties());
        } else {
            model.put("conletProperties", Collections.emptyMap());
        }
        return model;
    }

    /**
     * Build a freemarker model that combines {@link #fmTypeModel},
     * {@link #fmSessionModel} and {@link #fmConletModel}. 
     *
     * @param event the event
     * @param channel the channel
     * @param conletId the conlet id
     * @param conletState the conlet's state information
     * @return the model
     */
    protected Map<String, Object> fmModel(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId, Object conletState) {
        return fmModel(event, event.renderSupport(), channel, conletId,
            conletState);
    }

    /**
     * Build a freemarker model that combines {@link #fmTypeModel},
     * {@link #fmSessionModel} and {@link #fmConletModel}. 
     *
     * @param event the event
     * @param channel the channel
     * @param conletId the conlet id
     * @param conletState the conlet's state information
     * @return the model
     */
    protected Map<String, Object> fmModel(NotifyConletModel event,
            ConsoleConnection channel, String conletId, Object conletState) {
        return fmModel(event, event.renderSupport(), channel, conletId,
            conletState);
    }

    private Map<String, Object> fmModel(Event<?> event,
            RenderSupport renderSupport, ConsoleConnection channel,
            String conletId, Object conletState) {
        final Map<String, Object> model
            = fmSessionModel(channel.session());
        model.put("locale", channel.locale());
        model.putAll(fmTypeModel(renderSupport));
        model.putAll(fmConletModel(event, channel, conletId, conletState));
        return model;
    }

    /**
     * Checks if the path of the requested resource ends with
     * `*.ftl.*`. If so, processes the template with the
     * {@link #fmTypeModel(RenderSupport)} and 
     * {@link #fmSessionModel(Session)} and
     * sends the result. Else, invoke the super class' method. 
     * 
     * @param event the event. The result will be set to
     * `true` on success
     * @param channel the channel
     */
    @Override
    protected void doGetResource(ConletResourceRequest event,
            IOSubchannel channel) {
        if (!templatePattern.matcher(event.resourceUri().getPath()).matches()) {
            super.doGetResource(event, channel);
            return;
        }
        try {
            // Prepare template
            final Template tpl = freemarkerConfig().getTemplate(
                event.resourceUri().getPath());
            Map<String, Object> model = fmSessionModel(event.session());
            model.putAll(fmTypeModel(event.renderSupport()));

            // Everything successfully prepared
            event.setResult(new ResourceByProducer(event,
                c -> {
                    try {
                        tpl.process(model, new ByteBufferWriter(c));
                    } catch (TemplateException e) {
                        throw new IOException(e);
                    }
                }, HttpResponse.contentType(event.resourceUri()),
                Instant.now(), 0));
            event.stop();
        } catch (IOException e) { // NOPMD
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns a future string providing the result
     * from processing the given template with the given data.
     * 
     * The method delegates to
     * {@link #processTemplate(Event, Template, Object)}. The version with
     * this signature is kept for backward compatibility.
     *
     * @param request the request, used to obtain the
     * {@link ExecutorService} service related with the request being
     * processed
     * @param template the template
     * @param dataModel the data model
     * @return the future
     */
    public Future<String> processTemplate(RenderConletRequestBase<?> request,
            Template template, Object dataModel) {
        return processTemplate((Event<?>) request, template, dataModel);
    }

    /**
     * Returns a future string providing the result
     * from processing the given template with the given data. 
     *
     * @param request the request, used to obtain the
     * {@link ExecutorService} service related with the request being
     * processed
     * @param template the template
     * @param dataModel the data model
     * @return the future
     */
    public Future<String> processTemplate(Event<?> request, Template template,
            Object dataModel) {
        return request.processedBy().map(EventPipeline::executorService)
            .orElse(Components.defaultExecutorService()).submit(() -> {
                StringWriter out = new StringWriter();
                try {
                    template.process(dataModel, out);
                } catch (TemplateException | IOException e) {
                    throw new IllegalArgumentException(e);
                }
                return out.toString();
            });
    }
}
