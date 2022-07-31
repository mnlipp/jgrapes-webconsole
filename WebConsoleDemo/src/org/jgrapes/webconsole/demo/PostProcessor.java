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

package org.jgrapes.webconsole.demo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.util.FormUrlDecoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * The Class PostProcessor.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class PostProcessor extends Component {

    /**
     * The form context data.
     */
    protected static class FormContext {
        public HttpRequest request;
        public Session session;
        public FormUrlDecoder fieldDecoder = new FormUrlDecoder();
    }

    /**
     * Instantiates a new post processor.
     *
     * @param componentChannel the component channel
     */
    public PostProcessor(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On post.
     *
     * @param event the event
     * @param channel the channel
     */
    @RequestHandler(patterns = "/form")
    public void onPost(Request.In.Post event, IOSubchannel channel) {
        FormContext ctx = channel
            .associated(this, FormContext::new);
        ctx.request = event.httpRequest();
        ctx.session = Session.from(event);
        event.stop();
    }

    /**
     * On input.
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    @Handler
    public void onInput(Input<ByteBuffer> event, IOSubchannel channel)
            throws InterruptedException, UnsupportedEncodingException {
        Optional<FormContext> ctx = channel.associated(this, FormContext.class);
        if (!ctx.isPresent()) {
            return;
        }
        ctx.get().fieldDecoder.addData(event.buffer().backingBuffer());
        if (!event.isEndOfRecord()) {
            return;
        }

        long invocations = (Long) ctx.get().session.computeIfAbsent(
            "invocations", key -> {
                return 0L;
            });
        ctx.get().session.put("invocations", invocations + 1);

        HttpResponse response = ctx.get().request.response().get();
        response.setStatus(HttpStatus.OK);
        response.setHasPayload(true);
        response.setField(HttpField.CONTENT_TYPE,
            MediaType.builder().setType("text", "plain")
                .setParameter("charset", "utf-8").build());
        String data = "First name: "
            + ctx.get().fieldDecoder.fields().get("firstname")
            + "\r\n" + "Last name: "
            + ctx.get().fieldDecoder.fields().get("lastname")
            + "\r\n" + "Previous invocations: " + invocations;
        channel.respond(new Response(response));
        ManagedBuffer<ByteBuffer> out = channel.byteBufferPool().acquire();
        out.backingBuffer().put(data.getBytes("utf-8"));
        channel.respond(Output.fromSink(out, true));
    }

}
