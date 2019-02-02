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

package org.jgrapes.portlets.markdowndisplay;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
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
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalUtils;
import org.jgrapes.portal.base.Portlet.RenderMode;
import org.jgrapes.portal.base.UserPrincipal;
import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.NotifyPortletModel;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.events.UpdatePortletModel;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;

import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * A portlet used to display information to the user. Instances
 * may be used as a kind of note, i.e. created and configured by
 * a user himself. A typical use case, however, is to create
 * an instance during startup by a portal policy.
 */
@SuppressWarnings("PMD.DataClass")
public class MarkdownDisplayPortlet extends FreeMarkerPortlet {

    /** Property for forcing a portlet id (used for singleton instaces). */
    public static final String PORTLET_ID = "PortletId";
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
    public MarkdownDisplayPortlet(Channel componentChannel) {
        super(componentChannel, false);
    }

    private String storagePath(Session session) {
        return "/" + PortalUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/portlets/" + MarkdownDisplayPortlet.class.getName() + "/";
    }

    /**
     * On {@link PortalReady}, fire the {@link AddPortletType}.
     *
     * @param event the event
     * @param portalSession the portal session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onPortalReady(PortalReady event, PortalSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
        // Add MarkdownDisplayPortlet resources to page
        portalSession.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
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
                .setScriptUri(event.renderSupport().portletResource(
                    type(), "MarkdownDisplay-functions.ftl.js")))
            .addCss(event.renderSupport(), PortalUtils.uriFromPath(
                "MarkdownDisplay-style.css"))
            .setInstantiable());
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(portalSession.browserSession()), portalSession);
        fire(query, portalSession);
    }

    /**
     * Restore portlet information, if contained in the event.
     *
     * @param event the event
     * @param channel the channel
     * @throws JsonDecodeException the json decode exception
     */
    @Handler
    public void onKeyValueStoreData(
            KeyValueStoreData event, PortalSession channel)
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
     * Adds the portlet to the portal. The portlet supports the 
     * following options (see {@link AddPortletRequest#properties()}:
     * 
     * * `PORTLET_ID` (String): The portlet id.
     * 
     * * `TITLE` (String): The portlet title.
     * 
     * * `PREVIEW_SOURCE` (String): The markdown source that is rendered 
     *   in the portlet preview.
     * 
     * * `VIEW_SOURCE` (String): The markdown source that is rendered 
     *   in the portlet view.
     * 
     * * `DELETABLE` (Boolean): Indicates that the portlet may be 
     *   deleted from the overview page.
     * 
     * * `EDITABLE_BY` (Set&lt;Principal&gt;): The principals that may edit 
     *   the portlet instance.
     */
    @Override
    public String doAddPortlet(AddPortletRequest event,
            PortalSession portalSession) throws Exception {
        ResourceBundle resourceBundle = resourceBundle(portalSession.locale());

        // Create new model
        String portletId = (String) event.properties().get(PORTLET_ID);
        if (portletId == null) {
            portletId = generatePortletId();
        }
        MarkdownDisplayModel model = putInSession(
            portalSession.browserSession(),
            new MarkdownDisplayModel(portletId));
        model.setTitle((String) event.properties().getOrDefault(TITLE,
            resourceBundle.getString("portletName")));
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
        portalSession.respond(new KeyValueStoreUpdate().update(
            storagePath(portalSession.browserSession()) + model.getPortletId(),
            jsonState));

        // Send HTML
        renderPortlet(event, portalSession, model);
        return portletId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
     */
    @Override
    protected void doRenderPortlet(RenderPortletRequest event,
            PortalSession portalSession, String portletId,
            Serializable retrievedState) throws Exception {
        MarkdownDisplayModel model = (MarkdownDisplayModel) retrievedState;
        renderPortlet(event, portalSession, model);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession portalSession, MarkdownDisplayModel model)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        Set<RenderMode> modes = renderModes(model);
        if (model.getViewContent() != null
            && !model.getViewContent().isEmpty()) {
            modes.add(RenderMode.View);
        }
        switch (event.renderMode()) {
        case Preview:
        case DeleteablePreview: {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-preview.ftl.html");
            portalSession.respond(new RenderPortletFromTemplate(event,
                MarkdownDisplayPortlet.class, model.getPortletId(),
                tpl, fmModel(event, portalSession, model))
                    .setRenderMode(event.renderMode()).setSupportedModes(modes)
                    .setForeground(event.isForeground()));
            updateView(portalSession, model);
            break;
        }
        case View: {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-view.ftl.html");
            portalSession.respond(new RenderPortletFromTemplate(event,
                MarkdownDisplayPortlet.class, model.getPortletId(),
                tpl, fmModel(event, portalSession, model))
                    .setRenderMode(RenderMode.View).setSupportedModes(modes)
                    .setForeground(event.isForeground()));
            updateView(portalSession, model);
            break;
        }
        case Edit: {
            Template tpl = freemarkerConfig()
                .getTemplate("MarkdownDisplay-edit.ftl.html");
            portalSession.respond(new RenderPortletFromTemplate(event,
                MarkdownDisplayPortlet.class, model.getPortletId(),
                tpl, fmModel(event, portalSession, model))
                    .setRenderMode(RenderMode.Edit).setSupportedModes(modes));
            break;
        }
        default:
            break;
        }
    }

    private Set<RenderMode> renderModes(MarkdownDisplayModel model) {
        Set<RenderMode> modes = new HashSet<>();
        modes.add(model.isDeletable() ? RenderMode.DeleteablePreview
            : RenderMode.Preview);
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
        channel.respond(new NotifyPortletView(type(),
            model.getPortletId(), "updateAll", model.getTitle(),
            model.getPreviewContent(), model.getViewContent(),
            renderModes(model)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
     */
    @Override
    protected void doDeletePortlet(DeletePortletRequest event,
            PortalSession channel, String portletId,
            Serializable retrievedState) throws Exception {
        channel.respond(new KeyValueStoreUpdate().delete(
            storagePath(channel.browserSession()) + portletId));
        channel.respond(new DeletePortlet(portletId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doNotifyPortletModel
     */
    @Override
    protected void doNotifyPortletModel(NotifyPortletModel event,
            PortalSession portalSession, Serializable portletState)
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
        fire(new UpdatePortletModel(event.portletId(), properties),
            portalSession);
    }

    /**
     * Stores the modified properties using a {@link KeyValueStoreUpdate}
     * event and updates the view with a {@link NotifyPortletView}. 
     *
     * @param event the event
     * @param portalSession the portal session
     */
    @SuppressWarnings("unchecked")
    @Handler
    public void onUpdatePortletModel(UpdatePortletModel event,
            PortalSession portalSession) {
        stateFromSession(portalSession.browserSession(), event.portletId(),
            MarkdownDisplayModel.class).ifPresent(model -> {
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
                    portalSession.respond(new KeyValueStoreUpdate().update(
                        storagePath(portalSession.browserSession())
                            + model.getPortletId(),
                        jsonState));
                    updateView(portalSession, model);
                } catch (IOException e) { // NOPMD
                    // Won't happen, uses internal writer
                }
            });
    }

    /**
     * The portlet's model.
     */
    @SuppressWarnings("serial")
    public static class MarkdownDisplayModel extends PortletBaseModel {

        private String title = "";
        private String previewContent = "";
        private String viewContent = "";
        private boolean deletable = true;
        private Set<Principal> editableBy;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public MarkdownDisplayModel(String portletId) {
            super(portletId);
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
