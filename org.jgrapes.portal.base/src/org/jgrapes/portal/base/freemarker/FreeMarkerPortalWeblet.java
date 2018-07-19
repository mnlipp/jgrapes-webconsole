/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.portal.base.freemarker;

import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.portal.base.PortalWeblet;

/**
 * 
 */
public abstract class FreeMarkerPortalWeblet extends PortalWeblet {

    public static final String UTF_8 = "utf-8";
    /**
     * Initialized with a base FreeMarker configuration.
     */
    protected Configuration freeMarkerConfig;

    /**
     * Instantiates a new free marker portal weblet.
     *
     * @param webletChannel the weblet channel
     * @param portalChannel the portal channel
     * @param portalPrefix the portal prefix
     */
    public FreeMarkerPortalWeblet(Channel webletChannel, Channel portalChannel,
            URI portalPrefix) {
        super(webletChannel, portalChannel, portalPrefix);
        freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_26);
        freeMarkerConfig.setClassLoaderForTemplateLoading(
            getClass().getClassLoader(),
            getClass().getPackage().getName().replace('.', '/'));
        freeMarkerConfig.setDefaultEncoding(UTF_8);
        freeMarkerConfig.setTemplateExceptionHandler(
            TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarkerConfig.setLogTemplateExceptions(false);
    }

    protected Map<String, Object> createPortalBaseModel() {
        // Create portal model
        Map<String, Object> portalModel = new HashMap<>();
        portalModel.put("renderSupport", renderSupport());
        portalModel.put("useMinifiedResources", useMinifiedResources());
        portalModel.put("minifiedExtension",
            useMinifiedResources() ? ".min" : "");
        portalModel.put(
            "portalSessionRefreshInterval", portalSessionRefreshInterval());
        portalModel.put(
            "portalSessionInactivityTimeout", portalSessionInactivityTimeout());
        return portalModel;
    }

    protected Map<String, Object> expandPortalModel(
            Map<String, Object> model, GetRequest event,
            UUID portalSessionId) {
        // Portal Session UUID
        model.put("portalSessionId", portalSessionId.toString());

        // Add locale
        final Locale locale = event.associated(Selection.class).map(
            sel -> sel.get()[0]).orElse(Locale.getDefault());
        model.put("locale", locale);

        // Add supported locales
        final Collator coll = Collator.getInstance(locale);
        final Comparator<LanguageInfo> comp
            = new Comparator<LanguageInfo>() {
                @Override
                public int compare(LanguageInfo o1, LanguageInfo o2) {
                    return coll.compare(o1.getLabel(), o2.getLabel());
                }
            };
        LanguageInfo[] languages = supportedLocales().stream()
            .map(lang -> new LanguageInfo(lang))
            .sorted(comp).toArray(size -> new LanguageInfo[size]);
        model.put("supportedLanguages", languages);

        // Add localization
        final ResourceBundle additionalResources
            = resourceBundleSupplier() == null
                ? null
                : resourceBundleSupplier().apply(locale);
        final ResourceBundle baseResources = ResourceBundle.getBundle(
            getClass().getPackage().getName() + ".l10n", locale,
            ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT));
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
                    return additionalResources.getString(key);
                } catch (MissingResourceException e) { // NOPMD
                    // try base resources
                }
                try {
                    return baseResources.getString(key);
                } catch (MissingResourceException e) { // NOPMD
                    // no luck
                }
                return key;
            }
        });
        return model;
    }

    @Override
    protected void renderPortal(GetRequest event, IOSubchannel channel,
            UUID portalSessionId) throws IOException, InterruptedException {
        event.setResult(true);
        event.stop();

        // Prepare response
        HttpResponse response = event.httpRequest().response().get();
        MediaType mediaType = MediaType.builder().setType("text", "html")
            .setParameter("charset", UTF_8).build();
        response.setField(HttpField.CONTENT_TYPE, mediaType);
        response.setStatus(HttpStatus.OK);
        response.setHasPayload(true);
        channel.respond(new Response(response));
        try (ByteBufferOutputStream bbos = new ByteBufferOutputStream(
            channel, channel.responsePipeline());
                Writer out = new OutputStreamWriter(bbos.suppressClose(),
                    UTF_8)) {
            @SuppressWarnings("PMD.UseConcurrentHashMap")

            Template tpl = freeMarkerConfig.getTemplate("portal.ftl.html");
            Map<String, Object> portalModel = expandPortalModel(
                createPortalBaseModel(), event, portalSessionId);
            tpl.process(portalModel, out);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void providePortalResource(GetRequest event,
            String requestPath, IOSubchannel channel) {
        ResponseCreationSupport.sendStaticContent(event, channel,
            path -> getClass().getResource(requestPath),
            null);
    }

    /**
    * Holds the information about the selected language.
    */
    public static class LanguageInfo {
        private final Locale locale;

        /**
         * Instantiates a new language info.
         *
         * @param locale the locale
         */
        public LanguageInfo(Locale locale) {
            this.locale = locale;
        }

        /**
         * Gets the locale.
         *
         * @return the locale
         */
        public Locale getLocale() {
            return locale;
        }

        /**
         * Gets the label.
         *
         * @return the label
         */
        public String getLabel() {
            String str = locale.getDisplayName(locale);
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
    }

}
