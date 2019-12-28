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

package org.jgrapes.portal.base;

import java.beans.ConstructorProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.nio.CharBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.portal.base.events.AddPortletRequest;
import org.jgrapes.portal.base.events.AddPortletType;
import org.jgrapes.portal.base.events.DeletePortlet;
import org.jgrapes.portal.base.events.DeletePortletRequest;
import org.jgrapes.portal.base.events.NotifyPortletModel;
import org.jgrapes.portal.base.events.NotifyPortletView;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.PortletResourceRequest;
import org.jgrapes.portal.base.events.RenderPortlet;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.portal.base.events.RenderPortletRequestBase;

/**
 * Provides a base class for implementing portlet components.
 * In addition to translating events to invocations of abstract
 * methods, this class manages the state information of a
 * portlet instance.
 * 
 * # Event handling
 * 
 * The following diagrams show the events exchanged between
 * the {@link Portal} and a portlet from the portlet's 
 * perspective. If applicable, they also show how the events 
 * are translated by the {@link AbstractPortlet} to invocations 
 * of the abstract methods that have to be implemented by the
 * derived class (the portlet component that provides
 * a specific portlet type).
 * 
 * ## PortalReady
 * 
 * ![Add portlet type handling](AddPortletTypeHandling.svg)
 * 
 * From the portal page point of view, a portlet consists of
 * CSS and JavaScript that is added to the portal page by
 * {@link AddPortletType} events and HTML that is provided by 
 * {@link RenderPortlet} events (see below). These events must 
 * therefore be generated by a portlet component. With respect to
 * the firing of the initial {@link AddPortletType}, 
 * the {@link AbstractPortlet} does not provide any support. The
 * handler for the {@link PortalReady} must be implemented by
 * the derived class itself.
 *
 * ## AddPortletRequest
 * 
 * ![Add portlet handling](AddPortletHandling.svg)
 * 
 * The {@link AddPortletRequest} indicates that a new portlet
 * instance of a given type should be added to the page. The
 * {@link AbstractPortlet} checks the type requested, and if
 * it matches, invokes {@link #doAddPortlet doAddPortlet}. The
 * derived class generates a new unique portlet id (optionally 
 * using {@link #generatePortletId generatePortletId}) 
 * and a state (model) for the instance. The derived class 
 * calls {@link #putInSession putInSession} to make the
 * state known to the {@link AbstractPortlet}. Eventually,
 * it fires the {@link RenderPortlet} event and returns the
 * new portlet id. The {@link RenderPortlet} event delivers
 * the HTML that represents the portlet on the page to the portal
 * session. The portlet state may be used to generate HTML that represents
 * the state. Alternatively, state independent HTML may be delivered
 * followed by a {@link NotifyPortletView} event that updates
 * the HTML using JavaScript in the portal page.
 * 
 * ## RenderPortlet
 * 
 * ![Render portlet handling](RenderPortletHandling.svg)
 * 
 * A {@link RenderPortlet} event indicates that the portal page
 * needs the HTML for displaying a portlet. This may be cause
 * by e.g. a refresh or by requesting a full page view from
 * the preview.
 * 
 * Upon receiving such an event, the {@link AbstractPortlet}
 * checks if it has state information for the portlet id
 * requested. If so, it invokes 
 * {@link #doRenderPortlet doRenderPortlet}
 * with the state information. This method has to fire
 * the {@link RenderPortlet} event that delivers the HTML.
 * 
 * ## DeletePortletRequest
 * 
 * ![Delete portlet handling](DeletePortletHandling.svg)
 * 
 * When the {@link AbstractPortlet} receives a {@link DeletePortletRequest},
 * it checks if state information for the portlet id exists. If so,
 * it deletes the state information from the session and
 * invokes 
 * {@link #doDeletePortlet(DeletePortletRequest, PortalSession, String, Serializable)}
 * with the state information. This method fires the {@link DeletePortlet}
 * event that confirms the deletion of the portlet.
 *
 * ## NotifyPortletModel
 * 
 * ![Notify portlet model handling](NotifyPortletModelHandling.svg)
 * 
 * If the portlet display includes input elements, actions on these
 * elements may result in {@link NotifyPortletModel} events from
 * the portal page to the portal. When the {@link AbstractPortlet}
 * receives such events, it checks if state information for the 
 * portlet id exists. If so, it invokes 
 * {@link #doNotifyPortletModel doNotifyPortletModel} with the
 * retrieved information. The portal component usually responds with
 * a {@link NotifyPortletView} event. However, it can also
 * re-render the complete portelt display.
 *
 * Support for unsolicited updates
 * -------------------------------
 * 
 * In addition, the class provides support for tracking the 
 * relationship between {@link PortalSession}s and the ids 
 * of portlets displayed in the portal session and support for
 * unsolicited updates.
 * 
 * @startuml AddPortletTypeHandling.svg
 * hide footbox
 * 
 * activate Portal
 * Portal -> PortletComponent: PortalReady
 * deactivate Portal
 * activate PortletComponent
 * PortletComponent -> Portal: AddPortletType 
 * deactivate PortletComponent
 * activate Portal
 * deactivate Portal
 * @enduml
 * 
 * @startuml AddPortletHandling.svg
 * hide footbox
 * 
 * activate Portal
 * Portal -> PortletComponent: AddPortletRequest
 * deactivate Portal
 * activate PortletComponent
 * PortletComponent -> PortletComponent: doAddPortlet
 * activate PortletComponent
 * opt
 * 	   PortletComponent -> PortletComponent: generatePortletId
 * end opt
 * PortletComponent -> PortletComponent: putInSession
 * PortletComponent -> Portal: RenderPortlet
 * opt 
 *     PortletComponent -> Portal: NotifyPortletView
 * end opt 
 * deactivate PortletComponent
 * deactivate PortletComponent
 * activate Portal
 * deactivate Portal
 * @enduml
 * 
 * @startuml RenderPortletHandling.svg
 * hide footbox
 * 
 * activate Portal
 * Portal -> PortletComponent: RenderPortlet
 * deactivate Portal
 * activate PortletComponent
 * PortletComponent -> PortletComponent: doRenderPortlet
 * activate PortletComponent
 * PortletComponent -> Portal: RenderPortlet 
 * opt 
 *     PortletComponent -> Portal: NotifyPortletView
 * end opt 
 * deactivate PortletComponent
 * deactivate PortletComponent
 * activate Portal
 * deactivate Portal
 * @enduml
 * 
 * @startuml NotifyPortletModelHandling.svg
 * hide footbox
 * 
 * activate Portal
 * Portal -> PortletComponent: NotifyPortletModel
 * deactivate Portal
 * activate PortletComponent
 * PortletComponent -> PortletComponent: doNotifyPortletModel
 * activate PortletComponent
 * opt
 *     PortletComponent -> Portal: RenderPortlet
 * end opt 
 * opt 
 *     PortletComponent -> Portal: NotifyPortletView
 * end opt 
 * deactivate PortletComponent
 * deactivate PortletComponent
 * activate Portal
 * deactivate Portal
 * @enduml
 * 
 * @startuml DeletePortletHandling.svg
 * hide footbox
 * 
 * activate Portal
 * Portal -> PortletComponent: DeletePortletRequest
 * deactivate Portal
 * activate PortletComponent
 * PortletComponent -> PortletComponent: doDeletePortlet
 * activate PortletComponent
 * PortletComponent -> Portal: DeletePortlet 
 * deactivate PortletComponent
 * deactivate PortletComponent
 * activate Portal
 * deactivate Portal
 * @enduml
 */
@SuppressWarnings({ "PMD.TooManyMethods",
    "PMD.EmptyMethodInAbstractClassShouldBeAbstract" })
public abstract class AbstractPortlet extends Component {

    private Map<PortalSession, Set<String>> portletIdsByPortalSession;
    private Duration refreshInterval;
    private Supplier<Event<?>> refreshEventSupplier;
    private Timer refreshTimer;
    private Map<Locale, ResourceBundle> l10nBundles = new HashMap<>();

    /**
     * Creates a new component that listens for new events
     * on the given channel.
     * 
     * @param channel the channel to listen on
     * @param trackPortalSessions if set, track the relationship between
     * portal sessions and portlet ids
     */
    public AbstractPortlet(Channel channel, boolean trackPortalSessions) {
        this(channel, null, trackPortalSessions);
    }

    /**
     * Like {@link #AbstractPortlet(Channel, boolean)}, but supports
     * the specification of channel replacements.
     *
     * @param channel the channel to listen on
     * @param channelReplacements the channel replacements (see
     * {@link Component})
     * @param trackPortalSessions if set, track the relationship between
     * portal sessions and portlet ids
     */
    public AbstractPortlet(Channel channel,
            ChannelReplacements channelReplacements,
            boolean trackPortalSessions) {
        super(channel, channelReplacements);
        if (trackPortalSessions) {
            portletIdsByPortalSession = Collections.synchronizedMap(
                new WeakHashMap<>());
        }
    }

    /**
     * If set to a value different from `null` causes an event
     * from the given supplier to be fired on all tracked portal
     * sessions periodically.
     * 
     * @param interval the refresh interval
     * @return the portlet for easy chaining
     */
    public AbstractPortlet setPeriodicRefresh(
            Duration interval, Supplier<Event<?>> supplier) {
        refreshInterval = interval;
        refreshEventSupplier = supplier;
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        updateRefresh();
        return this;
    }

    private void updateRefresh() {
        if (refreshInterval == null || portletIdsByPortalSession().isEmpty()) {
            // At least one of the prerequisites is missing, terminate
            if (refreshTimer != null) {
                refreshTimer.cancel();
                refreshTimer = null;
            }
            return;
        }
        if (refreshTimer != null) {
            // Already running.
            return;
        }
        refreshTimer = Components.schedule(tmr -> {
            tmr.reschedule(tmr.scheduledFor().plus(refreshInterval));
            fire(refreshEventSupplier.get(), trackedSessions());
        }, Instant.now().plus(refreshInterval));
    }

    /**
     * Returns the portlet type. The default implementation
     * returns the class' name.
     * 
     * @return the type
     */
    protected String type() {
        return getClass().getName();
    }

    /**
     * A default handler for resource requests. Checks that the request
     * is directed at this portlet, and calls {@link #doGetResource}.
     * 
     * @param event the resource request event
     * @param channel the channel that the request was recived on
     */
    @Handler
    public final void onResourceRequest(
            PortletResourceRequest event, IOSubchannel channel) {
        // For me?
        if (!event.portletClass().equals(type())) {
            return;
        }
        doGetResource(event, channel);
    }

    /**
     * The default implementation searches for a file with the 
     * requested resource URI in the portlet's class path and sets 
     * its {@link URL} as result if found.
     * 
     * @param event the event. The result will be set to
     * `true` on success
     * @param channel the channel
     */
    protected void doGetResource(PortletResourceRequest event,
            IOSubchannel channel) {
        URL resourceUrl = this.getClass().getResource(
            event.resourceUri().getPath());
        if (resourceUrl == null) {
            return;
        }
        event.setResult(new ResourceByUrl(event, resourceUrl));
        event.stop();
    }

    /**
     * Returns the bundles for the given locales. The default implementation 
     * looks up the available bundles for the locales using the
     * package name plus "l10n" as base name. Note that the bundle returned
     * for a given locale may be fallback bundle.
     *
     * @param toGet the locales to get bundles for
     * @return the map with locales and bundles
     */
    protected Map<Locale, ResourceBundle> l10nBundles(Set<Locale> toGet) {
        Map<Locale, ResourceBundle> result = new HashMap<>();
        for (Locale locale : toGet) {
            ResourceBundle bundle = l10nBundles.get(locale);
            if (bundle == null) {
                bundle = ResourceBundle.getBundle(
                    getClass().getPackage().getName() + ".l10n", locale,
                    getClass().getClassLoader(),
                    ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_DEFAULT));
                l10nBundles.put(locale, bundle);
            }
            result.put(locale, bundle);
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Provides a resource bundle for localization.
     * The default implementation looks up a bundle using the
     * package name plus "l10n" as base name.
     * 
     * @return the resource bundle
     */
    protected ResourceBundle resourceBundle(Locale locale) {
        return ResourceBundle.getBundle(
            getClass().getPackage().getName() + ".l10n", locale,
            getClass().getClassLoader(),
            ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT));
    }

    /**
     * Generates a new unique portlet id.
     * 
     * @return the portlet id
     */
    protected String generatePortletId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the tracked models and channels as unmodifiable map.
     * If sessions are not tracked, the method returns an empty map.
     * It is therefore always safe to invoke the method and use its
     * result.
     * 
     * If you need a particular session's portlet ids, you should
     * prefer {@link #portletIds(PortalSession)} over calling
     * this method with `get(portalSession)` appended.
     * 
     * @return the result
     */
    protected Map<PortalSession, Set<String>> portletIdsByPortalSession() {
        // Create copy to get a non-weak map.
        if (portletIdsByPortalSession != null) {
            return Collections.unmodifiableMap(
                new HashMap<>(portletIdsByPortalSession));
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the tracked sessions. This is effectively
     * `portletIdsByPortalSession().keySet()` converted to
     * an array. This representation is especially useful 
     * when the portal sessions are used as argument for 
     * {@link #fire(Event, Channel...)}.
     *
     * @return the portal sessions
     */
    protected PortalSession[] trackedSessions() {
        if (portletIdsByPortalSession == null) {
            return new PortalSession[0];
        }
        Set<PortalSession> sessions = new HashSet<>(
            portletIdsByPortalSession.keySet());
        return sessions.toArray(new PortalSession[0]);
    }

    /**
     * Returns the set of portlet ids associated with the portal session
     * as an unmodifiable {@link Set}. If sessions aren't tracked, or no
     * portlets have registered yet, an empty set is returned. The method 
     * can therefore always be called and always returns a usable result.
     * 
     * @param portalSession the portal session
     * @return the set
     */
    protected Set<String> portletIds(PortalSession portalSession) {
        if (portletIdsByPortalSession != null) {
            return Collections.unmodifiableSet(
                portletIdsByPortalSession.getOrDefault(
                    portalSession, Collections.emptySet()));
        }
        return new HashSet<>();
    }

    /**
     * Track the given portlet from the given session if tracking is
     * enabled.
     *
     * @param portalSession the portal session
     * @param portletId the portlet id
     */
    protected void trackPortlet(PortalSession portalSession, String portletId) {
        if (portletIdsByPortalSession != null) {
            portletIdsByPortalSession.computeIfAbsent(portalSession,
                newKey -> ConcurrentHashMap.newKeySet()).add(portletId);
            updateRefresh();
        }
    }

    /**
     * Puts the given portlet state in the session using the 
     * {@link #type()} and the given portlet id as keys.
     * 
     * @param session the session to use
     * @param portletId the portlet id
     * @param portletState the portlet state
     * @return the portlet state
     */
    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    protected <T extends Serializable> T putInSession(
            Session session, String portletId, T portletState) {
        ((Map<Serializable,
                Map<Serializable, Map<String, Serializable>>>) (Map<
                        Serializable, ?>) session)
                            .computeIfAbsent(AbstractPortlet.class,
                                newKey -> new ConcurrentHashMap<>())
                            .computeIfAbsent(type(),
                                newKey -> new ConcurrentHashMap<>())
                            .put(portletId, portletState);
        return portletState;
    }

    /**
     * Puts the given portlet instance state in the browser
     * session associated with the channel, using  
     * {@link #type()} and the portlet id from the model.
     *
     * @param session the session to use
     * @param portletModel the portlet model
     * @return the portlet model
     */
    protected <T extends PortletBaseModel> T putInSession(
            Session session, T portletModel) {
        return putInSession(session, portletModel.getPortletId(), portletModel);
    }

    /**
     * Returns the portlet state of this portlet's type with the given id
     * from the session.
     * 
     * @param session the session to use
     * @param portletId the portlet id
     * @param type the state's type
     * @return the portlet state
     */
    @SuppressWarnings("unchecked")
    protected <T extends Serializable> Optional<T> stateFromSession(
            Session session, String portletId, Class<T> type) {
        return Optional.ofNullable(
            ((Map<Serializable,
                    Map<Serializable, Map<String, T>>>) (Map<Serializable,
                            ?>) session)
                                .computeIfAbsent(AbstractPortlet.class,
                                    newKey -> new HashMap<>())
                                .computeIfAbsent(type(),
                                    newKey -> new HashMap<>())
                                .get(portletId));
    }

    /**
     * Returns all portlet states of this portlet's type from the
     * session.
     * 
     * @param channel the channel, used to access the session
     * @param type the states' type
     * @return the states
     */
    @SuppressWarnings("unchecked")
    protected <T extends Serializable> Collection<T> statesFromSession(
            IOSubchannel channel, Class<T> type) {
        return channel.associated(Session.class)
            .map(session -> ((Map<Serializable,
                    Map<Serializable, Map<String, T>>>) (Map<Serializable,
                            ?>) session)
                                .computeIfAbsent(AbstractPortlet.class,
                                    newKey -> new HashMap<>())
                                .computeIfAbsent(type(),
                                    newKey -> new HashMap<>())
                                .values())
            .orElseThrow(
                () -> new IllegalStateException("Session is missing."));
    }

    /**
     * Removes the portlet state of the portlet with the given id
     * from the session. 
     * 
     * @param session the session to use
     * @param portletId the portlet id
     * @return the removed state if state existed
     */
    @SuppressWarnings("unchecked")
    protected Optional<? extends Serializable> removeState(
            Session session, String portletId) {
        Serializable state = ((Map<Serializable,
                Map<Serializable, Map<String, Serializable>>>) (Map<
                        Serializable, ?>) session)
                            .computeIfAbsent(AbstractPortlet.class,
                                newKey -> new HashMap<>())
                            .computeIfAbsent(type(), newKey -> new HashMap<>())
                            .remove(portletId);
        return Optional.ofNullable(state);
    }

    /**
     * Checks if the request applies to this component. If so, stops the event,
     * and calls {@link #doAddPortlet}. 
     * 
     * @param event the event
     * @param portalSession the channel
     */
    @Handler
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException",
        "PMD.AvoidDuplicateLiterals" })
    public final void onAddPortletRequest(AddPortletRequest event,
            PortalSession portalSession) throws Exception {
        if (!event.portletType().equals(type())) {
            return;
        }
        event.stop();
        String portletId = doAddPortlet(event, portalSession);
        event.setResult(portletId);
        trackPortlet(portalSession, portletId);
    }

    /**
     * Called by {@link #onAddPortletRequest} to complete adding the portlet.
     * If the portlet has associated state, the implementation should
     * call {@link #putInSession(Session, String, Serializable)} to create
     * the state and put it in the session.
     * 
     * @param event the event
     * @param portalSession the channel
     * @return the id of the created portlet
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract String doAddPortlet(AddPortletRequest event,
            PortalSession portalSession) throws Exception;

    /**
     * Checks if the request applies to this component. If so, stops 
     * the event, removes the portlet state from the browser session
     * and calls {@link #doDeletePortlet} with the state.
     * 
     * If the association of {@link PortalSession}s and portlet ids
     * is tracked for this portlet, any existing association is
     * also removed.
     * 
     * @param event the event
     * @param portalSession the portal session
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onDeletePortletRequest(DeletePortletRequest event,
            PortalSession portalSession) throws Exception {
        Optional<? extends Serializable> optPortletState
            = stateFromSession(portalSession.browserSession(),
                event.portletId(), Serializable.class);
        if (!optPortletState.isPresent()) {
            return;
        }
        String portletId = event.portletId();
        removeState(portalSession.browserSession(), portletId);
        if (portletIdsByPortalSession != null) {
            for (Iterator<PortalSession> psi = portletIdsByPortalSession
                .keySet().iterator(); psi.hasNext();) {
                Set<String> idSet = portletIdsByPortalSession.get(psi.next());
                idSet.remove(portletId);
                if (idSet.isEmpty()) {
                    psi.remove();
                }
            }
            updateRefresh();
        }
        event.stop();
        doDeletePortlet(event, portalSession, event.portletId(),
            optPortletState.get());
    }

    /**
     * Called by {@link #onDeletePortletRequest} to complete deleting
     * the portlet. If the portlet component wants to veto the
     * deletion of the portlet, it puts the state information back in
     * the session with 
     * {@link #putInSession(Session, String, Serializable)} and does
     * not fire the {@link DeletePortlet} event.
     * 
     * @param event the event
     * @param channel the channel
     * @param portletId the portlet id
     * @param portletState the portlet state
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract void doDeletePortlet(DeletePortletRequest event,
            PortalSession channel, String portletId,
            Serializable portletState) throws Exception;

    /**
     * Checks if the request applies to this component by calling
     * {@link #stateFromSession(Session, String, Class)}. If a model
     * is found, sets the event's result to `true`, stops the event, and 
     * calls {@link #doRenderPortlet} with the state information. 
     * 
     * Some portlets that do not persist their models between sessions
     * (e.g. because the model only references data maintained elsewhere)
     * should override {@link #stateFromSession(Session, String, Class)}
     * in such a way that it creates the requested model if it doesn't 
     * exist yet.
     * 
     * @param event the event
     * @param portalSession the portal session
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onRenderPortlet(RenderPortletRequest event,
            PortalSession portalSession) throws Exception {
        Optional<? extends Serializable> optPortletState
            = stateFromSession(portalSession.browserSession(),
                event.portletId(), Serializable.class);
        if (!optPortletState.isPresent()) {
            return;
        }
        event.setResult(true);
        event.stop();
        doRenderPortlet(
            event, portalSession, event.portletId(), optPortletState.get());
        trackPortlet(portalSession, event.portletId());
    }

    /**
     * Called by {@link #onRenderPortlet} to complete rendering
     * the portlet.
     * 
     * @param event the event
     * @param channel the channel
     * @param portletId the portlet id
     * @param portletState the portletState
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract void doRenderPortlet(RenderPortletRequest event,
            PortalSession channel, String portletId,
            Serializable portletState) throws Exception;

    /**
     * Checks if the request applies to this component by calling
     * {@link #stateFromSession(Session, String, Class)}. If a model
     * is found, calls {@link #doNotifyPortletModel} with the state 
     * information. 
     * 
     * @param event the event
     * @param channel the channel
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onNotifyPortletModel(NotifyPortletModel event,
            PortalSession channel) throws Exception {
        Optional<? extends Serializable> optPortletState
            = stateFromSession(channel.browserSession(), event.portletId(),
                Serializable.class);
        if (!optPortletState.isPresent()) {
            return;
        }
        doNotifyPortletModel(event, channel, optPortletState.get());
    }

    /**
     * Called by {@link #onNotifyPortletModel} to complete handling
     * the notification. The default implementation does nothing.
     * 
     * @param event the event
     * @param channel the channel
     * @param portletState the portletState
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected void doNotifyPortletModel(NotifyPortletModel event,
            PortalSession channel, Serializable portletState)
            throws Exception {
        // Default is to do nothing.
    }

    /**
     * Removes the {@link PortalSession} from the set of tracked sessions.
     * If derived portlets need to perform extra actions when a
     * portalSession is closed, they have to override 
     * {@link #afterOnClosed(Closed, PortalSession)}.
     * 
     * @param event the closed event
     * @param portalSession the portal session
     */
    @Handler
    public final void onClosed(Closed event, PortalSession portalSession) {
        if (portletIdsByPortalSession != null) {
            portletIdsByPortalSession.remove(portalSession);
            updateRefresh();
        }
        afterOnClosed(event, portalSession);
    }

    /**
     * Invoked by {@link #onClosed(Closed, PortalSession)} after
     * the portal session has been removed from the set of
     * tracked sessions. The default implementation does
     * nothing.
     * 
     * @param event the closed event
     * @param portalSession the portal session
     */
    protected void afterOnClosed(Closed event, PortalSession portalSession) {
        // Default is to do nothing.
    }

    /**
     * Defines the portlet model following the JavaBean conventions.
     * 
     * Portlet models should follow these conventions because
     * many template engines rely on them and to support serialization
     * to portable formats. 
     */
    @SuppressWarnings("serial")
    public static class PortletBaseModel implements Serializable {

        protected String portletId;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param portletId the portlet id
         */
        @ConstructorProperties({ "portletId" })
        public PortletBaseModel(String portletId) {
            this.portletId = portletId;
        }

        /**
         * Returns the portlet id.
         * 
         * @return the portlet id
         */
        public String getPortletId() {
            return portletId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        public int hashCode() {
            @SuppressWarnings("PMD.AvoidFinalLocalVariable")
            final int prime = 31;
            int result = 1;
            result = prime * result
                + ((portletId == null) ? 0 : portletId.hashCode());
            return result;
        }

        /**
         * Two objects are equal if they have equal portlet ids.
         * 
         * @param obj the other object
         * @return the result
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PortletBaseModel other = (PortletBaseModel) obj;
            if (portletId == null) {
                if (other.portletId != null) {
                    return false;
                }
            } else if (!portletId.equals(other.portletId)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Send to the portal page for adding or updating a complete portlet
     * representation.
     */
    public class RenderPortletFromReader extends RenderPortlet {

        private final Future<String> content;

        /**
         * Creates a new event.
         * 
         * @param portletClass the portlet class
         * @param portletId the id of the portlet
         */
        public RenderPortletFromReader(RenderPortletRequestBase<?> request,
                Class<?> portletClass, String portletId, Reader contentReader) {
            super(portletClass, portletId);
            // Start to prepare the content immediately and concurrently.
            content = request.processedBy().map(pby -> pby.executorService())
                .orElse(Components.defaultExecutorService()).submit(() -> {
                    StringWriter content = new StringWriter();
                    CharBuffer buffer = CharBuffer.allocate(8192);
                    try (Reader rdr = new BufferedReader(contentReader)) {
                        while (true) {
                            if (rdr.read(buffer) < 0) {
                                break;
                            }
                            buffer.flip();
                            content.append(buffer);
                            buffer.clear();
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                    return content.toString();
                });

        }

        @Override
        public Future<String> content() {
            return content;
        }
    }
}
