/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.portal.demo.portlets.helloworld;

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
import org.jgrapes.portal.base.PortalWeblet;
import org.jgrapes.portal.base.Portlet.RenderMode;
import org.jgrapes.portal.base.UserPrincipal;
import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.DisplayNotification;
import org.jgrapes.portal.base.events.NotifyPortletModel;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;
import org.jgrapes.portal.demo.portlets.helloworld.HelloWorldPortlet;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * 
 */
public class HelloWorldPortlet extends FreeMarkerPortlet {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.DeleteablePreview, RenderMode.View);

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public HelloWorldPortlet(Channel componentChannel) {
        super(componentChannel, false);
    }

    private String storagePath(Session session) {
        return "/" + PortalUtils.userFromSession(session)
            .map(UserPrincipal::toString).orElse("")
            + "/portlets/" + HelloWorldPortlet.class.getName() + "/";
    }

    @Handler
    public void onPortalReady(PortalReady event, PortalSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
        // Add HelloWorldPortlet resources to page
        portalSession.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
            .addScript(new ScriptResource().setScriptUri(
                event.renderSupport().portletResource(type(),
                    "HelloWorld-functions.js")))
            .addCss(event.renderSupport(), PortalWeblet.uriFromPath(
                "HelloWorld-style.css"))
            .setInstantiable());
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
            HelloWorldModel model = JsonBeanDecoder.create(json)
                .readObject(HelloWorldModel.class);
            putInSession(channel.browserSession(), model);
        }
    }

    @Override
    public String doAddPortlet(AddPortletRequest event,
            PortalSession channel) throws Exception {
        String portletId = generatePortletId();
        HelloWorldModel portletModel = putInSession(
            channel.browserSession(), new HelloWorldModel(portletId));
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
        HelloWorldModel portletModel = (HelloWorldModel) retrievedState;
        renderPortlet(event, channel, portletModel);
    }

    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel, HelloWorldModel portletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        switch (event.renderMode()) {
        case Preview:
        case DeleteablePreview: {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
            channel.respond(new RenderPortletFromTemplate(event,
                HelloWorldPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            break;
        }
        case View: {
            Template tpl
                = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
            channel.respond(new RenderPortletFromTemplate(event,
                HelloWorldPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            channel.respond(new NotifyPortletView(type(),
                portletModel.getPortletId(), "setWorldVisible",
                portletModel.isWorldVisible()));
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

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doNotifyPortletModel
     */
    @Override
    protected void doNotifyPortletModel(NotifyPortletModel event,
            PortalSession channel, Serializable portletState)
            throws Exception {
        event.stop();
        HelloWorldModel portletModel = (HelloWorldModel) portletState;
        portletModel.setWorldVisible(!portletModel.isWorldVisible());

        String jsonState = JsonBeanEncoder.create()
            .writeObject(portletModel).toJson();
        channel.respond(new KeyValueStoreUpdate().update(
            storagePath(channel.browserSession()) + portletModel.getPortletId(),
            jsonState));
        channel.respond(new NotifyPortletView(type(),
            portletModel.getPortletId(), "setWorldVisible",
            portletModel.isWorldVisible()));
        channel.respond(new DisplayNotification("<span>"
            + resourceBundle(channel.locale()).getString("visibilityChange")
            + "</span>")
                .addOption("autoClose", 2000));
    }

    @SuppressWarnings("serial")
    public static class HelloWorldModel extends PortletBaseModel {

        private boolean worldVisible = true;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public HelloWorldModel(String portletId) {
            super(portletId);
        }

        /**
         * @param visible the visible to set
         */
        public void setWorldVisible(boolean visible) {
            this.worldVisible = visible;
        }

        public boolean isWorldVisible() {
            return worldVisible;
        }
    }

}
