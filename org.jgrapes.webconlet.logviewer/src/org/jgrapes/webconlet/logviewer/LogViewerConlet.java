/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2020  Michael N. Lipp
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

package org.jgrapes.webconlet.logviewer;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogRecord;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * A conlet for displaying records from java.util.logging. The class
 * {@link LogViewerHandler} has to be registered as a handler in the
 * logging configuration (e.g. with 
 * `-Djava.util.logging.config.file=logging.properties` and
 * `logging.properties`:
 * ```
 * org.jgrapes.webconlet.logviewer.LogViewerHandler.level=CONFIG
 * ```
 * 
 * The handler implements a ring buffer for the last 100 
 * {@link LogRecord}s. When the conlet is displayed, it obtains
 * the initially shown messages from the ring buffer. All subsequently
 * published {@link LogRecord}s are forwarded to the conlet.
 * In order to limit the memory required in the browser, the conlet
 * also retains only the 100 most recent messages. 
 */
public class LogViewerConlet extends FreeMarkerConlet<Serializable> {

    private static final Set<RenderMode> MODES
        = RenderMode.asSet(RenderMode.View);

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     * on by default and that {@link Manager#fire(Event, Channel...)}
     * sends the event to
     */
    public LogViewerConlet(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On {@link ConsoleReady}, fire the {@link AddConletType}.
     *
     * @param event the event
     * @param channel the channel
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name
     *             exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleConnection channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .setDisplayNames(
                localizations(channel.supportedLocales(), "conletName"))
            .addRenderMode(RenderMode.View)
            .addScript(new ScriptResource().setScriptType("module")
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "LogViewer-functions.ftl.js")))
            .addCss(event.renderSupport(),
                WebConsoleUtils.uriFromPath("LogViewer-style.css")));
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleConnection channel, String conletId,
            Serializable conletState)
            throws Exception {
        Set<RenderMode> renderedAs = new HashSet<>();
        if (event.renderAs().contains(RenderMode.View)) {
            Template tpl
                = freemarkerConfig().getTemplate("LogViewer-view.ftl.html");
            channel.respond(new RenderConlet(type(), conletId,
                processTemplate(event, tpl,
                    fmModel(event, channel, conletId, conletState)))
                        .setRenderAs(
                            RenderMode.View.addModifiers(event.renderAs()))
                        .setSupportedModes(MODES));
            sendAllEntries(channel, conletId);
            renderedAs.add(RenderMode.View);
        }
        return renderedAs;
    }

    private void sendAllEntries(ConsoleConnection channel, String conletId) {
        channel.respond(new NotifyConletView(type(),
            conletId, "entries",
            (Object) LogViewerHandler.setConlet(this).stream()
                .map(this::logEntryAsMap).toArray()));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    /* default */ void addEntry(LogRecord entry) {
        for (ConsoleConnection connection : trackedConnections()) {
            for (String conletId : conletIds(connection)) {
                connection.respond(new NotifyConletView(type(),
                    conletId, "addEntry", logEntryAsMap(entry))
                        .disableTracking());
            }
        }
    }

    private Map<String, Object> logEntryAsMap(LogRecord record) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        result.put("exception", Optional.ofNullable(record.getThrown())
            .map(Throwable::getMessage).orElse(""));
        result.put("stacktrace", Optional.ofNullable(record.getThrown())
            .map(exc -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter printWriter = new PrintWriter(out);
                exc.printStackTrace(printWriter);
                printWriter.close();
                return out.toString();
            }).orElse(""));
        result.put("loggerName", record.getLoggerName());
        result.put("source",
            record.getSourceClassName() + "::" + record.getSourceMethodName());
        result.put("logLevel", record.getLevel().toString());
        result.put("message", record.getMessage());
        result.put("time", record.getInstant().toEpochMilli());
        result.put("sequence", record.getSequenceNumber());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.console.AbstractConlet#doNotifyConletModel
     */
    @Override
    @SuppressWarnings({ "PMD.SwitchStmtsShouldHaveDefault",
        "PMD.TooFewBranchesForASwitchStatement" })
    protected void doUpdateConletState(NotifyConletModel event,
            ConsoleConnection channel, Serializable conletState)
            throws Exception {
        event.stop();
        switch (event.method()) {
        case "resync":
            sendAllEntries(channel, event.conletId());
            break;
        }
    }

    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleConnection channel,
            String conletId) throws Exception {
        return true;
    }
}
