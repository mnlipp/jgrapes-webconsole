/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
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

package org.jgrapes.webconlet.logviewer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * The Class LogViewerHandler.
 */
public class LogViewerHandler extends Handler {

    private static List<LogRecord> buffer = new LinkedList<>();
    private static LogViewerConlet conlet;

    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        synchronized (buffer) {
            buffer.add(record);
            while (buffer.size() > 100) {
                buffer.remove(0);
            }
        }
        if (conlet != null) {
            conlet.addEntry(record);
        }
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    /* default */ static List<LogRecord> setConlet(LogViewerConlet conlet) {
        synchronized (buffer) {
            LogViewerHandler.conlet = conlet;
            return new ArrayList<>(buffer);
        }
    }

    @Override
    public void flush() {
        // Nothing to do.
    }

    @Override
    public void close() {
        buffer.clear();
    }

}
