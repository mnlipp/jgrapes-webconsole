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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferReader;
import org.jgrapes.io.util.ThreadCleaner;
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
    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ObjectMapper mapper = JsonMapper.builder()
        .addModule(new ParameterNamesModule()).addModule(new Jdk8Module())
        .addModule(new JavaTimeModule()).build();

    private final WeakReference<ConsoleConnection> channelRef;
    private final WeakReference<EventPipeline> pipelineRef;
    private ManagedBufferReader jsonSource;

    /**
     * Instantiates a new web socket input reader.
     *
     * @param wsInPipeline the ws in pipeline
     * @param consoleChannel the web console channel
     */
    public WebSocketInputSink(EventPipeline wsInPipeline,
            ConsoleConnection consoleChannel) {
        channelRef = new WeakReference<>(consoleChannel);
        pipelineRef = new WeakReference<>(wsInPipeline);
        ThreadCleaner.watch(wsInPipeline, this);
        ThreadCleaner.watch(consoleChannel, this);
    }

    /**
     * Forward the data to the JSON decoder.
     *
     * @param input the data to be converted
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void feed(ManagedBuffer<CharBuffer> input) throws IOException {
        // Delayed initialization, allows adaption to buffer size.
        if (jsonSource == null) {
            jsonSource = new ManagedBufferReader();
            (Components.useVirtualThreads() ? ofVirtual()
                : ofPlatform()).start(this);
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
        @SuppressWarnings("PMD.CloseResource")
        JsonParser parser;
        try {
            parser = new JsonFactory().createParser(jsonSource);
        } catch (IOException e) {
            logger.severe(() -> toString() + " cannot create JSON parser: "
                + e.getMessage());
            return;
        }
        while (true) {
            JsonRpc rpc;
            try {
                if (parser.nextToken() == null) {
                    break;
                }
                rpc = mapper.readValue(parser, WcJsonRpc.class);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, () -> toString()
                    + " cannot read JSON: " + e.getMessage());
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
                && connection.consoleConnectionId().equals(rpc.param(0))) {
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