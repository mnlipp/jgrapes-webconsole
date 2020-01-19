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
                displayNames(portalSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setRequires("chart.js")
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "SysInfo-functions.ftl.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "SysInfo-style.css")));
    }

    @Override
    protected String generateConletId() {
        return type() + "-" + super.generateConletId();
    }

    @Override
    protected Optional<SysInfoModel> stateFromSession(
            Session session, String conletId) {
        if (conletId.startsWith(type() + "-")) {
            return Optional.of(new SysInfoModel(conletId));
        }
        return Optional.empty();
    }

    @Override
    public String doAddConlet(AddConletRequest event,
            ConsoleSession consoleSession) throws Exception {
        String conletId = generateConletId();
        SysInfoModel conletModel = putInSession(
            consoleSession.browserSession(), new SysInfoModel(conletId));
        renderConlet(event, consoleSession, conletModel);
        return conletId;
    }

    @Override
    protected void doRenderConlet(RenderConletRequest event,
            ConsoleSession consoleSession, String conletId,
            SysInfoModel conletModel) throws Exception {
        renderConlet(event, consoleSession, conletModel);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void renderConlet(RenderConletRequestBase<?> event,
            ConsoleSession portalSession, SysInfoModel conletModel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        if (event.renderPreview()) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-preview.ftl.html");
            portalSession.respond(new RenderConletFromTemplate(event,
                SysInfoConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, portalSession, conletModel))
                    .setRenderMode(RenderMode.DeleteablePreview)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
            updateView(portalSession, conletModel.getConletId());
        }
        if (event.renderModes().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-view.ftl.html");
            portalSession.respond(new RenderConletFromTemplate(event,
                SysInfoConlet.class, conletModel.getConletId(),
                tpl, fmModel(event, portalSession, conletModel))
                    .setRenderMode(RenderMode.View)
                    .setSupportedModes(MODES)
                    .setForeground(event.isForeground()));
        }
    }

    private void updateView(ConsoleSession portalSession, String conletId) {
        if (!portalSession.isConnected()) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        portalSession.respond(new NotifyConletView(type(),
            conletId, "updateMemorySizes",
            System.currentTimeMillis(), runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.totalMemory() - runtime.freeMemory()));
    }

    @Override
    protected void doDeleteConlet(DeleteConletRequest event,
            ConsoleSession portalSession, String conletId,
            SysInfoModel retrievedState) throws Exception {
        portalSession.respond(new DeleteConlet(conletId));
    }

    /**
     * Handle the periodic update event by sending {@link NotifyConletView}
     * events.
     *
     * @param event the event
     * @param consoleSession the console session
     */
    @Handler
    public void onUpdate(Update event, ConsoleSession consoleSession) {
        for (String conletId : conletIds(consoleSession)) {
            updateView(consoleSession, conletId);
        }
    }

    @Override
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession portalSession, SysInfoModel conletState)
            throws Exception {
        event.stop();
        System.gc();
        for (String conletId : conletIds(portalSession)) {
            updateView(portalSession, conletId);
        }
    }

    /**
     * The conlet's model.
     */
    @SuppressWarnings("serial")
    public static class SysInfoModel
            extends AbstractConlet.ConletBaseModel {

        /**
         * Creates a new model with the given type and id.
         * 
         * @param conletId the web console component id
         */
        @ConstructorProperties({ "conletId" })
        public SysInfoModel(String conletId) {
            super(conletId);
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
