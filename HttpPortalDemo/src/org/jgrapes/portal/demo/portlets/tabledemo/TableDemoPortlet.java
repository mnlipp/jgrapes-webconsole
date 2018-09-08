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

package org.jgrapes.portal.demo.portlets.tabledemo;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.Portlet.RenderMode;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;
import org.jgrapes.portal.base.freemarker.FreeMarkerPortlet;

/**
 * 
 */
public class TableDemoPortlet extends FreeMarkerPortlet {

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
    public TableDemoPortlet(Channel componentChannel) {
        super(componentChannel, false);
    }

    @Handler
    public void onPortalReady(PortalReady event, PortalSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
        // Add HelloWorldPortlet resources to page
        portalSession.respond(new AddPortletType(type())
            .setDisplayName(resourceBundle.getString("portletName"))
            .setInstantiable());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
     */
    @Override
    protected String generatePortletId() {
        return type() + "-" + super.generatePortletId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#modelFromSession
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <T extends Serializable> Optional<T> stateFromSession(
            Session session, String portletId, Class<T> type) {
        if (portletId.startsWith(type() + "-")) {
            return Optional.of((T) new TableDemoModel(portletId));
        }
        return Optional.empty();
    }

    @Override
    public String doAddPortlet(AddPortletRequest event,
            PortalSession channel) throws Exception {
        String portletId = generatePortletId();
        TableDemoModel portletModel = putInSession(
            channel.browserSession(), new TableDemoModel(portletId));
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
        TableDemoModel portletModel = (TableDemoModel) retrievedState;
        renderPortlet(event, channel, portletModel);
    }

    private void renderPortlet(RenderPortletRequestBase<?> event,
            PortalSession channel, TableDemoModel portletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        switch (event.renderMode()) {
        case Preview:
        case DeleteablePreview: {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-preview.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                TableDemoPortlet.class, portletModel.getPortletId(),
                tpl, fmModel(event, channel, portletModel))
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            break;
        }
        case View: {
            Template tpl
                = freemarkerConfig().getTemplate("TableDemo-view.ftl.html");
            channel.respond(new RenderPortletFromTemplate(event,
                TableDemoPortlet.class, portletModel.getPortletId(),
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
        channel.respond(new DeletePortlet(portletId));
    }

    @SuppressWarnings("serial")
    public static class TableDemoModel extends PortletBaseModel {

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public TableDemoModel(String portletId) {
            super(portletId);
        }

    }

}
