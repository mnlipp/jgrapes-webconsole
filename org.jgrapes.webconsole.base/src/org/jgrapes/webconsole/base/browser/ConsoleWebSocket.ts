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
class ConsoleWebSocket {

    private _debugHandler = false;
    private _console: Console;
    private _ws: WebSocket | null = null;
    private _sendQueue: string[] = [];
    private _recvQueue: any[] = [];
    private _recvQueueLocks = 0;
    private _messageHandlers = new Map<string, (...args: any[]) => void>();
    private _isHandling = false;
    private _inactivityTimer: ReturnType<typeof setInterval> | null = null;
    private _lastActivityAt: number = Date.now();
    private _lastSendAt: number = 0;
    private _reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    private _connectRequested = false;
    private _initialConnect = true;
    private _connectionId: string | null = null;
    private _connectionLost = false;
    private _oldConnectionId: string | null;
    private _beingHandled: any;

    constructor(console: Console) {
        this._console = console;
        this._oldConnectionId = sessionStorage.getItem(
            "org.jgrapes.webconsole.base.connectionId");
    }

    /**
     * Returns the unique id used to identify the connection.
     * 
     * @return the id
     */
    connectionId(): string | null {
        return this._connectionId;
    }

    _connect() {
        let location = (window.location.protocol === "https:" ? "wss" : "ws") +
            "://" + window.location.host + window.location.pathname;
        if (!location.endsWith("/")) {
            location += "/";
        }
        this._connectionId = sessionStorage.getItem(
            "org.jgrapes.webconsole.base.connectionId");
        location += "console-connection/" + this._connectionId;
        if (this._oldConnectionId) {
            location += "?was=" + this._oldConnectionId;
            this._oldConnectionId = null;
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
                _this._console.findPreviews().forEach(function(conlet) {
                    _this._console.renderConlet(conlet.id(), [RenderMode.Preview]);
                });
                _this._console.findViews().forEach(function(conlet) {
                    _this._console.renderConlet(conlet.id(), [RenderMode.View]);
                });
            }
            // Activate timer
            _this._lastSendAt = Date.now();
            _this._rescheduleInactivityCheck();
        }
        this._ws.onclose = function(event) {
            Log.debug("OnClose called for WebSocket (reconnect: " +
                _this._connectRequested + ").");
            if (_this._inactivityTimer !== null) {
                clearInterval(_this._inactivityTimer);
                _this._inactivityTimer = null;
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
        if (this._connectionId) {
            this._send({
                "jsonrpc": "2.0", "method": "disconnect",
                "params": [this._connectionId]
            });
        }
        this._connectRequested = false;
        this._ws?.close();
    }

    _clearInactivityCheck() {
        if (this._inactivityTimer) {
            clearTimeout(this._inactivityTimer);
            this._inactivityTimer = null;
        }
    }
        
    _rescheduleInactivityCheck() {
        let _this = this;
        _this._clearInactivityCheck();
        _this._inactivityTimer = setTimeout(function() {
             _this._checkForInactivity();
             }, _this._console.connectionRefreshInterval + 50);
    }

    _checkForInactivity() {
        let _this = this;
        // Optimization, no need to check while sending
        if (_this._sendQueue.length != 0) {
            return;
        }
        let now = Date.now();
        let inactiveFor = now - _this._lastActivityAt;
        if (_this._console.connectionInactivityTimeout > 0 &&
            inactiveFor >= _this._console.connectionInactivityTimeout) {
            // Enter suspended state
            _this._clearInactivityCheck();
            _this.close();
            _this._console.connectionSuspended(function() {
                // Resume actions
                _this._lastActivityAt = Date.now();
                _this._rescheduleInactivityCheck();
                _this.connect();
            });
            return;
        }
        let idleFor = now - _this._lastSendAt;
        if (idleFor >= _this._console.connectionRefreshInterval) {
            // Sending implies update for lastSendAt and rescheduling
            _this._send({
                "jsonrpc": "2.0", "method": "keepAlive",
                "params": []
            });
        }
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
        this._clearInactivityCheck();
        this._sendQueue.push(JSON.stringify(data));
        this._drainSendQueue();
        this._lastSendAt = Date.now();
        this._rescheduleInactivityCheck();
    }

    /**
     * Convert the passed object to its JSON representation
     * and sends it to the server. The object should represent
     * a JSON RPC notification.
     * 
     * @param data the data
     */
    send(data: any) {
        this._lastActivityAt = Date.now();
        this._send(data);
    }

    /**
     * Invokes the given method on the server.
     *
     * @param the method
     * @param params the parameters
     */
    rpc(method: string, ...params: any[]) {
        if (params.length > 0) {
            this.send({
                "jsonrpc": "2.0", "method": method,
                "params": params
            });
        } else {
            this.send({ "jsonrpc": "2.0", "method": method
            });
        }
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
            this._beingHandled = this._recvQueue.shift();
            var handler = this._messageHandlers.get(this._beingHandled.method);
            if (!handler) {
                Log.error("No handler for invoked method " + this._beingHandled.method);
                this._beingHandled = null;
                continue;
            }
            try {
                if (this._beingHandled.hasOwnProperty("params")) {
                    this._handlerLog(() => "Handling: " + this._beingHandled.method
                        + "[" + this._beingHandled.params + "]");
                    handler(...this._beingHandled.params);
                } else {
                    this._handlerLog(() => "Handling: " + this._beingHandled.method);
                    handler();
                }
            } catch (e: any) {
                Log.error(e);
            }
            this._beingHandled = null;
            // Lots of actions may block execution of inactivity timer
            this._checkForInactivity();
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

    postponeMessage() {
        if (this._beingHandled) {
            this._recvQueue.unshift(this._beingHandled);
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

export default ConsoleWebSocket;
