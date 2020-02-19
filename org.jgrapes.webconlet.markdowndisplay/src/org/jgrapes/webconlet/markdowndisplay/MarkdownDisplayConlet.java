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

package org.jgrapes.webconlet.markdowndisplay;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.AbstractConlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.UserPrincipal;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConletDeleted;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.UpdateConletModel;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * A web console component used to display information to the user. Instances
 * may be used as a kind of note, i.e. created and configured by
 * a user himself. A typical use case, however, is to create
 * an instance during startup by a web console policy.
 */
@SuppressWarnings("PMD.DataClass")
public class MarkdownDisplayConlet
        extends
        FreeMarkerConlet<MarkdownDisplayConlet.MarkdownDisplayModel> {

    /** Property for forcing a conlet id (used for singleton instances). */
    public static final String CONLET_ID = "ConletId";
    /** Property for setting a title. */
    public static final String TITLE = "Title";
    /** Property for setting the preview source. */
    public static final String PREVIEW_SOURCE = "PreviewSource";
    /** Property for setting the view source. */
    public static final String VIEW_SOURCE = "ViewSource";
    /** Boolean property that controls if the preview is deletable. */
    public static final String DELETABLE = "Deletable";
    /** Property of type `Set<Principal>` for restricting who 
     * can edit the content. */
    public static final String EDITABLE_BY = "EditableBy";

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public MarkdownDisplayConlet(Channel componentChannel) {
        super(componentChannel);
    }

    private String storagePath(Session session) {
        return "/" + WebConsoleUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/conlets/" + MarkdownDisplayConlet.class.getName() + "/";
    }

    /**
     * On {@link ConsoleReady}, fire the {@link AddConletType}.
     *
     * @param event the event
     * @param consoleSession the console session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add MarkdownDisplayConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(consoleSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io",
                    "github.com/markdown-it/markdown-it-abbr",
                    "github.com/markdown-it/markdown-it-container",
                    "github.com/markdown-it/markdown-it-deflist",
                    "github.com/markdown-it/markdown-it-emoji",
                    "github.com/markdown-it/markdown-it-footnote",
                    "github.com/markdown-it/markdown-it-ins",
                    "github.com/markdown-it/markdown-it-mark",
                    "github.com/markdown-it/markdown-it-sub",
                    "github.com/markdown-it/markdown-it-sup" })
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "MarkdownDisplay-functions.ftl.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "MarkdownDisplay-style.css")));
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(consoleSession.browserSession()), consoleSession);
        fire(query, consoleSession);
    }

    /**
     * Restore web console component information, if contained in the event.
     *
     * @param event the event
     * @param channel the channel
     * @throws JsonDecodeException the json decode exception
     */
    @Handler
    public void onKeyValueStoreData(
            KeyValueStoreData event, ConsoleSession channel)
            throws JsonDecodeException {
        if (!event.event().query()
            .equals(storagePath(channel.browserSession()))) {
            return;
        }
        for (String json : event.data().values()) {
            MarkdownDisplayModel model = JsonBeanDecoder.create(json)
                .readObject(MarkdownDisplayModel.class);
            putInSession(channel.browserSession(), model);
        }
    }

    /**
     * Adds the web console component to the console. The web console 
     * component supports the 
     * following options (see {@link AddConletRequest#properties()}:
     * 
     * * `CONLET_ID` (String): The web console component id.
     * 
     * * `TITLE` (String): The web console component title.
     * 
     * * `PREVIEW_SOURCE` (String): The markdown source that is rendered 
     *   in the web console component preview.
     * 
     * * `VIEW_SOURCE` (String): The markdown source that is rendered 
     *   in the web console component view.
     * 
     * * `DELETABLE` (Boolean): Indicates that the web console component may be 
     *   deleted from the overview page.
     * 
     * * `EDITABLE_BY` (Set&lt;Principal&gt;): The principals that may edit 
     *   the web console component instance.
     */
    @Override
    public ConletTrackingInfo doAddConlet(AddConletRequest event,
            ConsoleSession consoleSession) throws Exception {
        ResourceBundle resourceBundle = resourceBundle(consoleSession.locale());

        // Create new model
        String conletId = (String) event.properties().get(CONLET_ID);
        if (conletId == null) {
            conletId = generateConletId();
        }
        MarkdownDisplayModel model = putInSession(
            consoleSession.browserSession(),
            new MarkdownDisplayModel(conletId));
        model.setTitle((String) event.properties().getOrDefault(TITLE,
            resourceBundle.getString("conletName")));
        model.setPreviewContent((String) event.properties().getOrDefault(
            PREVIEW_SOURCE, ""));
        model.setViewContent((String) event.properties().getOrDefault(
            VIEW_SOURCE, ""));
        model.setDeletable((Boolean) event.properties().getOrDefault(
            DELETABLE, Boolean.TRUE));
        @SuppressWarnings("unchecked")
        Set<Principal> editableBy = (Set<Principal>) event.properties().get(
            EDITABLE_BY);
        model.setEditableBy(editableBy);

        // Save model
        String jsonState = JsonBeanEncoder.create()
            .writeObject(model).toJson();
        consoleSession.respond(new KeyValueStoreUpdate().update(
            storagePath(consoleSession.browserSession()) + model.getConletId(),
            jsonState));

        // Send HTML
        return new ConletTrackingInfo(conletId)
            .addModes(renderConlet(event, consoleSession, model));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequest event,
            ConsoleSession consoleSession, String conletId,
            MarkdownDisplayModel model) throws Exception {
        return renderConlet(event, consoleSession, model);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private Set<RenderMode> renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession consoleSession, MarkdownDisplayModel model)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        Set<RenderMode> supported = renderModes(model);
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-preview.ftl.html");
            consoleSession.respond(new RenderConletFromTemplate(event,
                type(), model.getConletId(),
                tpl, fmModel(event, consoleSession, model))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(supported));
            updateView(consoleSession, model);
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-view.ftl.html");
            consoleSession.respond(new RenderConletFromTemplate(event,
                type(), model.getConletId(),
                tpl, fmModel(event, consoleSession, model))
                    .setRenderAs(RenderMode.View.addModifiers(event.renderAs()))
                    .setSupportedModes(supported));
            updateView(consoleSession, model);
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.Edit)) {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-edit.ftl.html");
            consoleSession.respond(new RenderConletFromTemplate(event,
                type(), model.getConletId(),
                tpl, fmModel(event, consoleSession, model))
                    .setRenderAs(RenderMode.Edit.addModifiers(event.renderAs()))
                    .setSupportedModes(supported));
        }
        return renderedAs;
    }

    private Set<RenderMode> renderModes(MarkdownDisplayModel model) {
        Set<RenderMode> modes = new HashSet<>();
        modes.add(RenderMode.Preview);
        if (!model.isDeletable()) {
            modes.add(RenderMode.StickyPreview);
        }
        if (model.getViewContent() != null
            && !model.getViewContent().isEmpty()) {
            modes.add(RenderMode.View);
        }
        if (model.getEditableBy() == null) {
            modes.add(RenderMode.Edit);
        }
        return modes;
    }

    private void updateView(IOSubchannel channel, MarkdownDisplayModel model) {
        channel.respond(new NotifyConletView(type(),
            model.getConletId(), "updateAll", model.getTitle(),
            model.getPreviewContent(), model.getViewContent(),
            renderModes(model)));
    }

    @Override
    protected void doConletDeleted(ConletDeleted event,
            ConsoleSession channel, String conletId,
            MarkdownDisplayModel retrievedState) throws Exception {
        if (event.renderModes().isEmpty()) {
            channel.respond(new KeyValueStoreUpdate().delete(
                storagePath(channel.browserSession()) + conletId));
        }
    }

    @Override
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession consoleSession, MarkdownDisplayModel conletState)
            throws Exception {
        event.stop();
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> properties = new HashMap<>();
        if (event.params().get(0) != null) {
            properties.put(TITLE, event.params().asString(0));
        }
        if (event.params().get(1) != null) {
            properties.put(PREVIEW_SOURCE, event.params().asString(1));
        }
        if (event.params().get(2) != null) {
            properties.put(VIEW_SOURCE, event.params().asString(2));
        }
        fire(new UpdateConletModel(event.conletId(), properties),
            consoleSession);
    }

    /**
     * Stores the modified properties using a {@link KeyValueStoreUpdate}
     * event and updates the view with a {@link NotifyConletView}. 
     *
     * @param event the event
     * @param consoleSession the console session
     */
    @SuppressWarnings("unchecked")
    @Handler
    public void onUpdateConletModel(UpdateConletModel event,
            ConsoleSession consoleSession) {
        stateFromSession(consoleSession.browserSession(), event.conletId())
            .ifPresent(model -> {
                event.ifPresent(TITLE,
                    (key, value) -> model.setTitle((String) value))
                    .ifPresent(PREVIEW_SOURCE,
                        (key, value) -> model.setPreviewContent((String) value))
                    .ifPresent(VIEW_SOURCE,
                        (key, value) -> model.setViewContent((String) value))
                    .ifPresent(DELETABLE,
                        (key, value) -> model.setDeletable((Boolean) value))
                    .ifPresent(EDITABLE_BY,
                        (key, value) -> {
                            model.setEditableBy((Set<Principal>) value);
                        });
                try {
                    String jsonState = JsonBeanEncoder.create()
                        .writeObject(model).toJson();
                    consoleSession.respond(new KeyValueStoreUpdate().update(
                        storagePath(consoleSession.browserSession())
                            + model.getConletId(),
                        jsonState));
                    updateView(consoleSession, model);
                } catch (IOException e) { // NOPMD
                    // Won't happen, uses internal writer
                }
            });
    }

    /**
     * The web console component's model.
     */
    @SuppressWarnings("serial")
    public static class MarkdownDisplayModel
            extends AbstractConlet.ConletBaseModel {

        private String title = "";
        private String previewContent = "";
        private String viewContent = "";
        private boolean deletable = true;
        private Set<Principal> editableBy;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param conletId the web console component id
         */
        @ConstructorProperties({ "conletId" })
        public MarkdownDisplayModel(String conletId) {
            super(conletId);
        }

        /**
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * @param title the title to set
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @return the previewContent
         */
        public String getPreviewContent() {
            return previewContent;
        }

        /**
         * @param previewContent the previewContent to set
         */
        public void setPreviewContent(String previewContent) {
            this.previewContent = previewContent;
        }

        /**
         * @return the viewContent
         */
        public String getViewContent() {
            return viewContent;
        }

        /**
         * @param viewContent the viewContent to set
         */
        public void setViewContent(String viewContent) {
            this.viewContent = viewContent;
        }

        /**
         * @return the deletable
         */
        public boolean isDeletable() {
            return deletable;
        }

        /**
         * @param deletable the deletable to set
         */
        public void setDeletable(boolean deletable) {
            this.deletable = deletable;
        }

        /**
         * @return the editableBy
         */
        public Set<Principal> getEditableBy() {
            return editableBy;
        }

        /**
         * @param editableBy the editableBy to set
         */
        public void setEditableBy(Set<Principal> editableBy) {
            this.editableBy = editableBy;
        }

    }

}
