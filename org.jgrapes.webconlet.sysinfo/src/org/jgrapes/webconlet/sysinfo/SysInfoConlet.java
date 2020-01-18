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

package org.jgrapes.webconlet.sysinfo;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.webconsole.base.AbstractConlet;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteConlet;
import org.jgrapes.webconsole.base.events.DeleteConletRequest;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerComponent;

/**
 * 
 */
public class SysInfoConlet
        extends FreeMarkerComponent<SysInfoConlet.SysInfoModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.DeleteablePreview, RenderMode.View);

    /**
     * The periodically generated update event.
     */
    public static class Update extends Event<Void> {
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public SysInfoConlet(Channel componentChannel) {
        super(componentChannel);
        setPeriodicRefresh(Duration.ofSeconds(1), () -> new Update());
    }

    /**
     * On {@link ConsoleReady}, fire the {@link AddConletType}.
     *
     * @param event the event
     * @param portalSession the portal session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name
     *             exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add SysInfoConlet resources to page
        portalSession.respond(new AddConletType(type())
            .setDisplayNames(
                displayNames(portalSession.supportedLocales(), "portletName"))
            .addScript(new ScriptResource()
                .setRequires("chart.js")
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "SysInfo-functions.ftl.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "SysInfo-style.css")));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
     */
    @Override
    protected String generateConletId() {
        return type() + "-" + super.generateConletId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#modelFromSession
     */
    @Override
    protected Optional<SysInfoModel> stateFromSession(
            Session session, String portletId) {
        if (portletId.startsWith(type() + "-")) {
            return Optional.of(new SysInfoModel(portletId));
        }
        return Optional.empty();
    }

    @Override
    public String doAddConlet(AddConletRequest event,
            ConsoleSession consoleSession) throws Exception {
        String portletId = generateConletId();
        SysInfoModel conletModel = putInSession(
            consoleSession.browserSession(), new SysInfoModel(portletId));
        renderConlet(event, consoleSession, conletModel);
        return portletId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet
     */
    @Override
    protected void doRenderConlet(RenderConletRequest event,
            ConsoleSession consoleSession, String portletId,
            SysInfoModel conletModel) throws Exception {
        renderConlet(event, consoleSession, conletModel);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession portalSession, SysInfoModel portletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        if (event.renderPreview()) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-preview.ftl.html");
            portalSession.respond(new RenderPortletFromTemplate(event,
                SysInfoConlet.class, portletModel.getConletId(),
                tpl, fmModel(event, portalSession, portletModel))
                    .setRenderMode(RenderMode.DeleteablePreview)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            updateView(portalSession, portletModel.getConletId());
        }
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-view.ftl.html");
            portalSession.respond(new RenderPortletFromTemplate(event,
                SysInfoConlet.class, portletModel.getConletId(),
                tpl, fmModel(event, portalSession, portletModel))
                    .setRenderMode(RenderMode.View)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
        }
    }

    private void updateView(ConsoleSession portalSession, String portletId) {
        if (!portalSession.isConnected()) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        portalSession.respond(new NotifyConletView(type(),
            portletId, "updateMemorySizes",
            System.currentTimeMillis(), runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.totalMemory() - runtime.freeMemory()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
     */
    @Override
    protected void doDeleteConlet(DeleteConletRequest event,
            ConsoleSession portalSession, String portletId,
            SysInfoModel retrievedState) throws Exception {
        portalSession.respond(new DeleteConlet(portletId));
    }

    /**
     * Handle the periodic update event by sending {@link NotifyConletView}
     * events.
     *
     * @param event the event
     * @param portalSession the portal session
     */
    @Handler
    public void onUpdate(Update event, ConsoleSession portalSession) {
        for (String portletId : conletIds(portalSession)) {
            updateView(portalSession, portletId);
        }
    }

    @Override
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession portalSession, SysInfoModel portletState)
            throws Exception {
        event.stop();
        System.gc();
        for (String conletId : conletIds(portalSession)) {
            updateView(portalSession, conletId);
        }
    }

    /**
     * The portlet's model.
     */
    @SuppressWarnings("serial")
    public static class SysInfoModel
            extends AbstractConlet.ConletBaseModel {

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public SysInfoModel(String portletId) {
            super(portletId);
        }

        /**
         * Return the system properties.
         *
         * @return the properties
         */
        public Properties systemProperties() {
            return System.getProperties();
        }

        /**
         * Return the {@link Runtime}.
         *
         * @return the runtime
         */
        public Runtime runtime() {
            return Runtime.getRuntime();
        }
    }

}
