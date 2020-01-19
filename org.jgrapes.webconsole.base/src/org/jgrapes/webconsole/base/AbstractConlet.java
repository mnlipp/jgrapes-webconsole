/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2020 Michael N. Lipp
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
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.ConletResourceRequest;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.DeleteConlet;
import org.jgrapes.webconsole.base.events.DeleteConletRequest;
import org.jgrapes.webconsole.base.events.NotifyConletModel;
import org.jgrapes.webconsole.base.events.NotifyConletView;
import org.jgrapes.webconsole.base.events.RenderConlet;
import org.jgrapes.webconsole.base.events.RenderConletRequest;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;

/**
 * Provides a base class for implementing web console components.
 * In addition to translating events to invocations of abstract
 * methods, this class manages the state information of a
 * web console component instance.
 * 
 * # Event handling
 * 
 * The following diagrams show the events exchanged between
 * the {@link WebConsole} and a web console component from the 
 * web console component's perspective. If applicable, they also show 
 * how the events are translated by the {@link AbstractConlet} to invocations 
 * of the abstract methods that have to be implemented by the
 * derived class (the web console component component that provides
 * a specific web console component type).
 * 
 * ## ConsoleReady
 * 
 * ![Add web console component type handling](AddConletTypeHandling.svg)
 * 
 * From the portal page point of view, a web console component consists of
 * CSS and JavaScript that is added to the console page by
 * {@link AddConletType} events and HTML that is provided by 
 * {@link RenderConlet} events (see below). These events must 
 * therefore be generated by a web console component. With respect to
 * the firing of the initial {@link AddConletType}, 
 * the {@link AbstractConlet} does not provide any support. The
 * handler for the {@link ConsoleReady} must be implemented by
 * the derived class itself.
 * 
 * ## AddConletRequest
 * 
 * ![Add web console component handling](AddConletHandling.svg)
 * 
 * The {@link AddConletRequest} indicates that a new web console component
 * instance of a given type should be added to the page. The
 * {@link AbstractConlet} checks the type requested, and if
 * it matches, invokes {@link #doAddConlet doAddConlet}. The
 * derived class generates a new unique web console component id (optionally 
 * using {@link #generateConletId generateConletId}) 
 * and a state (model) for the instance. The derived class 
 * calls {@link #putInSession putInSession} to make the
 * state known to the {@link AbstractConlet}. Eventually,
 * it fires the {@link RenderConlet} event and returns the
 * new web console component id. The {@link RenderConlet} event delivers
 * the HTML that represents the web console component on the page to the 
 * console session. The web console component state may be used to generate 
 * HTML that represents the state. Alternatively, state independent HTML 
 * may be delivered followed by a {@link NotifyConletView} event that updates
 * the HTML using JavaScript in the console page.
 * 
 * ## RenderConlet
 * 
 * ![Render web console component handling](RenderConletHandling.svg)
 * 
 * A {@link RenderConlet} event indicates that the portal page
 * needs the HTML for displaying a web console component. This may be cause
 * by e.g. a refresh or by requesting a full page view from
 * the preview.
 * 
 * Upon receiving such an event, the {@link AbstractConlet}
 * checks if it has state information for the web console component id
 * requested. If so, it invokes 
 * {@link #doRenderConlet doRenderConlet}
 * with the state information. This method has to fire
 * the {@link RenderConlet} event that delivers the HTML.
 * 
 * ## DeleteConletRequest
 * 
 * ![Delete web console component handling](DeleteConletHandling.svg)
 * 
 * When the {@link AbstractConlet} receives a {@link DeleteConletRequest},
 * it checks if state information for the web console component id exists. If so,
 * it deletes the state information from the session and
 * invokes 
 * {@link #doDeleteConlet(DeleteConletRequest, ConsoleSession, String, Serializable)}
 * with the state information. This method fires the {@link DeleteConlet}
 * event that confirms the deletion of the web console component.
 * 
 * ## NotifyConletModel
 * 
 * ![Notify web console component model handling](NotifyConletModelHandling.svg)
 * 
 * If the web console component display includes input elements, actions 
 * on these elements may result in {@link NotifyConletModel} events from
 * the portal page to the portal. When the {@link AbstractConlet}
 * receives such events, it checks if state information for the 
 * web console component id exists. If so, it invokes 
 * {@link #doNotifyConletModel doNotifyConletModel} with the
 * retrieved information. The portal component usually responds with
 * a {@link NotifyConletView} event. However, it can also
 * re-render the complete portelt display.
 * 
 * Support for unsolicited updates
 * -------------------------------
 * 
 * In addition, the class provides support for tracking the 
 * relationship between {@link ConsoleSession}s and the ids 
 * of web console components displayed in the console session and support for
 * unsolicited updates.
 *
 * @param <S> the type of the web console component state information
 * 
 * @startuml AddConletTypeHandling.svg
 * hide footbox
 * 
 * activate WebConsole
 * WebConsole -> Conlet: ConsoleReady
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> WebConsole: AddConletType 
 * deactivate Conlet
 * activate WebConsole
 * deactivate WebConsole
 * @enduml 
 * @startuml AddConletHandling.svg
 * hide footbox
 * 
 * activate WebConsole
 * WebConsole -> Conlet: AddConletRequest
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> Conlet: doAddConlet
 * activate Conlet
 * opt
 * 	   Conlet -> Conlet: generateConletId
 * end opt
 * Conlet -> Conlet: putInSession
 * Conlet -> WebConsole: RenderConlet
 * opt 
 *     Conlet -> WebConsole: NotifyConletView
 * end opt 
 * deactivate Conlet
 * deactivate Conlet
 * activate WebConsole
 * deactivate WebConsole
 * @enduml 
 * @startuml RenderConletHandling.svg
 * hide footbox
 * 
 * activate WebConsole
 * WebConsole -> Conlet: RenderConlet
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> Conlet: doRenderConlet
 * activate Conlet
 * Conlet -> WebConsole: RenderConlet 
 * opt 
 *     Conlet -> WebConsole: NotifyConletView
 * end opt 
 * deactivate Conlet
 * deactivate Conlet
 * activate WebConsole
 * deactivate WebConsole
 * @enduml 
 * @startuml NotifyConletModelHandling.svg
 * hide footbox
 * 
 * activate WebConsole
 * WebConsole -> Conlet: NotifyConletModel
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> Conlet: doNotifyConletModel
 * activate Conlet
 * opt
 *     Conlet -> WebConsole: RenderConlet
 * end opt 
 * opt 
 *     Conlet -> WebConsole: NotifyConletView
 * end opt 
 * deactivate Conlet
 * deactivate Conlet
 * activate WebConsole
 * deactivate WebConsole
 * @enduml 
 * @startuml DeleteConletHandling.svg
 * hide footbox
 * 
 * activate WebConsole
 * WebConsole -> Conlet: DeleteConletRequest
 * deactivate WebConsole
 * activate Conlet
 * Conlet -> Conlet: doDeleteConlet
 * activate Conlet
 * Conlet -> WebConsole: DeleteConlet 
 * deactivate Conlet
 * deactivate Conlet
 * activate WebConsole
 * deactivate WebConsole
 * @enduml 
 */
@SuppressWarnings({ "PMD.TooManyMethods",
    "PMD.EmptyMethodInAbstractClassShouldBeAbstract" })
public abstract class AbstractConlet<S extends Serializable>
        extends Component {

    private Map<ConsoleSession, Set<String>> conletIdsByConsoleSession;
    private Duration refreshInterval;
    private Supplier<Event<?>> refreshEventSupplier;
    private Timer refreshTimer;
    private Map<Locale, ResourceBundle> l10nBundles = new HashMap<>();

    /**
     * Creates a new component that listens for new events
     * on the given channel.
     * 
     * @param channel the channel to listen on
     */
    public AbstractConlet(Channel channel) {
        this(channel, null);
    }

    /**
     * Like {@link #AbstractConlet(Channel)}, but supports
     * the specification of channel replacements.
     *
     * @param channel the channel to listen on
     * @param channelReplacements the channel replacements (see
     * {@link Component})
     */
    public AbstractConlet(Channel channel,
            ChannelReplacements channelReplacements) {
        super(channel, channelReplacements);
        conletIdsByConsoleSession
            = Collections.synchronizedMap(new WeakHashMap<>());
    }

    /**
     * If set to a value different from `null` causes an event
     * from the given supplier to be fired on all tracked portal
     * sessions periodically.
     *
     * @param interval the refresh interval
     * @param supplier the supplier
     * @return the web console component for easy chaining
     */
    public AbstractConlet<S> setPeriodicRefresh(
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
        if (refreshInterval == null || conletIdsByConsoleSession().isEmpty()) {
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
     * Returns the web console component type. The default implementation
     * returns the class' name.
     * 
     * @return the type
     */
    protected String type() {
        return getClass().getName();
    }

    /**
     * A default handler for resource requests. Checks that the request
     * is directed at this web console component, and calls 
     * {@link #doGetResource}.
     * 
     * @param event the resource request event
     * @param channel the channel that the request was recived on
     */
    @Handler
    public final void onResourceRequest(
            ConletResourceRequest event, IOSubchannel channel) {
        // For me?
        if (!event.conletClass().equals(type())) {
            return;
        }
        doGetResource(event, channel);
    }

    /**
     * The default implementation searches for a file with the 
     * requested resource URI in the web console component's class 
     * path and sets its {@link URL} as result if found.
     * 
     * @param event the event. The result will be set to
     * `true` on success
     * @param channel the channel
     */
    protected void doGetResource(ConletResourceRequest event,
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
     * for a given locale may be the fallback bundle.
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

    protected Map<Locale, String> displayNames(Set<Locale> locales,
            String key) {
        Map<Locale, String> result = new HashMap<>();
        Map<Locale, ResourceBundle> bundles = l10nBundles(locales);
        for (Map.Entry<Locale, ResourceBundle> entry : bundles.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getString(key));
        }
        return result;
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
     * Generates a new unique web console component id.
     * 
     * @return the web console component id
     */
    protected String generateConletId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the tracked models and channels as unmodifiable map.
     * If sessions are not tracked, the method returns an empty map.
     * It is therefore always safe to invoke the method and use its
     * result.
     * 
     * If you need a particular session's web console component ids, you 
     * should prefer {@link #conletIds(ConsoleSession)} over calling
     * this method with `get(portalSession)` appended.
     * 
     * @return the result
     */
    protected Map<ConsoleSession, Set<String>> conletIdsByConsoleSession() {
        // Create copy to get a non-weak map.
        return Collections
            .unmodifiableMap(new HashMap<>(conletIdsByConsoleSession));
    }

    /**
     * Returns the tracked sessions. This is effectively
     * `conletIdsByConsoleSession().keySet()` converted to
     * an array. This representation is especially useful 
     * when the portal sessions are used as argument for 
     * {@link #fire(Event, Channel...)}.
     *
     * @return the portal sessions
     */
    protected ConsoleSession[] trackedSessions() {
        Set<ConsoleSession> sessions = new HashSet<>(
            conletIdsByConsoleSession.keySet());
        return sessions.toArray(new ConsoleSession[0]);
    }

    /**
     * Returns the set of web console component ids associated with the 
     * console session as an unmodifiable {@link Set}. If sessions aren't 
     * tracked, or no web console components have registered yet, an empty 
     * set is returned. The method can therefore always be called and 
     * always returns a usable result.
     * 
     * @param consoleSession the console session
     * @return the set
     */
    protected Set<String> conletIds(ConsoleSession consoleSession) {
        return Collections.unmodifiableSet(
            conletIdsByConsoleSession.getOrDefault(
                consoleSession, Collections.emptySet()));
    }

    /**
     * Track the given web console component from the given session. 
     * This is invoked by 
     * {@link #onAddConletRequest(AddConletRequest, ConsoleSession)} and
     * needs only be used if {@link #onAddConletRequest} is overridden.
     *
     * @param portalSession the portal session
     * @param conletId the web console component id
     */
    protected void trackConlet(ConsoleSession portalSession,
            String conletId) {
        conletIdsByConsoleSession.computeIfAbsent(portalSession,
            newKey -> ConcurrentHashMap.newKeySet()).add(conletId);
        updateRefresh();
    }

    /**
     * Puts the given web console component state in the session using the 
     * {@link #type()} and the given web console component id as keys.
     * 
     * @param session the session to use
     * @param conletId the web console component id
     * @param conletState the web console component state
     * @return the portlweb console componentet state
     */
    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    protected Serializable putInSession(Session session, String conletId,
            Serializable conletState) {
        ((Map<Serializable,
                Map<Serializable, Map<String, Serializable>>>) (Map<
                        Serializable, ?>) session)
                            .computeIfAbsent(AbstractConlet.class,
                                newKey -> new ConcurrentHashMap<>())
                            .computeIfAbsent(type(),
                                newKey -> new ConcurrentHashMap<>())
                            .put(conletId, conletState);
        return conletState;
    }

    /**
     * Puts the given web console component instance state in the browser
     * session associated with the channel, using  
     * {@link #type()} and the web console component id from the model.
     *
     * @param session the session to use
     * @param conletModel the web console component model
     * @return the web console component model
     */
    protected <T extends ConletBaseModel> T putInSession(
            Session session, T conletModel) {
        putInSession(session, conletModel.getConletId(), conletModel);
        return conletModel;
    }

    /**
     * Returns the web console component state of this portlet's type 
     * with the given id from the session.
     *
     * @param session the session to use
     * @param conletId the web console component id
     * @return the web console component state
     */
    @SuppressWarnings("unchecked")
    protected Optional<S> stateFromSession(Session session, String conletId) {
        return Optional.ofNullable(
            ((Map<Serializable,
                    Map<Serializable, Map<String, S>>>) (Map<Serializable,
                            ?>) session)
                                .computeIfAbsent(AbstractConlet.class,
                                    newKey -> new HashMap<>())
                                .computeIfAbsent(type(),
                                    newKey -> new HashMap<>())
                                .get(conletId));
    }

    /**
     * Returns all web console component states of this web console 
     * component's type from the session.
     *
     * @param channel the channel, used to access the session
     * @return the states
     */
    @SuppressWarnings("unchecked")
    protected Collection<S> statesFromSession(IOSubchannel channel) {
        return channel.associated(Session.class)
            .map(session -> ((Map<Serializable,
                    Map<Serializable, Map<String, S>>>) (Map<Serializable,
                            ?>) session)
                                .computeIfAbsent(AbstractConlet.class,
                                    newKey -> new HashMap<>())
                                .computeIfAbsent(type(),
                                    newKey -> new HashMap<>())
                                .values())
            .orElseThrow(
                () -> new IllegalStateException("Session is missing."));
    }

    /**
     * Removes the web console component state of the 
     * web console component with the given id from the session. 
     * 
     * @param session the session to use
     * @param conletId the web console component id
     * @return the removed state if state existed
     */
    @SuppressWarnings("unchecked")
    protected Optional<S> removeState(Session session, String conletId) {
        S state = ((Map<Serializable,
                Map<Serializable, Map<String, S>>>) (Map<Serializable,
                        ?>) session)
                            .computeIfAbsent(AbstractConlet.class,
                                newKey -> new HashMap<>())
                            .computeIfAbsent(type(), newKey -> new HashMap<>())
                            .remove(conletId);
        return Optional.ofNullable(state);
    }

    /**
     * Checks if the request applies to this component. If so, stops the event,
     * and calls {@link #doAddConlet}. 
     *
     * @param event the event
     * @param consoleSession the channel
     * @throws Exception the exception
     */
    @Handler
    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException",
        "PMD.AvoidDuplicateLiterals" })
    public final void onAddConletRequest(AddConletRequest event,
            ConsoleSession consoleSession) throws Exception {
        if (!event.conletType().equals(type())) {
            return;
        }
        event.stop();
        String conletId = doAddConlet(event, consoleSession);
        event.setResult(conletId);
        trackConlet(consoleSession, conletId);
    }

    /**
     * Called by {@link #onAddConletRequest} to complete adding the 
     * web console component. If the web console component has associated 
     * state, the implementation should
     * call {@link #putInSession(Session, String, Serializable)} to create
     * the state and put it in the session.
     * 
     * @param event the event
     * @param portalSession the channel
     * @return the id of the created web console component
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract String doAddConlet(AddConletRequest event,
            ConsoleSession portalSession) throws Exception;

    /**
     * Checks if the request applies to this component. If so, stops 
     * the event, removes the web console component state from the 
     * browser session and calls {@link #doDeleteConlet} with the state.
     * 
     * If the association of {@link ConsoleSession}s and web console 
     * component ids is tracked for this web console component, any 
     * existing association is also removed.
     *
     * @param event the event
     * @param consoleSession the portal session
     * @throws Exception the exception
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onDeleteConletRequest(DeleteConletRequest event,
            ConsoleSession consoleSession) throws Exception {
        Optional<S> optConletState = stateFromSession(
            consoleSession.browserSession(), event.conletId());
        if (!optConletState.isPresent()) {
            return;
        }
        String conletId = event.conletId();
        removeState(consoleSession.browserSession(), conletId);
        for (Iterator<ConsoleSession> psi = conletIdsByConsoleSession
            .keySet().iterator(); psi.hasNext();) {
            Set<String> idSet = conletIdsByConsoleSession.get(psi.next());
            idSet.remove(conletId);
            if (idSet.isEmpty()) {
                psi.remove();
            }
        }
        updateRefresh();
        event.stop();
        doDeleteConlet(event, consoleSession, event.conletId(),
            optConletState.get());
    }

    /**
     * Called by {@link #onDeleteConletRequest} to complete deleting
     * the web console component. If the web console component component 
     * wants to veto the deletion of the web console component, it puts 
     * the state information back in the session with 
     * {@link #putInSession(Session, String, Serializable)} and does
     * not fire the {@link DeleteConlet} event.
     * 
     * @param event the event
     * @param channel the channel
     * @param conletId the web console component id
     * @param conletState the web console component state
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract void doDeleteConlet(DeleteConletRequest event,
            ConsoleSession channel, String conletId, S conletState)
            throws Exception;

    /**
     * Checks if the request applies to this component by calling
     * {@link #stateFromSession(Session, String)}. If a model
     * is found, sets the event's result to `true`, stops the event, and 
     * calls {@link #doRenderConlet} with the state information. 
     * 
     * Some web console components that do not persist their models 
     * between sessions (e.g. because the model only references data 
     * maintained elsewhere) should override 
     * {@link #stateFromSession(Session, String)}
     * in such a way that it creates the requested model if it doesn't 
     * exist yet.
     *
     * @param event the event
     * @param portalSession the portal session
     * @throws Exception the exception
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onRenderConlet(RenderConletRequest event,
            ConsoleSession portalSession) throws Exception {
        Optional<S> optConletState = stateFromSession(
            portalSession.browserSession(), event.conletId());
        if (!optConletState.isPresent()) {
            return;
        }
        event.setResult(true);
        event.stop();
        doRenderConlet(
            event, portalSession, event.conletId(), optConletState.get());
        trackConlet(portalSession, event.conletId());
    }

    /**
     * Called by {@link #onRenderConlet} to complete rendering
     * the web console component.
     * 
     * @param event the event
     * @param channel the channel
     * @param conletId the web console component id
     * @param conletState the web console component state
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract void doRenderConlet(RenderConletRequest event,
            ConsoleSession channel, String conletId, S conletState)
            throws Exception;

    /**
     * Invokes {@link #doSetLocale(SetLocale, ConsoleSession, String)}
     * for each web console component in the console session.
     * 
     * If the vent has the reload flag set, does nothing.
     * 
     * The default implementation fires a 
     *
     * @param event the event
     * @param consoleSession the portal session
     * @throws Exception the exception
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void onSetLocale(SetLocale event, ConsoleSession consoleSession)
            throws Exception {
        if (event.reload()) {
            return;
        }
        for (String conletId : conletIds(consoleSession)) {
            if (!doSetLocale(event, consoleSession, conletId)) {
                event.forceReload();
                break;
            }
        }
    }

    /**
     * Called by {@link #onSetLocale(SetLocale, ConsoleSession)} for
     * each web console component in the console session. Derived 
     * classes must send events for updating the representation to 
     * match the new locale.
     * 
     * If the method returns `false` this indicates that the representation 
     * cannot be updated without reloading the portal page.
     * 
     * The default implementation fires a {@link RenderConletRequest}
     * with modes {@link RenderMode#Preview} and {@link RenderMode#View},
     * thus updating all possible representations. (Assuming that "Edit"
     * and "Help" modes are represented with modal dialogs and therefore
     * locale changes aren't possible while these are open.) 
     *
     * @param event the event
     * @param channel the channel
     * @param conletId the web console component id
     * @return true, if the locale could be changed
     * @throws Exception the exception
     */
    protected boolean doSetLocale(SetLocale event, ConsoleSession channel,
            String conletId) throws Exception {
        fire(new RenderConletRequest(event.renderSupport(), conletId,
            RenderMode.asSet(RenderMode.Preview, RenderMode.View)),
            channel);
        return true;
    }

    /**
     * Checks if the request applies to this component by calling
     * {@link #stateFromSession(Session, String)}. If a model
     * is found, calls {@link #doNotifyConletModel} with the state 
     * information. 
     *
     * @param event the event
     * @param channel the channel
     * @throws Exception the exception
     */
    @Handler
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public final void onNotifyConletModel(NotifyConletModel event,
            ConsoleSession channel) throws Exception {
        Optional<S> optConletState
            = stateFromSession(channel.browserSession(), event.conletId());
        if (!optConletState.isPresent()) {
            return;
        }
        doNotifyConletModel(event, channel, optConletState.get());
    }

    /**
     * Called by {@link #onNotifyConletModel} to complete handling
     * the notification. The default implementation does nothing.
     * 
     * @param event the event
     * @param channel the channel
     * @param conletState the web console component state
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected void doNotifyConletModel(NotifyConletModel event,
            ConsoleSession channel, S conletState) throws Exception {
        // Default is to do nothing.
    }

    /**
     * Removes the {@link ConsoleSession} from the set of tracked sessions.
     * If derived web console components need to perform extra actions when a
     * console session is closed, they have to override 
     * {@link #afterOnClosed(Closed, ConsoleSession)}.
     * 
     * @param event the closed event
     * @param portalSession the portal session
     */
    @Handler
    public final void onClosed(Closed event, ConsoleSession portalSession) {
        conletIdsByConsoleSession.remove(portalSession);
        updateRefresh();
        afterOnClosed(event, portalSession);
    }

    /**
     * Invoked by {@link #onClosed(Closed, ConsoleSession)} after
     * the portal session has been removed from the set of
     * tracked sessions. The default implementation does
     * nothing.
     * 
     * @param event the closed event
     * @param consoleSession the portal session
     */
    protected void afterOnClosed(Closed event, ConsoleSession consoleSession) {
        // Default is to do nothing.
    }

    /**
     * Defines the web console component model following the 
     * JavaBean conventions.
     * 
     * Conlet models should follow these conventions because
     * many template engines rely on them and to support serialization
     * to portable formats. 
     */
    @SuppressWarnings("serial")
    public static class ConletBaseModel implements Serializable {

        protected String conletId;

        /**
         * Creates a new model with the given type and id.
         * 
         * @param conletId the web console component id
         */
        @ConstructorProperties({ "conletId" })
        public ConletBaseModel(String conletId) {
            this.conletId = conletId;
        }

        /**
         * Returns the web console component id.
         * 
         * @return the web console component id
         */
        public String getConletId() {
            return conletId;
        }

        /**
         * Hash code.
         *
         * @return the int
         */
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
                + ((conletId == null) ? 0 : conletId.hashCode());
            return result;
        }

        /**
         * Two objects are equal if they have equal web console component ids.
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
            ConletBaseModel other = (ConletBaseModel) obj;
            if (conletId == null) {
                if (other.conletId != null) {
                    return false;
                }
            } else if (!conletId.equals(other.conletId)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Send to the portal page for adding or updating a complete web 
     * console component representation.
     */
    public class RenderConletFromReader extends RenderConlet {

        private final Future<String> content;

        /**
         * Creates a new event.
         *
         * @param request the request
         * @param conletClass the web console component class
         * @param conletId the id of the web console component
         * @param contentReader the content reader
         */
        public RenderConletFromReader(RenderConletRequestBase<?> request,
                Class<?> conletClass, String conletId, Reader contentReader) {
            super(conletClass, conletId);
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

        /**
         * Content.
         *
         * @return the future
         */
        @Override
        public Future<String> content() {
            return content;
        }
    }
}