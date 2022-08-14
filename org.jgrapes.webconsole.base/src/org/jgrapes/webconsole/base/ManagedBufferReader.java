/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.base;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Objects;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * A {@link Reader} that provides the data from the {@link ManagedBuffer}s
 * fed to it to a consumer. This class is intended to be used as a pipe 
 * between two threads.  
 */
public class ManagedBufferReader extends Reader {

    private boolean isOpen = true;
    private ManagedBuffer<CharBuffer> current;

    /**
     * Feed data to the reader. The call blocks while data from a previous
     * invocation has not been fully read. The buffer passed as argument
     * is locked (see {@link ManagedBuffer#lockBuffer()}) until all
     * data has been read.
     *
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void feed(ManagedBuffer<CharBuffer> buffer) throws IOException {
        synchronized (lock) {
            if (!isOpen) {
                throw new IOException("Reader is closed.");
            }
            while (current != null) {
                try {
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    var exc = new InterruptedIOException(e.getMessage());
                    exc.setStackTrace(e.getStackTrace());
                    throw exc;
                }
            }
            current = buffer;
            buffer.lockBuffer();
            lock.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            isOpen = false;
            if (current != null) {
                current.unlockBuffer();
                current = null;
            }
            lock.notifyAll();
        }
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public int read(char[] cbuf, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, cbuf.length);
        synchronized (lock) {
            while (isOpen && current == null) {
                try {
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    var exc = new InterruptedIOException(e.getMessage());
                    exc.setStackTrace(e.getStackTrace());
                    throw exc;
                }
            }
            if (!isOpen) {
                return -1;
            }
            int transferred;
            if (current.remaining() <= len) {
                // Get all remaining.
                transferred = current.remaining();
                current.backingBuffer().get(cbuf, off, transferred);
                current.unlockBuffer();
                current = null;
                lock.notifyAll();
            } else {
                // Get requested.
                transferred = len;
                current.backingBuffer().get(cbuf, off, transferred);
            }
            return transferred;
        }
    }

}
