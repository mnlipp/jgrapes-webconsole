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

package org.jgrapes.webconsole.base;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
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

import org.jdrupes.json.JsonArray;
import org.jdrupes.json.JsonObject;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.webconsole.base.ConsoleComponent.RenderMode;
import org.jgrapes.webconsole.base.events.AddComponentRequest;
import org.jgrapes.webconsole.base.events.ConsoleConfigured;
import org.jgrapes.webconsole.base.events.ConsoleLayoutChanged;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteComponent;
import org.jgrapes.webconsole.base.events.DeleteComponentRequest;
import org.jgrapes.webconsole.base.events.JsonInput;
import org.jgrapes.webconsole.base.events.NotifyComponentModel;
import org.jgrapes.webconsole.base.events.RenderComponentRequest;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;

/**
 * Provides the portlet related part of the portal.
 */
public class WebConsole extends Component {

    private static final Logger logger
        = Logger.getLogger(WebConsole.class.getName());

    private ConsoleWeblet view;

    /**
     * @param componentChannel
     */
    WebConsole(Channel componentChannel) {
        super(componentChannel);
    }

    /* default */ void setView(ConsoleWeblet view) {
        this.view = view;
        MBeanView.addPortal(this);
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
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void onJsonInput(JsonInput event, ConsoleSession channel)
            throws InterruptedException, IOException {
        // Send events to portlets on portal's channel
        JsonArray params = event.request().params();
        switch (event.request().method()) {
        case "portalReady": {
            fire(new ConsoleReady(view.renderSupport()), channel);
            break;
        }
        case "addPortlet": {
            fire(new AddComponentRequest(view.renderSupport(),
                params.asString(0), params.asArray(1).stream().map(
                    value -> RenderMode.valueOf((String) value))
                    .collect(Collectors.toSet())),
                channel);
            break;
        }
        case "deletePortlet": {
            fire(new DeleteComponentRequest(
                view.renderSupport(), params.asString(0)), channel);
            break;
        }
        case "portalLayout": {
            List<String> previewLayout = params.asArray(0).stream().map(
                value -> (String) value).collect(Collectors.toList());
            List<String> tabsLayout = params.asArray(1).stream().map(
                value -> (String) value).collect(Collectors.toList());
            JsonObject xtraInfo = (JsonObject) params.get(2);
            fire(new ConsoleLayoutChanged(
                previewLayout, tabsLayout, xtraInfo), channel);
            break;
        }
        case "renderPortlet": {
            fire(new RenderComponentRequest(view.renderSupport(),
                params.asString(0),
                params.asArray(1).stream().map(
                    value -> RenderMode.valueOf((String) value))
                    .collect(Collectors.toSet())),
                channel);
            break;
        }
        case "setLocale": {
            fire(new SetLocale(view.renderSupport(),
                Locale.forLanguageTag(params.asString(0)),
                params.asBoolean(1)), channel);
            break;
        }
        case "notifyPortletModel": {
            fire(new NotifyComponentModel(view.renderSupport(),
                params.asString(0), params.asString(1),
                params.size() <= 2
                    ? JsonArray.EMPTY_ARRAY
                    : params.asArray(2)),
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
    public void onPortalConfigured(
            ConsoleConfigured event, ConsoleSession channel)
            throws InterruptedException, IOException {
        channel.respond(new SimpleConsoleCommand("portalConfigured"));
    }

    /**
     * Fallback handler that sends a {@link DeleteComponent} event 
     * if the {@link RenderComponentRequest} event has not been handled
     * successfully.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = -1000000)
    public void onRenderPortlet(
            RenderComponentRequest event, ConsoleSession channel) {
        if (!event.hasBeenRendered()) {
            channel.respond(new DeleteComponent(event.portletId()));
        }
    }

    /**
     * Discard all portal sessions on stop.
     *
     * @param event the event
     */
    @Handler
    public void onStop(Stop event) {
        for (ConsoleSession ps : ConsoleSession.byPortal(this)) {
            ps.discard();
        }
    }

    /**
     * The MBeans view of a portal.
     */
    @SuppressWarnings({ "PMD.CommentRequired", "PMD.AvoidDuplicateLiterals" })
    public interface PortalMXBean {

        @SuppressWarnings("PMD.CommentRequired")
        class PortalSessionInfo {

            private final ConsoleSession session;

            public PortalSessionInfo(ConsoleSession session) {
                super();
                this.session = session;
            }

            public String getChannel() {
                return session.upstreamChannel().toString();
            }

            public String getExpiresAt() {
                return session.expiresAt().atZone(ZoneId.systemDefault())
                    .toString();
            }
        }

        String getComponentPath();

        String getPrefix();

        boolean isUseMinifiedResources();

        void setUseMinifiedResources(boolean useMinifiedResources);

        SortedMap<String, PortalSessionInfo> getPortalSessions();
    }

    @SuppressWarnings("PMD.CommentRequired")
    public static class PortalInfo implements PortalMXBean {

        private static MBeanServer mbs
            = ManagementFactory.getPlatformMBeanServer();

        private ObjectName mbeanName;
        private final WeakReference<WebConsole> portalRef;

        public PortalInfo(WebConsole portal) {
            try {
                mbeanName = new ObjectName("org.jgrapes.portal:type="
                    + WebConsole.class.getSimpleName() + ",name="
                    + ObjectName.quote(Components.simpleObjectName(portal)
                        + " (" + portal.view.prefix().toString() + ")"));
            } catch (MalformedObjectNameException e) {
                // Should not happen
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            portalRef = new WeakReference<>(portal);
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

        public Optional<WebConsole> portal() {
            WebConsole portal = portalRef.get();
            if (portal == null) {
                try {
                    mbs.unregisterMBean(mbeanName);
                } catch (Exception e) { // NOPMD
                    // Should work.
                }
            }
            return Optional.ofNullable(portal);
        }

        @Override
        public String getComponentPath() {
            return portal().map(mgr -> mgr.componentPath()).orElse("<removed>");
        }

        @Override
        public String getPrefix() {
            return portal().map(
                portal -> portal.view.prefix().toString()).orElse("<unknown>");
        }

        @Override
        public boolean isUseMinifiedResources() {
            return portal().map(
                portal -> portal.view.useMinifiedResources())
                .orElse(false);
        }

        @Override
        public void setUseMinifiedResources(boolean useMinifiedResources) {
            portal().ifPresent(portal -> portal.view.setUseMinifiedResources(
                useMinifiedResources));
        }

        @Override
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public SortedMap<String, PortalSessionInfo> getPortalSessions() {
            SortedMap<String, PortalSessionInfo> result = new TreeMap<>();
            portal().ifPresent(portal -> {
                for (ConsoleSession ps : ConsoleSession.byPortal(portal)) {
                    result.put(Components.simpleObjectName(ps),
                        new PortalSessionInfo(ps));
                }
            });
            return result;
        }
    }

    /**
     * An MBean interface for getting information about all portals.
     * 
     * There is currently no summary information. However, the (periodic)
     * invocation of {@link PortalSummaryMXBean#getPortals()} ensures
     * that entries for removed {@link WebConsole}s are unregistered.
     */
    @SuppressWarnings("PMD.CommentRequired")
    public interface PortalSummaryMXBean {

        Set<PortalMXBean> getPortals();

    }

    /**
     * Provides an MBean view of the portal.
     */
    @SuppressWarnings("PMD.CommentRequired")
    private static class MBeanView implements PortalSummaryMXBean {

        private static Set<PortalInfo> portalInfos = new HashSet<>();

        public static void addPortal(WebConsole portal) {
            synchronized (portalInfos) {
                portalInfos.add(new PortalInfo(portal));
            }
        }

        @Override
        public Set<PortalMXBean> getPortals() {
            Set<PortalInfo> expired = new HashSet<>();
            synchronized (portalInfos) {
                for (PortalInfo portalInfo : portalInfos) {
                    if (!portalInfo.portal().isPresent()) {
                        expired.add(portalInfo);
                    }
                }
                portalInfos.removeAll(expired);
            }
            @SuppressWarnings("unchecked")
            Set<PortalMXBean> result = (Set<PortalMXBean>) (Object) portalInfos;
            return result;
        }
    }

    static {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName
                = new ObjectName("org.jgrapes.portal:type="
                    + WebConsole.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Should not happen
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
