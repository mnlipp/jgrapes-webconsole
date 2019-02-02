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
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonDecodeException;
import org.jdrupes.json.JsonRpc;
import org.jdrupes.json.JsonRpc.DefaultJsonRpc;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.events.Input;
import org.jgrapes.portal.base.events.JsonInput;

/**
 * Assembles {@link Input} events until a complete
 * JSON message has been collected and then fires
 * the message as {@link JsonInput} event.
 */
public class WebSocketInputReader extends Thread {

    private final WeakReference<EventPipeline> pipelineRef;
    private final WeakReference<PortalSession> channelRef;
    private PipedWriter decodeIn;
    private Reader jsonSource;

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
                    WebSocketInputReader.RefWithThread<Object> ref
                        = (WebSocketInputReader.RefWithThread<Object>) abandoned
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
     * @param portalChannel the portal channel
     */
    public WebSocketInputReader(EventPipeline wsInPipeline,
            PortalSession portalChannel) {
        pipelineRef
            = new WebSocketInputReader.RefWithThread<>(wsInPipeline, this);
        channelRef
            = new WebSocketInputReader.RefWithThread<>(portalChannel, this);
        setDaemon(true);
    }

    /**
     * Forward the data to the JSON decoder.
     *
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void write(CharBuffer buffer) throws IOException {
        // Delayed initialization, allows adaption to buffer size.
        if (decodeIn == null) {
            decodeIn = new PipedWriter();
            jsonSource = new PipedReader(decodeIn, buffer.capacity());
            start();
        }
        decodeIn.append(buffer);
        decodeIn.flush();
    }

    /**
     * Forward the close to the JSON decoder.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void close() throws IOException {
        if (decodeIn == null) {
            // Never started
            return;
        }
        decodeIn.close();
        try {
            join(1000);
        } catch (InterruptedException e) {
            // Just in case
            interrupt();
        }
    }

    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.DataflowAnomalyAnalysis" })
    @Override
    public void run() {
        while (true) {
            JsonBeanDecoder jsonDecoder = JsonBeanDecoder.create(jsonSource);
            JsonRpc rpc = null;
            try {
                rpc = jsonDecoder.readObject(DefaultJsonRpc.class);
            } catch (JsonDecodeException e) {
                break;
            }
            if (rpc == null) {
                break;
            }
            // Fully decoded JSON available.
            PortalSession portalSession = channelRef.get();
            EventPipeline eventPipeline = pipelineRef.get();
            if (eventPipeline == null || portalSession == null) {
                break;
            }
            // Portal session established, check for special disconnect
            if ("disconnect".equals(rpc.method())
                && portalSession.portalSessionId().equals(
                    rpc.params().asString(0))) {
                portalSession.discard();
                return;
            }
            // Ordinary message from portal (view) to server.
            portalSession.refresh();
            if ("keepAlive".equals(rpc.method())) {
                continue;
            }
            eventPipeline.fire(new JsonInput(rpc), portalSession);
        }
    }
}