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
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.events.ConsoleLayoutChanged;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.LastConsoleLayout;
import org.jgrapes.webconsole.base.events.RenderConletRequest;

/**
 * A component that restores the console layout
 * using key/value events for persisting the data between sessions.
 * 
 * <img src="KVPPBootSeq.svg" alt="Boot Event Sequence">
 * 
 * This component requires another component that handles the key/value
 * store events ({@link KeyValueStoreUpdate}, {@link KeyValueStoreQuery})
 * used by this component for implementing persistence. When the web console 
 * becomesready, this policy sends a query for the persisted data.
 * 
 * When the web console has been prepared, the policy sends the last layout
 * as retrieved from persistent storage to the web console and then generates
 * render events for all web console components contained in this layout.
 * 
 * Each time the layout is changed in the web console, the web console sends 
 * the new layout data and this component updates the persistent storage
 * accordingly.
 * 
 * @startuml KVPPBootSeq.svg
 * hide footbox
 * 
 * actor System
 * System -> KVStoreBasedConsolePolicy: ConsolePrepared
 * activate KVStoreBasedConsolePolicy
 * KVStoreBasedConsolePolicy -> "KV Store": KeyValueStoreQuery
 * deactivate KVStoreBasedConsolePolicy
 * activate "KV Store"
 * "KV Store" -> KVStoreBasedConsolePolicy: KeyValueStoreData
 * deactivate "KV Store"
 * activate KVStoreBasedConsolePolicy
 * KVStoreBasedConsolePolicy -> WebConsole: LastConsoleLayout
 * activate WebConsole
 * WebConsole -> Browser: "lastConsoleLayout"
 * deactivate WebConsole
 * loop for all conlets to be displayed
 *     KVStoreBasedConsolePolicy -> ConletX: RenderConletRequest
 *     activate ConletX
 *     ConletX -> WebConsole: RenderConlet
 *     deactivate ConletX
 *     activate WebConsole
 *     WebConsole -> Browser: "renderConlet"
 *     deactivate WebConsole
 * end
 * deactivate KVStoreBasedConsolePolicy
 * 
 * Browser -> WebConsole: "consoleLayout"
 * activate WebConsole
 * WebConsole -> KVStoreBasedConsolePolicy: ConsoleLayoutChanged
 * deactivate WebConsole
 * activate KVStoreBasedConsolePolicy
 * KVStoreBasedConsolePolicy -> "KV Store": KeyValueStoreUpdate
 * deactivate KVStoreBasedConsolePolicy
 * activate "KV Store"
 * deactivate "KV Store"
 * 
 * @enduml
 */
public class KVStoreBasedConsolePolicy extends Component {

    /**
     * Creates a new component with its channel set to
     * itself.
     */
    public KVStoreBasedConsolePolicy() {
        // Everything done by super.
    }

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel
     */
    public KVStoreBasedConsolePolicy(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Create browser session scoped storage and forward event to it.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onConsolePrepared(
            ConsolePrepared event, ConsoleConnection channel) {
        ((KVStoredLayoutData) channel.session().transientData()
            .computeIfAbsent(KVStoredLayoutData.class,
                key -> new KVStoredLayoutData(channel.session())))
                    .onConsolePrepared(event, channel);
    }

    /**
     * Forward layout changed event to browser session scoped storage.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleLayoutChanged(ConsoleLayoutChanged event,
            ConsoleConnection channel) throws IOException {
        Optional<KVStoredLayoutData> optDs = Optional.ofNullable(
            (KVStoredLayoutData) channel.session().transientData()
                .get(KVStoredLayoutData.class));
        if (optDs.isPresent()) {
            optDs.get().onConsoleLayoutChanged(event, channel);
        }
    }

    /**
     * Caches the data in the session.
     */
    @SuppressWarnings("PMD.CommentRequired")
    private class KVStoredLayoutData {

        private final String storagePath;
        private Map<String, Object> persisted;

        public KVStoredLayoutData(Session session) {
            storagePath = "/"
                + WebConsoleUtils.userFromSession(session)
                    .map(ConsoleUser::getName).orElse("")
                + "/" + KVStoreBasedConsolePolicy.class.getName();
        }

        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.AvoidInstantiatingObjectsInLoops" })
        public void onConsolePrepared(
                ConsolePrepared event, IOSubchannel channel) {
            KeyValueStoreQuery query = new KeyValueStoreQuery(
                storagePath, channel);
            Event.onCompletion(query, e -> onQueryCompleted(e, channel,
                event.event().renderSupport()));
            fire(query, channel);
        }

        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public void onQueryCompleted(KeyValueStoreQuery query,
                IOSubchannel channel, RenderSupport renderSupport) {
            try {
                String data = Optional.ofNullable(query.get())
                    .flatMap(m -> Optional.ofNullable(m.get(storagePath)))
                    .orElse(null);
                if (data == null) {
                    persisted = new HashMap<>();
                } else {
                    JsonBeanDecoder decoder = JsonBeanDecoder.create(data);
                    @SuppressWarnings({ "unchecked", "PMD.LooseCoupling" })
                    Class<Map<String, Object>> cls
                        = (Class<Map<String, Object>>) (Class<?>) HashMap.class;
                    persisted = decoder.readObject(cls);
                }
            } catch (InterruptedException | JsonDecodeException e) {
                persisted = new HashMap<>();
            }

            // Make sure data is consistent
            @SuppressWarnings("unchecked")
            List<String> previewLayout = (List<String>) persisted
                .computeIfAbsent("previewLayout",
                    newKey -> Collections.emptyList());
            @SuppressWarnings("unchecked")
            List<String> tabsLayout = (List<String>) persisted.computeIfAbsent(
                "tabsLayout", newKey -> Collections.emptyList());
            JsonObject xtraInfo = (JsonObject) persisted.computeIfAbsent(
                "xtraInfo", newKey -> JsonObject.create());

            // Update (now consistent) layout
            channel.respond(new LastConsoleLayout(
                previewLayout, tabsLayout, xtraInfo));

            // Restore conlets
            for (String conletId : tabsLayout) {
                fire(new RenderConletRequest(renderSupport, conletId,
                    RenderMode.asSet(RenderMode.View)), channel);
            }
            for (String conletId : previewLayout) {
                fire(new RenderConletRequest(renderSupport, conletId,
                    RenderMode.asSet(RenderMode.Preview,
                        RenderMode.Foreground)),
                    channel);
            }
        }

        public void onConsoleLayoutChanged(ConsoleLayoutChanged event,
                IOSubchannel channel) throws IOException {
            persisted.put("previewLayout", event.previewLayout());
            persisted.put("tabsLayout", event.tabsLayout());
            persisted.put("xtraInfo", event.xtraInfo());

            // Now store.
            @SuppressWarnings("PMD.CloseResource")
            JsonBeanEncoder encoder = JsonBeanEncoder.create();
            encoder.writeObject(persisted);
            fire(new KeyValueStoreUpdate()
                .update(storagePath, encoder.toJson()), channel);
        }

    }
}
