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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.TypedIdKey;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.util.events.KeyValueStoreUpdate.Action;
import org.jgrapes.util.events.KeyValueStoreUpdate.Deletion;
import org.jgrapes.util.events.KeyValueStoreUpdate.Update;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.JsonInput;
import org.jgrapes.webconsole.base.events.SimpleConsoleCommand;

/**
 * The Class BrowserLocalBackedKVStore.
 */
public class BrowserLocalBackedKVStore extends Component {

    private final String consolePrefix;

    /**
     * Create a new key/value store that uses the browser's local storage
     * for persisting the values.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     * @param consolePrefix the web console's prefix as returned by
     * {@link ConsoleWeblet#prefix()}, i.e. staring and ending with a slash
     */
    public BrowserLocalBackedKVStore(
            Channel componentChannel, String consolePrefix) {
        super(componentChannel);
        this.consolePrefix = consolePrefix;
    }

    /**
     * Intercept {@link ConsoleReady} event to first get data.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     */
    @Handler(priority = 1000)
    public void onConsoleReady(ConsoleReady event, ConsoleConnection channel)
            throws InterruptedException {
        // Put there by onJsonInput if retrieval has been done.
        if (TypedIdKey.get(channel.session(), Store.class,
            consolePrefix).isPresent()) {
            // Already have store, nothing to do
            return;
        }
        // Suspend and trigger data retrieval
        event.suspendHandling();
        channel.setAssociated(this, event);
        String keyStart = consolePrefix
            + BrowserLocalBackedKVStore.class.getName() + "/";
        channel
            .respond(new SimpleConsoleCommand("retrieveLocalData", keyStart));
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private Store getStore(ConsoleConnection channel) {
        return TypedIdKey
            .get(channel.session(), Store.class, consolePrefix)
            .orElseGet(
                () -> TypedIdKey.put(channel.session(), consolePrefix,
                    new Store()));
    }

    /**
     * Evaluate "retrievedLocalData" response.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void onJsonInput(JsonInput event, ConsoleConnection channel)
            throws InterruptedException, IOException {
        if (!"retrievedLocalData".equals(event.request().method())) {
            return;
        }
        channel.associated(this, ConsoleReady.class)
            .ifPresent(origEvent -> {
                // We have intercepted the web console ready event, fill store.
                // Having a store now also shows that retrieval has been done.
                final String keyStart = consolePrefix
                    + BrowserLocalBackedKVStore.class.getName() + "/";
                @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
                    "PMD.LooseCoupling" })
                Store data = getStore(channel);
                String[][] values = event.request().param(0);
                Arrays.stream(values).forEach(item -> {
                    String key = item[0];
                    if (key.startsWith(keyStart)) {
                        data.put(key.substring(keyStart.length() - 1), item[1]);
                    }
                });
                // Don't re-use
                channel.setAssociated(this, null);
                // Let others process the web console ready event
                origEvent.resumeHandling();
            });
    }

    /**
     * Handle data update events.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public void onKeyValueStoreUpdate(
            KeyValueStoreUpdate event, ConsoleConnection channel)
            throws InterruptedException, IOException {
        @SuppressWarnings("PMD.LooseCoupling")
        Store data = getStore(channel);
        List<String[]> actions = new ArrayList<>();
        String keyStart = consolePrefix
            + BrowserLocalBackedKVStore.class.getName();
        for (Action action : event.actions()) {
            @SuppressWarnings("PMD.UselessParentheses")
            String key = keyStart + (action.key().startsWith("/")
                ? action.key()
                : ("/" + action.key()));
            if (action instanceof Update) {
                actions.add(new String[] { "u", key,
                    ((Update) action).value() });
                data.put(action.key(), ((Update) action).value());
            } else if (action instanceof Deletion) {
                actions.add(new String[] { "d", key });
                data.remove(action.key());
            }
        }
        channel.respond(new SimpleConsoleCommand("storeLocalData",
            new Object[] { actions.toArray() }));
    }

    /**
     * Handle data query..
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    @SuppressWarnings("PMD.ConfusingTernary")
    public void onKeyValueStoreQuery(
            KeyValueStoreQuery event, ConsoleConnection channel) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> result = new HashMap<>();
        TypedIdKey.get(channel.session(), Store.class, consolePrefix)
            .ifPresent(data -> {
                if (!event.query().endsWith("/")) {
                    // Single value
                    if (data.containsKey(event.query())) {
                        result.put(event.query(), data.get(event.query()));
                    }
                } else {
                    for (Map.Entry<String, String> e : data.entrySet()) {
                        if (e.getKey().startsWith(event.query())) {
                            result.put(e.getKey(), e.getValue());
                        }
                    }
                }
                event.setResult(result);
            });
    }

    /**
     * The store.
     */
    @SuppressWarnings("serial")
    private final class Store extends HashMap<String, String> {
    }
}
