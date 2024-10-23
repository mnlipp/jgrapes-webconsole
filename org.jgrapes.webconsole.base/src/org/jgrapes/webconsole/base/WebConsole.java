/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2024 Michael N. Lipp
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

package org.jgrapes.webconsole.base;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.WcJsonRpc.ConletInfo;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.ConletDeleted;
import org.jgrapes.webconsole.base.events.ConsoleConfigured;
import org.jgrapes.webconsole.base.events.ConsoleLayoutChanged;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteConlet;
import org.jgrapes.webconsole.base.events.JsonInput;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;

/**
 * Provides the web console component related part of the console.
 */
@SuppressWarnings("PMD.GuardLogStatement")
public class WebConsole extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(WebConsole.class.getName());

    private ConsoleWeblet view;

    /**
     * @param componentChannel
     */
    /* default */ WebConsole(Channel componentChannel) {
        super(componentChannel);
    }

    /* default */ void setView(ConsoleWeblet view) {
        this.view = view;
        MBeanView.addConsole(this);
    }

    /**
     * Provides access to the weblet's channel.
     *
     * @return the channel
     */
    public Channel webletChannel() {
        return view.channel();
    }

    /**
     * Handle JSON input.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount" })
    public void onJsonInput(JsonInput event, ConsoleConnection channel)
            throws InterruptedException, IOException {
        // Send events to web console components on console's channel
        var request = event.request();
        switch (request.method()) {
        case "consoleReady": {
            fire(new ConsoleReady(view.renderSupport()), channel);
            break;
        }
        case "addConlet": {
            fire(new AddConletRequest(view.renderSupport(),
                request.param(0),
                Arrays.stream((String[]) request.param(1)).map(
                    value -> RenderMode.valueOf(value))
                    .collect(Collectors.toSet()),
                request.params().length < 3 ? Collections.emptyMap()
                    : request.param(2)).setFrontendRequest(),
                channel);
            break;
        }
        case "conletsDeleted": {
            for (var conletInfo : (ConletInfo[]) request.param(0)) {
                fire(
                    new ConletDeleted(view.renderSupport(),
                        conletInfo.conletId(),
                        conletInfo.modes().stream().map(
                            value -> RenderMode.valueOf((String) value))
                            .collect(Collectors.toSet()),
                        Optional.ofNullable(conletInfo.opts())
                            .orElse(Collections.emptyMap())),
                    channel);
            }
            break;
        }
        case "consoleLayout": {
            String[] previewLayout = request.param(0);
            String[] tabsLayout = request.param(1);
            Object xtraInfo = request.param(2);
            fire(new ConsoleLayoutChanged(
                previewLayout, tabsLayout, xtraInfo), channel);
            break;
        }
        case "renderConlet": {
            fire(new RenderConletRequest(view.renderSupport(),
                request.param(0),
                Arrays.stream((String[]) request.param(1)).map(
                    value -> RenderMode.valueOf(value))
                    .collect(Collectors.toSet())),
                channel);
            break;
        }
        case "setLocale": {
            fire(new SetLocale(view.renderSupport(),
                Locale.forLanguageTag(request.param(0)),
                request.param(1)), channel);
            break;
        }
        case "notifyConletModel": {
            fire(new NotifyConletModel(view.renderSupport(),
                request.param(0), request.param(1),
                request.params().length <= 2 ? new Object[0]
                    : request.param(2)),
                channel);
            break;
        }
        default:
            // Ignore unknown
            break;
        }
    }

    /**
     * Handle network configured condition.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleConfigured(
            ConsoleConfigured event, ConsoleConnection channel)
            throws InterruptedException, IOException {
        channel.respond(new SimpleConsoleCommand("consoleConfigured"));
    }

    /**
     * Fallback handler that sends a {@link DeleteConlet} event 
     * if the {@link RenderConletRequest} event has not been handled
     * successfully.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = -1_000_000)
    public void onRenderConlet(
            RenderConletRequest event, ConsoleConnection channel) {
        if (!event.hasBeenRendered()) {
            channel.respond(
                new DeleteConlet(event.conletId(), Collections.emptySet()));
        }
    }

    /**
     * Discard all console connections on stop.
     *
     * @param event the event
     */
    @Handler
    public void onStop(Stop event) {
        for (ConsoleConnection ps : ConsoleConnection.byConsole(this)) {
            ps.close();
        }
    }

    /**
     * The MBeans view of a console.
     */
    @SuppressWarnings({ "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals" })
    public interface ConsoleMXBean {

        @SuppressWarnings("PMD.CommentRequired")
        class ConsoleConnectionInfo {

            private final ConsoleConnection connection;

            public ConsoleConnectionInfo(ConsoleConnection connection) {
                super();
                this.connection = connection;
            }

            public String getChannel() {
                return connection.upstreamChannel().toString();
            }

            public String getExpiresAt() {
                return connection.expiresAt().atZone(ZoneId.systemDefault())
                    .toString();
            }
        }

        String getComponentPath();

        String getPrefix();

        boolean isUseMinifiedResources();

        void setUseMinifiedResources(boolean useMinifiedResources);

        SortedMap<String, ConsoleConnectionInfo> getConsoleConnections();
    }

    @SuppressWarnings("PMD.CommentRequired")
    public static class WebConsoleInfo implements ConsoleMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<WebConsole> consoleRef;

        @SuppressWarnings("PMD.GuardLogStatement")
        public WebConsoleInfo(WebConsole console) {
            try {
                mbeanName = new ObjectName("org.jgrapes.webconsole:type="
                    + WebConsole.class.getSimpleName() + ",name="
                    + ObjectName.quote(Components.simpleObjectName(console)
                        + " (" + console.view.prefix().toString() + ")"));
            } catch (MalformedObjectNameException e) {
                // Should not happen
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            consoleRef = new WeakReference<>(console);
            try {
                mbs.unregisterMBean(mbeanName);
            } catch (Exception e) { // NOPMD
                // Just in case, should not work
            }
            try {
                mbs.registerMBean(this, mbeanName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                    | NotCompliantMBeanException e) {
                // Should not happen
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        public Optional<WebConsole> console() {
            WebConsole console = consoleRef.get();
            if (console == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (Exception e) { // NOPMD
                    // Should work.
                }
            }
            return Optional.ofNullable(console);
        }

        @Override
        public String getComponentPath() {
            return console().map(Component::componentPath)
                .orElse("<removed>");
        }

        @Override
        public String getPrefix() {
            return console().map(
                console -> console.view.prefix().toString())
                .orElse("<unknown>");
        }

        @Override
        public boolean isUseMinifiedResources() {
            return console().map(
                console -> console.view.useMinifiedResources())
                .orElse(false);
        }

        @Override
        public void setUseMinifiedResources(boolean useMinifiedResources) {
            console().ifPresent(console -> console.view.setUseMinifiedResources(
                useMinifiedResources));
        }

        @Override
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public SortedMap<String, ConsoleConnectionInfo>
                getConsoleConnections() {
            SortedMap<String, ConsoleConnectionInfo> result = new TreeMap<>();
            console().ifPresent(console -> {
                for (ConsoleConnection ps : ConsoleConnection
                    .byConsole(console)) {
                    result.put(Components.simpleObjectName(ps),
                        new ConsoleConnectionInfo(ps));
                }
            });
            return result;
        }
    }

    /**
     * An MBean interface for getting information about all consoles.
     * 
     * There is currently no summary information. However, the (periodic)
     * invocation of {@link WebConsoleSummaryMXBean#getConsoles()} ensures
     * that entries for removed {@link WebConsole}s are unregistered.
     */
    @SuppressWarnings("PMD.CommentRequired")
    public interface WebConsoleSummaryMXBean {

        Set<ConsoleMXBean> getConsoles();

    }

    /**
     * Provides an MBean view of the console.
     */
    @SuppressWarnings("PMD.CommentRequired")
    private static final class MBeanView implements WebConsoleSummaryMXBean {

        private static Set<WebConsoleInfo> consoleInfos = new HashSet<>();

        @SuppressWarnings("PMD.AvoidSynchronizedStatement")
        public static void addConsole(WebConsole console) {
            synchronized (consoleInfos) {
                consoleInfos.add(new WebConsoleInfo(console));
            }
        }

        @Override
        @SuppressWarnings("PMD.AvoidSynchronizedStatement")
        public Set<ConsoleMXBean> getConsoles() {
            Set<WebConsoleInfo> expired = new HashSet<>();
            synchronized (consoleInfos) {
                for (WebConsoleInfo consoleInfo : consoleInfos) {
                    if (!consoleInfo.console().isPresent()) {
                        expired.add(consoleInfo);
                    }
                }
                consoleInfos.removeAll(expired);
            }
            @SuppressWarnings("unchecked")
            Set<ConsoleMXBean> result
                = (Set<ConsoleMXBean>) (Object) consoleInfos;
            return result;
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName
                = new ObjectName("org.jgrapes.webconsole:type="
                    + WebConsole.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Should not happen
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
