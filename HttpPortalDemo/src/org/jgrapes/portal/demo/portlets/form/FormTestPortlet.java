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

package org.jgrapes.portal.demo.portlets.form;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
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
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalUtils;
import org.jgrapes.portal.base.Portlet.RenderMode;
import org.jgrapes.portal.base.UserPrincipal;
import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * 
 */
public class FormTestPortlet extends FreeMarkerPortlet {

    private static final Set<RenderMode> MODES
        = RenderMode.asSet(RenderMode.View);

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public FormTestPortlet(Channel componentChannel) {
        super(componentChannel, false);
    }

    private String storagePath(Session session) {
        return "/" + PortalUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/portlets/" + FormTestPortlet.class.getName() + "/";
    }

    @Handler
    public void onPortalReady(PortalReady event, PortalSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
        // Add HelloWorldPortlet resources to page
        portalSession.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
            .addRenderMode(RenderMode.View)
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().portletResource(type(),
                    "FormTest-functions.js")))
            .addCss(event.renderSupport(), PortalUtils.uriFromPath(
                "FormTest-style.css")));
        KeyValueStoreQuery query = new KeyValueStoreQuery(
            storagePath(portalSession.browserSession()), portalSession);
        fire(query, portalSession);
    }

    @Handler
    public void onKeyValueStoreData(
            KeyValueStoreData event, PortalSession channel)
            throws JsonDecodeException {
        if (!event.event().query()
            .equals(storagePath(channel.browserSession()))) {
            return;
        }
        for (String json : event.data().values()) {
            FormTestModel model = JsonBeanDecoder.create(json)
                .readObject(FormTestModel.class);
            putInSession(channel.browserSession(), model);
        }
    }

    @Override
    public String doAddPortlet(AddPortletRequest event,
            PortalSession channel) throws Exception {
        String portletId = generatePortletId();
        FormTestModel portletModel = putInSession(
            channel.browserSession(), new FormTestModel(portletId));
        String jsonState = JsonBeanEncoder.create()
            .writeObject(portletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession()) + portletModel.getPortletId(),
            jsonState));
        renderPortlet(event, channel, portletModel);
        return portletId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
     */
    @Override
    protected void doRenderPortlet(RenderPortletRequest event,
            PortalSession channel, String portletId,
            Serializable retrievedState) throws Exception {
        FormTestModel portletModel = (FormTestModel) retrievedState;
        renderPortlet(event, channel, portletModel);
    }

    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel, FormTestModel portletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        switch (event.preferredRenderMode()) {
        case View: {
            Template tpl
                = freemarkerConfig().getTemplate("FormTest-view.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                FormTestPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            break;
        }
        default:
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
     */
    @Override
    protected void doDeletePortlet(DeletePortletRequest event,
            PortalSession channel, String portletId,
            Serializable portletState) throws Exception {
        channel.respond(new KeyValueStoreUpdate().delete(
            storagePath(channel.browserSession()) + portletId));
        channel.respond(new DeletePortlet(portletId));
    }

    @SuppressWarnings("serial")
    public static class FormTestModel extends PortletBaseModel {

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public FormTestModel(String portletId) {
            super(portletId);
        }
    }

}
