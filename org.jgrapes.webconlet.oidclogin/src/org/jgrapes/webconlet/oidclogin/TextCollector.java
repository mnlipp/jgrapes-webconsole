/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

package org.jgrapes.webconlet.oidclogin;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.jgrapes.io.util.InputConsumer;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * Collects character data from buffers and makes it available as
 * a text.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TextCollector implements InputConsumer {
    private boolean isEof;
    private CharsetDecoder decoder;
    private Charset charset = StandardCharsets.UTF_8;
    private CharBuffer collected;
    private int maxLength = 8192;
    private Consumer<String> consumer = text -> {
    };

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    public TextCollector charset(Charset charset) {
        if (decoder != null) {
            throw new IllegalStateException("Charset cannot be changed.");
        }
        this.charset = charset;
        return this;
    }

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>` to the charset
     * specified as system property `native.encoding`. If this
     * property does not specify a valid charset, 
     * {@link Charset#defaultCharset()} is used.
     *  
     * Must be invoked before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @return the managed buffer reader
     */
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.EmptyCatchBlock" })
    public TextCollector nativeCharset() {
        Charset toSet = Charset.defaultCharset();
        var toCheck = System.getProperty("native.encoding");
        if (toCheck != null) {
            try {
                toSet = Charset.forName(toCheck);
            } catch (Exception e) {
                // If this fails, simply use default
            }
        }
        charset(toSet);
        return this;
    }

    /**
     * Configures the maximum length of the collected text. Input
     * exceeding this size will be discarded.
     *
     * @param maximumLength the maximum length
     * @return the maximum size
     */
    public TextCollector maximumSize(int maximumLength) {
        this.maxLength = maximumLength;
        return this;
    }

    /**
     * Configures a consumer for the collected text. The consumer 
     * is invoked once when the complete text is available.
     *
     * @param consumer the consumer
     * @return the line collector
     */
    public TextCollector consumer(Consumer<String> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Feed data to the collector. 
     * 
     * Calling this method with `null` as argument closes the feed.
     *
     * @param buffer the buffer
     */
    public <W extends Buffer> void feed(W buffer) {
        if (isEof) {
            return;
        }
        if (buffer == null) {
            isEof = true;
            collected.flip();
            consumer.accept(collected.toString());
            collected = null;
        } else {
            copyToCollected(buffer);
        }
    }

    /**
     * Feed data to the collector. 
     * 
     * Calling this method with `null` as argument closes the feed.
     *
     * @param buffer the buffer
     */
    @Override
    public <W extends Buffer> void feed(ManagedBuffer<W> buffer) {
        if (buffer == null) {
            feed((W) null);
        } else {
            feed(buffer.backingBuffer());
        }
    }

    private <W extends Buffer> void copyToCollected(W buffer) {
        try {
            buffer.mark();
            if (collected == null) {
                int size = buffer.capacity();
                if (size < maxLength && maxLength < 16_384) {
                    size = maxLength;
                }
                collected = CharBuffer.allocate(size);
            }
            if (collected.position() >= maxLength) {
                return;
            }
            if (buffer instanceof CharBuffer charBuf) {
                if (collected.remaining() < charBuf.remaining()) {
                    resizeCollected(charBuf);
                }
                collected.put(charBuf);
                return;
            }
            if (decoder == null) {
                decoder = charset.newDecoder();
            }
            while (true) {
                var result
                    = decoder.decode((ByteBuffer) buffer, collected, isEof);
                if (!result.isOverflow()) {
                    break;
                }
                // Need larger buffer
                resizeCollected(buffer);
            }
        } finally {
            buffer.reset();
        }
    }

    private void resizeCollected(Buffer toAppend) {
        var old = collected;
        collected = CharBuffer.allocate(old.capacity() + toAppend.capacity());
        old.flip();
        collected.put(old);
    }

    /**
     * Checks if more input may become available.
     *
     * @return true, if successful
     */
    public boolean eof() {
        return isEof;
    }
}
