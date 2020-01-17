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

package org.jgrapes.webcon.base;

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
import org.jgrapes.webcon.base.Portlet.RenderMode;
import org.jgrapes.webcon.base.events.AddPortletRequest;
import org.jgrapes.webcon.base.events.DeletePortlet;
import org.jgrapes.webcon.base.events.DeletePortletRequest;
import org.jgrapes.webcon.base.events.JsonInput;
import org.jgrapes.webcon.base.events.NotifyPortletModel;
import org.jgrapes.webcon.base.events.PortalConfigured;
import org.jgrapes.webcon.base.events.PortalLayoutChanged;
import org.jgrapes.webcon.base.events.PortalReady;
import org.jgrapes.webcon.base.events.RenderPortletRequest;
import org.jgrapes.webcon.base.events.SetLocale;
import org.jgrapes.webcon.base.events.SimplePortalCommand;

/**
 * Provides the portlet related part of the portal.
 */
public class Portal extends Component {

    private static final Logger logger
        = Logger.getLogger(Portal.class.getName());

    private PortalWeblet view;

    /**
     * @param componentChannel
     */
    /* default */ Portal(Channel componentChannel) {
        super(componentChannel);
    }

    /* default */ void setView(PortalWeblet view) {
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
    public void onJsonInput(JsonInput event, PortalSession channel)
            throws InterruptedException, IOException {
        // Send events to portlets on portal's channel
        JsonArray params = event.request().params();
        switch (event.request().method()) {
        case "portalReady": {
            fire(new PortalReady(view.renderSupport()), channel);
            break;
        }
        case "addPortlet": {
            fire(new AddPortletRequest(view.renderSupport(),
                params.asString(0), params.asArray(1).stream().map(
                    value -> RenderMode.valueOf((String) value))
                    .collect(Collectors.toSet())),
                channel);
            break;
        }
        case "deletePortlet": {
            fire(new DeletePortletRequest(
                view.renderSupport(), params.asString(0)), channel);
            break;
        }
        case "portalLayout": {
            List<String> previewLayout = params.asArray(0).stream().map(
                value -> (String) value).collect(Collectors.toList());
            List<String> tabsLayout = params.asArray(1).stream().map(
                value -> (String) value).collect(Collectors.toList());
            JsonObject xtraInfo = (JsonObject) params.get(2);
            fire(new PortalLayoutChanged(
                previewLayout, tabsLayout, xtraInfo), channel);
            break;
        }
        case "renderPortlet": {
            fire(new RenderPortletRequest(view.renderSupport(),
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
            fire(new NotifyPortletModel(view.renderSupport(),
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
            PortalConfigured event, PortalSession channel)
            throws InterruptedException, IOException {
        channel.respond(new SimplePortalCommand("portalConfigured"));
    }

    /**
     * Fallback handler that sends a {@link DeletePortlet} event 
     * if the {@link RenderPortletRequest} event has not been handled
     * successfully.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler(priority = -1000000)
    public void onRenderPortlet(
            RenderPortletRequest event, PortalSession channel) {
        if (!event.hasBeenRendered()) {
            channel.respond(new DeletePortlet(event.portletId()));
        }
    }

    /**
     * Discard all portal sessions on stop.
     *
     * @param event the event
     */
    @Handler
    public void onStop(Stop event) {
        for (PortalSession ps : PortalSession.byPortal(this)) {
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

            private final PortalSession session;

            public PortalSessionInfo(PortalSession session) {
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
        private final WeakReference<Portal> portalRef;

        public PortalInfo(Portal portal) {
            try {
                mbeanName = new ObjectName("org.jgrapes.portal:type="
                    + Portal.class.getSimpleName() + ",name="
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

        public Optional<Portal> portal() {
            Portal portal = portalRef.get();
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
                for (PortalSession ps : PortalSession.byPortal(portal)) {
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
     * that entries for removed {@link Portal}s are unregistered.
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

        public static void addPortal(Portal portal) {
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
                    + Portal.class.getSimpleName() + "s");
            mbs.registerMBean(new MBeanView(), mxbeanName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException
                | MBeanRegistrationException | NotCompliantMBeanException e) {
            // Should not happen
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
