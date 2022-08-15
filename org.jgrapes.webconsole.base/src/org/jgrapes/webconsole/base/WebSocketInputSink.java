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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.logging.Logger;
import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonDecodeException;
import org.jdrupes.json.JsonRpc;
import org.jdrupes.json.JsonRpc.DefaultJsonRpc;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferReader;
import org.jgrapes.webconsole.base.events.JsonInput;

/**
 * Assembles {@link Input} events until a complete
 * JSON message has been collected and then fires
 * the message as {@link JsonInput} event.
 */
public class WebSocketInputSink extends Thread {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(WebSocketInputSink.class.getName());

    private final WeakReference<EventPipeline> pipelineRef;
    private final WeakReference<ConsoleConnection> channelRef;
    private ManagedBufferReader jsonSource;

    private static ReferenceQueue<Object> abandoned
        = new ReferenceQueue<>();

    /**
     * Weak references to `T` that interrupt the input collecting
     * thread if the referent has been garbage collected.
     *
     * @param <T> the generic type
     */
    private static class RefWithThread<T> extends WeakReference<T> {
        public Thread watched;

        /**
         * Creates a new instance.
         *
         * @param referent the referent
         * @param thread the thread
         */
        public RefWithThread(T referent, Thread thread) {
            super(referent, abandoned);
            watched = thread;
        }
    }

    static {
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    @SuppressWarnings("unchecked")
                    WebSocketInputSink.RefWithThread<Object> ref
                        = (WebSocketInputSink.RefWithThread<Object>) abandoned
                            .remove();
                    ref.watched.interrupt();
                } catch (InterruptedException e) {
                    // Nothing to do
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();
    }

    /**
     * Instantiates a new web socket input reader.
     *
     * @param wsInPipeline the ws in pipeline
     * @param consoleChannel the web console channel
     */
    public WebSocketInputSink(EventPipeline wsInPipeline,
            ConsoleConnection consoleChannel) {
        pipelineRef
            = new WebSocketInputSink.RefWithThread<>(wsInPipeline, this);
        channelRef
            = new WebSocketInputSink.RefWithThread<>(consoleChannel, this);
        setDaemon(true);
    }

    /**
     * Forward the data to the JSON decoder.
     *
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void feed(ManagedBuffer<CharBuffer> input) throws IOException {
        // Delayed initialization, allows adaption to buffer size.
        if (jsonSource == null) {
            jsonSource = new ManagedBufferReader();
            start();
        }
        jsonSource.feed(input);
    }

    /**
     * Forward the close to the JSON decoder.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void close() throws IOException {
        if (jsonSource == null) {
            // Never started
            return;
        }
        jsonSource.close();
        try {
            join(1000);
        } catch (InterruptedException e) {
            // Just in case
            interrupt();
        }
    }

    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.DataflowAnomalyAnalysis",
        "PMD.GuardLogStatement", "PMD.CognitiveComplexity" })
    @Override
    public void run() {
        while (true) {
            JsonBeanDecoder jsonDecoder = JsonBeanDecoder.create(jsonSource);
            @SuppressWarnings("PMD.UnusedAssignment")
            JsonRpc rpc = null;
            try {
                rpc = jsonDecoder.readObject(DefaultJsonRpc.class);
            } catch (JsonDecodeException e) {
                logger.severe(
                    () -> toString() + " cannot decode request from console: "
                        + e.getMessage());
                break;
            }
            if (rpc == null) {
                break;
            }
            // Fully decoded JSON available.
            ConsoleConnection connection = channelRef.get();
            EventPipeline eventPipeline = pipelineRef.get();
            if (eventPipeline == null || connection == null) {
                break;
            }
            // WebConsole connection established, check for special disconnect
            if ("disconnect".equals(rpc.method())
                && connection.consoleConnectionId()
                    .equals(rpc.params().asString(0))) {
                connection.close();
                return;
            }
            // Ordinary message from web console (view) to server.
            connection.refresh();
            if ("keepAlive".equals(rpc.method())) {
                continue;
            }
            eventPipeline.fire(new JsonInput(rpc), connection);
        }
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder()
            .append(Components.objectName(this)).append(" [");
        Optional.ofNullable(channelRef.get()).ifPresentOrElse(
            c -> res.append(c.toString()),
            () -> res.append('?'));
        return res.append(']').toString();
    }

}