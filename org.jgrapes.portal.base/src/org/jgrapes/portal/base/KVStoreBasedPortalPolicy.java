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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jdrupes.json.JsonObject;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.base.Portlet.RenderMode;
import org.jgrapes.portal.base.events.LastPortalLayout;
import org.jgrapes.portal.base.events.PortalLayoutChanged;
import org.jgrapes.portal.base.events.PortalPrepared;
import org.jgrapes.portal.base.events.PortalReady;
import org.jgrapes.portal.base.events.RenderPortletRequest;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * A component that restores the portal layout,
 * using key/value events for persisting the data between sessions.
 * 
 * ![Boot Event Sequence](KVPPBootSeq.svg)
 * 
 * This component requires another component that handles the key/value
 * store events ({@link KeyValueStoreUpdate}, {@link KeyValueStoreQuery})
 * used by this component for implementing persistence. When the portal becomes
 * ready, this policy sends a query for the persisted data.
 * 
 * When the portal has been prepared, the policy sends the last layout
 * as retrieved from persistent storage to the portal and then generates
 * render events for all portlets contained in this layout.
 * 
 * Each time the layout is changed in the portal, the portal sends the
 * new layout data and this component updates the persistent storage
 * accordingly.
 * 
 * @startuml KVPPBootSeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "portalReady"
 * activate Portal
 * Portal -> KVStoreBasedPortalPolicy: PortalReady
 * deactivate Portal
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> "KV Store": KeyValueStoreQuery
 * activate "KV Store"
 * "KV Store" -> KVStoreBasedPortalPolicy: KeyValueStoreData
 * deactivate "KV Store"
 * deactivate KVStoreBasedPortalPolicy
 * 
 * actor System
 * System -> KVStoreBasedPortalPolicy: PortalPrepared
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> Portal: LastPortalLayout
 * activate Portal
 * Portal -> Browser: "lastPortalLayout"
 * deactivate Portal
 * loop for all portlets to be displayed
 *     KVStoreBasedPortalPolicy -> PortletX: RenderPortletRequest
 *     activate PortletX
 *     PortletX -> Portal: RenderPortlet
 *     deactivate PortletX
 *     activate Portal
 *     Portal -> Browser: "renderPortlet"
 *     deactivate Portal
 * end
 * deactivate KVStoreBasedPortalPolicy
 * 
 * Browser -> Portal: "portalLayout"
 * activate Portal
 * Portal -> KVStoreBasedPortalPolicy: PortalLayoutChanged
 * deactivate Portal
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> "KV Store": KeyValueStoreUpdate
 * deactivate KVStoreBasedPortalPolicy
 * 
 * @enduml
 */
public class KVStoreBasedPortalPolicy extends Component {

    /**
     * Creates a new component with its channel set to
     * itself.
     */
    public KVStoreBasedPortalPolicy() {
        // Everything done by super.
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel
     */
    public KVStoreBasedPortalPolicy(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Intercept the {@link PortalReady} event. Request the 
     * session data from the key/value store and resume.
     * 
     * @param event
     * @param channel
     * @throws InterruptedException
     */
    @Handler
    public void onPortalReady(PortalReady event, PortalSession channel)
            throws InterruptedException {
        PortalSessionDataStore sessionDs = channel.associated(
            PortalSessionDataStore.class,
            () -> new PortalSessionDataStore(channel.browserSession()));
        sessionDs.onPortalReady(event, channel);
    }

    /**
     * Handle returned data.
     *
     * @param event the event
     * @param channel the channel
     * @throws JsonDecodeException the json decode exception
     */
    @Handler
    public void onKeyValueStoreData(
            KeyValueStoreData event, PortalSession channel)
            throws JsonDecodeException {
        Optional<PortalSessionDataStore> optSessionDs
            = channel.associated(PortalSessionDataStore.class);
        if (optSessionDs.isPresent()) {
            optSessionDs.get().onKeyValueStoreData(event, channel);
        }
    }

    /**
     * Handle portal page loaded.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onPortalPrepared(
            PortalPrepared event, PortalSession channel) {
        channel.associated(PortalSessionDataStore.class).ifPresent(
            psess -> psess.onPortalPrepared(event, channel));
    }

    /**
     * Handle changed layout.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onPortalLayoutChanged(PortalLayoutChanged event,
            PortalSession channel) throws IOException {
        Optional<PortalSessionDataStore> optDs = channel.associated(
            PortalSessionDataStore.class);
        if (optDs.isPresent()) {
            optDs.get().onPortalLayoutChanged(event, channel);
        }
    }

    /**
     * Stores the data for the portal session.
     */
    @SuppressWarnings("PMD.CommentRequired")
    private class PortalSessionDataStore {

        private final String storagePath;
        private Map<String, Object> persisted;

        public PortalSessionDataStore(Session session) {
            storagePath = "/"
                + PortalUtils.userFromSession(session)
                    .map(UserPrincipal::toString).orElse("")
                + "/" + KVStoreBasedPortalPolicy.class.getName();
        }

        public void onPortalReady(PortalReady event, IOSubchannel channel)
                throws InterruptedException {
            if (persisted != null) {
                return;
            }
            KeyValueStoreQuery query = new KeyValueStoreQuery(
                storagePath, channel);
            fire(query, channel);
        }

        public void onKeyValueStoreData(
                KeyValueStoreData event, IOSubchannel channel)
                throws JsonDecodeException {
            if (!event.event().query().equals(storagePath)) {
                return;
            }
            String data = event.data().get(storagePath);
            if (data != null) {
                JsonBeanDecoder decoder = JsonBeanDecoder.create(data);
                @SuppressWarnings({ "unchecked", "PMD.LooseCoupling" })
                Class<Map<String, Object>> cls
                    = (Class<Map<String, Object>>) (Class<?>) HashMap.class;
                persisted = decoder.readObject(cls);
            }
        }

        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.AvoidInstantiatingObjectsInLoops" })
        public void onPortalPrepared(
                PortalPrepared event, IOSubchannel channel) {
            if (persisted == null) {
                // Retrieval was not successful
                persisted = new HashMap<>();
            }
            // Make sure data is consistent
            @SuppressWarnings("unchecked")
            List<String> previewLayout = (List<String>) persisted
                .computeIfAbsent("previewLayout",
                    newKey -> {
                        return Collections.emptyList();
                    });
            @SuppressWarnings("unchecked")
            List<String> tabsLayout = (List<String>) persisted.computeIfAbsent(
                "tabsLayout", newKey -> {
                    return Collections.emptyList();
                });
            JsonObject xtraInfo = (JsonObject) persisted.computeIfAbsent(
                "xtraInfo", newKey -> {
                    return JsonObject.create();
                });

            // Update layout
            channel.respond(new LastPortalLayout(
                previewLayout, tabsLayout, xtraInfo));

            // Restore portlets
            for (String portletId : tabsLayout) {
                fire(new RenderPortletRequest(
                    event.event().renderSupport(), portletId,
                    Arrays.asList(new RenderMode[] { RenderMode.View })),
                    channel);
            }
            for (String portletId : previewLayout) {
                fire(new RenderPortletRequest(
                    event.event().renderSupport(), portletId,
                    Arrays.asList(new RenderMode[] { RenderMode.Foreground })),
                    channel);
            }
        }

        public void onPortalLayoutChanged(PortalLayoutChanged event,
                IOSubchannel channel) throws IOException {
            persisted.put("previewLayout", event.previewLayout());
            persisted.put("tabsLayout", event.tabsLayout());
            persisted.put("xtraInfo", event.xtraInfo());

            // Now store.
            JsonBeanEncoder encoder = JsonBeanEncoder.create();
            encoder.writeObject(persisted);
            fire(new KeyValueStoreUpdate()
                .update(storagePath, encoder.toJson()), channel);
        }

    }
}
