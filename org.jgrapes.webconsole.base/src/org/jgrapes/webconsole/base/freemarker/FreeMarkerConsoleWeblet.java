/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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
import java.util.ArrayList;
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
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.webconsole.base.ConsoleWeblet;

/**
 * 
 */
public abstract class FreeMarkerConsoleWeblet extends ConsoleWeblet {

    public static final String UTF_8 = "utf-8";
    /**
     * Initialized with a base FreeMarker configuration.
     */
    protected Configuration freeMarkerConfig;

    /**
     * Instantiates a new free marker console weblet.
     *
     * @param webletChannel the weblet channel
     * @param consoleChannel the console channel
     * @param consolePrefix the console prefix
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public FreeMarkerConsoleWeblet(Channel webletChannel,
            Channel consoleChannel,
            URI consolePrefix) {
        super(webletChannel, consoleChannel, consolePrefix);
        freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_26);
        List<TemplateLoader> loaders = new ArrayList<>();
        Class<?> clazz = getClass();
        while (!clazz.equals(FreeMarkerConsoleWeblet.class)) {
            loaders.add(new ClassTemplateLoader(clazz.getClassLoader(),
                clazz.getPackage().getName().replace('.', '/')));
            clazz = clazz.getSuperclass();
        }
        freeMarkerConfig.setTemplateLoader(
            new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));
        freeMarkerConfig.setDefaultEncoding(UTF_8);
        freeMarkerConfig.setTemplateExceptionHandler(
            TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarkerConfig.setLogTemplateExceptions(false);
    }

    /**
     * Prepend a class template loader to the list of loaders 
     * derived from the class hierarchy.
     *
     * @param classloader the class loader
     * @param path the path
     * @return the free marker console weblet
     */
    public FreeMarkerConsoleWeblet
            prependClassTemplateLoader(ClassLoader classloader, String path) {
        List<TemplateLoader> loaders = new ArrayList<>();
        loaders.add(new ClassTemplateLoader(classloader, path));
        MultiTemplateLoader oldLoader
            = (MultiTemplateLoader) freeMarkerConfig.getTemplateLoader();
        for (int i = 0; i < oldLoader.getTemplateLoaderCount(); i++) {
            loaders.add(oldLoader.getTemplateLoader(i));
        }
        freeMarkerConfig.setTemplateLoader(
            new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));
        return this;
    }

    /**
     * Convenience version of 
     * {@link #prependClassTemplateLoader(ClassLoader, String)} that derives
     * the path from the class's package name.
     *
     * @param clazz the clazz
     * @return the free marker console weblet
     */
    public FreeMarkerConsoleWeblet prependClassTemplateLoader(Class<?> clazz) {
        return prependClassTemplateLoader(clazz.getClassLoader(),
            clazz.getPackage().getName().replace('.', '/'));
    }

    /**
     * Creates the console base model.
     *
     * @return the base model
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    protected Map<String, Object> createConsoleBaseModel() {
        // Create console model
        Map<String, Object> consoleModel = new HashMap<>();
        consoleModel.put("renderSupport", renderSupport());
        consoleModel.put("useMinifiedResources", useMinifiedResources());
        consoleModel.put("minifiedExtension",
            useMinifiedResources() ? ".min" : "");
        consoleModel.put(
            "consoleSessionRefreshInterval", consoleSessionRefreshInterval());
        consoleModel.put(
            "consoleSessionInactivityTimeout",
            consoleSessionInactivityTimeout());
        return consoleModel;
    }

    /**
     * Expand the given console model with data from the event 
     * (console session id, locale information).
     *
     * @param model the model
     * @param event the event
     * @param consoleSessionId the console session id
     * @return the map
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected Map<String, Object> expandConsoleModel(
            Map<String, Object> model, Request.In.Get event,
            UUID consoleSessionId) {
        // WebConsole Session UUID
        model.put("consoleSessionId", consoleSessionId.toString());

        // Add locale
        final Locale locale = event.associated(Selection.class).map(
            sel -> sel.get()[0]).orElse(Locale.getDefault());
        model.put("locale", locale);

        // Add supported locales
        final Collator coll = Collator.getInstance(locale);
        final Comparator<LanguageInfo> comp
            = new Comparator<LanguageInfo>() {
                @Override
                public int compare(LanguageInfo o1, LanguageInfo o2) { // NOPMD
                    return coll.compare(o1.getLabel(), o2.getLabel());
                }
            };
        LanguageInfo[] languages = supportedLocales().entrySet().stream()
            .map(entry -> new LanguageInfo(entry.getKey(), entry.getValue()))
            .sorted(comp).toArray(size -> new LanguageInfo[size]);
        model.put("supportedLanguages", languages);

        final ResourceBundle baseResources = consoleResourceBundle(locale);
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
    protected void renderConsole(Request.In.Get event, IOSubchannel channel,
            UUID consoleSessionId) throws IOException, InterruptedException {
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

            Template tpl = freeMarkerConfig.getTemplate("console.ftl.html");
            Map<String, Object> consoleModel = expandConsoleModel(
                createConsoleBaseModel(), event, consoleSessionId);
            tpl.process(consoleModel, out);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    /**
    * Holds the information about the selected language.
    */
    public static class LanguageInfo {
        private final Locale locale;
        private final ResourceBundle bundle;

        /**
         * Instantiates a new language info.
         *
         * @param locale the locale
         */
        public LanguageInfo(Locale locale, ResourceBundle bundle) {
            this.locale = locale;
            this.bundle = bundle;
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

        /**
         * Gets the bundle.
         *
         * @return the bundle
         */
        public ResourceBundle getL10nBundle() {
            return bundle;
        }
    }

}
