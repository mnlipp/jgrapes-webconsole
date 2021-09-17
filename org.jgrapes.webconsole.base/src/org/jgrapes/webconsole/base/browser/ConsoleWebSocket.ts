/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2021  Michael N. Lipp
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
import Console from "./Console";
import Log from "./Log";
import RenderMode from "./RenderMode";

/**
 * Defines a wrapper class for a web socket. An instance 
 * creates and maintains a connection to a session on the
 * server, i.e. it reconnects automatically to the session
 * when the connection is lost. The connection is used to
 * exchange JSON RPC notifications.
 */
export default class ConsoleWebSocket {

    private _debugHandler = false;
    private _console: Console;
    private _ws: WebSocket | null = null;
    private _sendQueue: string[] = [];
    private _recvQueue: any[] = [];
    private _recvQueueLocks = 0;
    private _messageHandlers = new Map<string, (...args: any[]) => void>();
    private _isHandling = false;
    private _refreshTimer: ReturnType<typeof setInterval> | null = null;
    private _inactivity = 0;
    private _reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    private _connectRequested = false;
    private _initialConnect = true;
    private _consoleSessionId: string | null = null;
    private _connectionLost = false;
    private _oldConsoleSessionId: string | null;

    constructor(console: Console) {
        this._console = console;
        this._oldConsoleSessionId = sessionStorage.getItem(
            "org.jgrapes.webconsole.base.sessionId");
    };

    /**
     * Returns the unique session id used to identify the connection.
     * 
     * @return the id
     */
    consoleSessionId(): string | null {
        return this._consoleSessionId;
    }

    _connect() {
        let location = (window.location.protocol === "https:" ? "wss" : "ws") +
            "://" + window.location.host + window.location.pathname;
        if (!location.endsWith("/")) {
            location += "/";
        }
        this._consoleSessionId = sessionStorage.getItem(
            "org.jgrapes.webconsole.base.sessionId");
        location += "console-session/" + this._consoleSessionId;
        if (this._oldConsoleSessionId) {
            location += "?was=" + this._oldConsoleSessionId;
            this._oldConsoleSessionId = null;
        }
        Log.debug("Creating WebSocket for " + location);
        this._ws = new WebSocket(location);
        Log.debug("Created WebSocket with readyState " + this._ws.readyState);
        let _this = this;
        this._ws.onopen = function() {
            Log.debug("OnOpen called for WebSocket.");
            if (_this._connectionLost) {
                _this._connectionLost = false;
                _this._console.connectionRestored();
            }
            _this._drainSendQueue();
            if (_this._initialConnect) {
                _this._initialConnect = false;
            } else {
                // Make sure to get any lost updates
                _this._console.findPreviewIds().forEach(function(id) {
                    _this._console.renderConlet(id, [RenderMode.Preview]);
                });
                _this._console.findViewIds().forEach(function(id) {
                    _this._console.renderConlet(id!, [RenderMode.View]);
                });
            }
            _this._refreshTimer = setInterval(function() {
                if (_this._sendQueue.length == 0) {
                    _this._inactivity += _this._console.sessionRefreshInterval;
                    if (_this._console.sessionInactivityTimeout > 0 &&
                        _this._inactivity >= _this._console.sessionInactivityTimeout) {
                        _this.close();
                        _this._console.connectionSuspended(function() {
                            _this.connect();
                        });
                        return;
                    }
                    _this._send({
                        "jsonrpc": "2.0", "method": "keepAlive",
                        "params": []
                    });
                }
            }, _this._console.sessionRefreshInterval);
        }
        this._ws.onclose = function(event) {
            Log.debug("OnClose called for WebSocket (reconnect: " +
                _this._connectRequested + ").");
            if (_this._refreshTimer !== null) {
                clearInterval(_this._refreshTimer);
                _this._refreshTimer = null;
            }
            if (_this._connectRequested) {
                // Not an intended disconnect
                if (!_this._connectionLost) {
                    _this._console.connectionLost();
                    _this._connectionLost = true;
                }
                _this._initiateReconnect();
            }
        }
        // "onClose" is called even if the initial connection fails,
        // so we don't need "onError".
        // this._ws.onerror = function(event) {
        // }
        this._ws.onmessage = function(event) {
            var msg;
            try {
                msg = JSON.parse(event.data);
            } catch (e: any) {
                Log.error(e.name + ":" + e.lineNumber + ":" + e.columnNumber
                    + ": " + e.message + ". Data: ");
                Log.error(event.data);
                return;
            }
            _this._recvQueue.push(msg);
            if (!_this._isHandling) {
                _this._handleMessages();
            }
        }
    }

    /**
     * Establishes the connection.
     */
    connect() {
        this._connectRequested = true;
        let _this = this;
        window.addEventListener('beforeunload', function() {
            Log.debug("Closing WebSocket due to page unload");
            // Internal connect, don't send disconnect
            _this._connectRequested = false;
            _this._ws?.close();
        });
        this._connect();
    }

    /**
     * Closes the connection.
     */
    close() {
        if (this._consoleSessionId) {
            this._send({
                "jsonrpc": "2.0", "method": "disconnect",
                "params": [this._consoleSessionId]
            });
        }
        this._connectRequested = false;
        this._ws?.close();
    }

    _initiateReconnect() {
        if (!this._reconnectTimer) {
            let _this = this;
            this._reconnectTimer = setTimeout(function() {
                _this._reconnectTimer = null;
                _this._connect();
            }, 1000);
        }
    }

    _drainSendQueue() {
        while (this._ws && this._ws.readyState == this._ws.OPEN 
            && this._sendQueue.length > 0) {
            let msg = this._sendQueue[0];
            try {
                this._ws.send(msg);
                this._sendQueue.shift();
            } catch (e) {
                Log.warn(e);
            }
        }
    }

    _send(data: any) {
        this._sendQueue.push(JSON.stringify(data));
        this._drainSendQueue();
    }

    /**
     * Convert the passed object to its JSON representation
     * and sends it to the server. The object should represent
     * a JSON RPC notification.
     * 
     * @param data the data
     */
    send(data: any) {
        this._inactivity = 0;
        this._send(data);
    }

    /**
     * When a JSON RPC notification is received, its method property
     * is matched against all added handlers. If a match is found,
     * the associated handler function is invoked with the
     * params values from the notification as parameters.
     * 
     * @param method the method property to match
     * @param handler the handler function
     */
    addMessageHandler(method: string, handler: ((...args: any[]) => void)) {
        this._messageHandlers.set(method, handler);
    }

    _handlerLog(msgSup: () => string) {
        if (this._debugHandler) {
            Log.debug(msgSup());
        }
    }

    _handleMessages() {
        if (this._isHandling) {
            return;
        }
        this._isHandling = true;
        while (true) {
            if (this._recvQueueLocks > 0) {
                this._handlerLog(() => "Handler receive queue locked.");
                break;
            }
            if (this._recvQueue.length === 0) {
                this._handlerLog(() => "Handler receive queue empty.");
                break;
            }
            var message = this._recvQueue.shift();
            var handler = this._messageHandlers.get(message.method);
            if (!handler) {
                Log.error("No handler for invoked method " + message.method);
                continue;
            }
            if (message.hasOwnProperty("params")) {
                this._handlerLog(() => "Handling: " + message.method
                    + "[" + message.params + "]");
                handler(...message.params);
            } else {
                this._handlerLog(() => "Handling: " + message.method);
                handler();
            }
        }
        this._isHandling = false;
    }

    lockMessageReceiver() {
        this._recvQueueLocks += 1;
        if (this._debugHandler) {
            try {
                throw new Error("Lock");
            } catch (e: any) {
                Log.debug("Locking receiver:\n" + e.stack);
            }
        }
    }

    unlockMessageReceiver() {
        this._recvQueueLocks -= 1;
        if (this._debugHandler) {
            try {
                throw new Error("Unlock");
            } catch (e: any) {
                Log.debug("Unlocking receiver:\n" + e.stack);
            }
        }
        if (this._recvQueueLocks == 0) {
            this._handleMessages();
        }
    }
}

