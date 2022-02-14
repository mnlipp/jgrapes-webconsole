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
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * 
 */
public class SysInfoConlet
        extends FreeMarkerConlet<SysInfoConlet.SysInfoModel> {

    private static final Set<RenderMode> MODES = RenderMode.asSet(
        RenderMode.Preview, RenderMode.View);

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
     * @param consoleSession the console session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name
     *             exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event,
            ConsoleSession consoleSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add SysInfoConlet resources to page
        consoleSession.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(consoleSession.supportedLocales(), "conletName"))
            .addScript(new ScriptResource()
                .setRequires("chart.js")
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "SysInfo-functions.ftl.js")))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "SysInfo-style.css")));
    }

    @Override
    protected Optional<SysInfoModel> createStateRepresentation(
            RenderConletRequestBase<?> event, ConsoleSession session,
            String conletId) throws Exception {
        return Optional.of(new SysInfoModel());
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession consoleSession, String conletId,
            SysInfoModel conletState) throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.Preview)) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-preview.ftl.html");
            consoleSession.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, consoleSession, conletId, conletState))
                    .setRenderAs(
                        RenderMode.Preview.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("SysInfo-view.ftl.html");
            consoleSession.respond(new RenderConletFromTemplate(event,
                type(), conletId, tpl,
                fmModel(event, consoleSession, conletId, conletState))
                    .setRenderAs(RenderMode.View.addModifiers(event.renderAs()))
                    .setSupportedModes(MODES));
            renderedAs.add(RenderMode.Preview);
        }
        if (!renderedAs.isEmpty()) {
            updateView(consoleSession, conletId);
        }
        return renderedAs;
    }

    private void updateView(ConsoleSession consoleSession, String conletId) {
        if (!consoleSession.isConnected()) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        consoleSession.respond(new NotifyConletView(type(),
            conletId, "updateMemorySizes",
            System.currentTimeMillis(), runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.totalMemory() - runtime.freeMemory()));
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
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleSession consoleSession, SysInfoModel conletState)
            throws Exception {
        event.stop();
        System.gc();
        for (String conletId : conletIds(consoleSession)) {
            updateView(consoleSession, conletId);
        }
    }

    /**
     * The conlet's model.
     */
    @SuppressWarnings("serial")
    public static class SysInfoModel implements Serializable {

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
