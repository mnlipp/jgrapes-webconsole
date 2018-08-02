/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

'use strict';

/**
 * JGPortal establishes a namespace for the JavaScript functions
 * that are provided by the portal.
 */
var JGPortal = {};

(function () {

    var log = {
        debug: function(message) {
            if (console && console.debug) {
                console.debug(message)
            }
        },
        info: function(message) {
            if (console && console.info) {
                console.info(message)
            }
        },
        warn: function(message) {
            if (console && console.warn) {
                console.warn(message)
            }
        },
        error: function(message) {
            if (console && console.error) {
                console.error(message)
            }
        }
    }
    JGPortal.log = log;

    // ///////////////////
    // WebSocket "wrapper"
    // ///////////////////
    
    /**
     * Defines a wrapper class for a web socket. An instance 
     * creates and maintains a connection to a session on the
     * server, i.e. it reconnects automatically to the session
     * when the connection is lost. The connection is used to
     * exchange JSON RPC notifications.
     */
    class PortalWebSocket {
        
        constructor(portal) {
            // "Privacy by convention" is sufficient for this.
            this._portal = portal;
            this._ws = null;
            this._sendQueue = [];
            this._recvQueue = [];
            this._recvQueueLocks = 0;
            this._messageHandlers = {};
            this._refreshTimer = null;
            this._inactivity = 0;
            this._reconnectTimer = null;
            this._connectRequested = false;
            this._initialConnect = true;
            this._portalSessionId = null;
            this._connectionLost = false;
            this._oldPortalSessionId = sessionStorage.getItem("org.jgrapes.portal.base.sessionId");
        };

        /**
         * Returns the unique session id used to identify the connection.
         * 
         * @return {string} the id
         */
        portalSessionId() {
            return this._portalSessionId;
        }
        
        _connect() {
            let location = (window.location.protocol === "https:" ? "wss" : "ws") +
                "://" + window.location.host + window.location.pathname;
            if (!location.endsWith("/")) {
                location += "/";
            }
            this._portalSessionId = sessionStorage.getItem("org.jgrapes.portal.base.sessionId");
            location += "portal-session/" + this._portalSessionId;
            if (this._oldPortalSessionId) {
                location += "?was=" + this._oldPortalSessionId;
                this._oldPortalSessionId = null;
            }
            log.debug("Creating WebSocket for " + location);
            this._ws = new WebSocket(location);
            log.debug("Created WebSocket with readyState " + this._ws.readyState);
            let self = this;
            this._ws.onopen = function() {
                log.debug("OnOpen called for WebSocket.");
                if (self._connectionLost) {
                    self._connectionLost = false;
                    self._portal.connectionRestored();
                }
                self._drainSendQueue();
                if (self._initialConnect) {
                    self._initialConnect = false;
                } else {
                    // Make sure to get any lost updates
                    let renderer = self._portal._renderer; 
                    renderer.findPreviewIds().forEach(function(id) {
                        renderer.sendRenderPortlet(id, "Preview", false);
                    });
                    renderer.findViewIds().forEach(function(id) {
                        renderer.sendRenderPortlet(id, "View", false);
                    });
                }
                self._refreshTimer = setInterval(function() {
                    if (self._sendQueue.length == 0) {
                        self._inactivity += self._portal.sessionRefreshInterval;
                        if (self._portal.sessionInactivityTimeout > 0 &&
                                self._inactivity >= self._portal.sessionInactivityTimeout) {
                            self.close();
                            self._portal.connectionSuspended(function() {
                                self.connect();
                            });
                            return;
                        }
                        self._send({"jsonrpc": "2.0", "method": "keepAlive",
                            "params": []});
                    }
                }, self._portal.sessionRefreshInterval);
            }
            this._ws.onclose = function(event) {
                log.debug("OnClose called for WebSocket (reconnect: " +
                        self._connectRequested + ").");
                if (self._refreshTimer !== null) {
                    clearInterval(self._refreshTimer);
                    self._refreshTimer = null;
                }
                if (self._connectRequested) {
                    // Not an intended disconnect
                    if (!self._connectionLost) {
                        self._portal.connectionLost();
                        self._connectionLost = true;
                    }
                    self._initiateReconnect();
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
                } catch (e) {
                    log.error(e.name + ":" + e.lineNumber + ":" + e.columnNumber 
                            + ": " + e.message + ". Data: ");
                    log.error(event.data);
                    return;
                }
                self._recvQueue.push(msg);
                if (self._recvQueue.length === 1) {
                    self._handleMessages();
                }
            }
        }

        /**
         * Establishes the connection.
         */
        connect() {
            this._connectRequested = true;
            let self = this;
            $(window).on('beforeunload', function(){
                log.debug("Closing WebSocket due to page unload");
                // Internal connect, don't send disconnect
                self._connectRequested = false;
                self._ws.close();
            });
            this._connect();
        } 

        /**
         * Closes the connection.
         */
        close() {
            if (this._portalSessionId) {
                this._send({"jsonrpc": "2.0", "method": "disconnect",
                    "params": [ this._portalSessionId ]});
            }
            this._connectRequested = false;
            this._ws.close();
        }

        _initiateReconnect() {
            if (!this._reconnectTimer) {
                let self = this;
                this._reconnectTimer = setTimeout(function() {
                    self._reconnectTimer = null;
                    self._connect();
                }, 1000);
            }
        }
    
        _drainSendQueue() {
            while (this._ws.readyState == this._ws.OPEN && this._sendQueue.length > 0) {
                let msg = this._sendQueue[0];
                try {
                    this._ws.send(msg);
                    this._sendQueue.shift();
                } catch (e) {
                    log.warn(e);
                }
            }
        }
    
        _send(data) {
            this._sendQueue.push(JSON.stringify(data));
            this._drainSendQueue();
        }

        /**
         * Convert the passed object to its JSON representation
         * and sends it to the server. The object should represent
         * a JSON RPC notification.
         * 
         * @param  {object} data the data
         */
        send(data) {
            this._inactivity = 0;
            this._send(data);
        }

        /**
         * When a JSON RPC notification is received, its method property
         * is matched against all added handlers. If a match is found,
         * the associated handler function is invoked with the
         * params values from the notification as parameters.
         * 
         * @param {string} method the method property to match
         * @param {function} handler the handler function
         */
        addMessageHandler(method, handler) {
            this._messageHandlers[method] = handler;
        }
    
        _handleMessages() {
            while (true) {
                if (this._recvQueue.length === 0 || this._recvQueueLocks > 0) {
                    break;
                }
                var message = this._recvQueue.shift(); 
                var handler = this._messageHandlers[message.method];
                if (!handler) {
                    log.error("No handler for invoked method " + message.method);
                    continue;
                }
                if (message.hasOwnProperty("params")) {
                    handler(...message.params);
                } else {
                    handler();
                }
            }
        }
    
        lockMessageReceiver() {
            this._recvQueueLocks += 1;
        }

        unlockMessageReceiver() {
            this._recvQueueLocks -= 1;
            if (this._recvQueueLocks == 0) {
                this._handleMessages();
            }
        }
    }

    class ResourceManager {

        constructor(portal) {
            this._portal = portal;
            this._debugLoading = false;
            this._providedScriptResources = new Set(); // Names, i.e. strings
            this._unresolvedScriptRequests = []; // ScriptResource objects
            this._loadingScripts = new Set(); // uris (src attribute)
            this._unlockMessageQueueAfterLoad = false;
            this._scriptResourceSnippet = 0;
        }
        
        _loadingMsg(msg) {
            if (this._debugLoading) {
                log.debug(moment().format("HH:mm:ss.SSS") + ": " + msg());
            }
        }
        
        _mayBeStartScriptLoad (scriptResource) {
            let self = this;
            if (self._debugLoading) {
                if (scriptResource.provides.length > 0) {
                    scriptResource.id = scriptResource.provides.join("/");
                } else if (scriptResource.uri) {
                    scriptResource.id = scriptResource.uri;
                } else {
                    scriptResource.id = "Snippet_" + ++this._scriptResourceSnippet;
                }
            }
            let stillRequired = scriptResource.requires;
            scriptResource.requires = [];
            stillRequired.forEach(function(required) {
                if (!self._providedScriptResources.has(required)) {
                    scriptResource.requires.push(required);
                }
            });
            if (scriptResource.requires.length > 0) {
                self._loadingMsg(function() { return "Not (yet) loading: " + scriptResource.id
                            + ", missing: " + scriptResource.requires.join(", ") });
                self._unresolvedScriptRequests.push(scriptResource);
                return;
            }
            this._startScriptLoad(scriptResource);
        }
        
        _startScriptLoad(scriptResource) {
            let self = this;
            let head = $("head").get()[0];
            let script = document.createElement("script");
            if (scriptResource.source) {
                script.text = scriptResource.source;
                head.appendChild(script);
                self._scriptResourceLoaded(scriptResource);
                return;
            }
            script.src = scriptResource.uri;
            script.addEventListener('load', function(event) {
                // Remove this from loading
                self._loadingScripts.delete(script.src);
                // All done?
                self._scriptResourceLoaded(scriptResource);
                if (self._loadingScripts.size == 0 && self._unlockMessageQueueAfterLoad) {
                    self._loadingMsg(function() { return "All loaded, unlocking message queue." });
                    self._portal.unlockMessageQueue();
                }
            });
            // Put on script load queue to indicate load in progress
            self._loadingMsg(function() { return "Loading: " + scriptResource.id });
            self._loadingScripts.add(script.src);
            head.appendChild(script);
        }

        _scriptResourceLoaded(scriptResource) {
            let self = this;
            // Whatever it provides is now provided
            this._loadingMsg(function() { return "Loaded: " + scriptResource.id });
            scriptResource.provides.forEach(function(res) {
                self._providedScriptResources.add(res);
            });
            // Re-evaluate
            let nowProvided = new Set(scriptResource.provides);
            let stillUnresolved = self._unresolvedScriptRequests;
            self._unresolvedScriptRequests = [];
            stillUnresolved.forEach(function(reqRes) {
                // Still required by this unresolved resource
                let stillRequired = reqRes.requires;
                reqRes.requires = []; // Accumulates new value
                stillRequired.forEach(function(required) {
                    if (!nowProvided.has(required)) {
                        // Not in newly provided, still required
                        reqRes.requires.push(required);
                    }
                });
                if (reqRes.requires.length == 0) {
                    self._startScriptLoad(reqRes);
                } else {
                    // Still unresolved
                    self._unresolvedScriptRequests.push(reqRes);
                }
            });
        }
        
        addPageResources(cssUris, cssSource, scriptResources) {
            for (let index in cssUris) {
                let uri = cssUris[index];
                if ($("head > link[href='" + uri + "']").length === 0) {
                    $("head link[rel='stylesheet']:last").after("<link rel='stylesheet' href='" + uri + "'>");
                }
            }
            if (cssSource) {
                let style = $("style");
                style.text(cssSource);
                $("head link[rel='stylesheet']:last").after(style);
            }
            // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
            for (let index in scriptResources) {
                let scriptResource = scriptResources[index];
                if (scriptResource.uri) {
                    if ($("head > script[src='" + scriptResource.uri + "']").length > 0) {
                        continue;
                    }
                }
                this._mayBeStartScriptLoad(scriptResource);
            }
        }
        
        lockWhileLoading() {
            if (this._loadingScripts.size > 0) {
                this._portal.lockMessageQueue();
                this._loadingMsg(function() { return "Locking message queue until all loaded." });
                this._unlockMessageQueueAfterLoad = true;
            }
        }
    }
    
    JGPortal.Renderer = class {
        
        init() {
        }
        
        connectionLost() {
            log.warn("Not implemented!");
        }
        
        connectionRestored() {
            log.warn("Not implemented!");
        }

        connectionSuspended(resume) {
            log.warn("Not implemented!");
        }
        
        portalConfigured() {
            log.warn("Not implemented!");
        }
        
        addPortletType(portletType, displayName, isInstantiable) {
            log.warn("Not implemented!");
        }
        
        lastPortalLayout(previewLayout, tabsLayout, xtraInfo) {
            log.warn("Not implemented!");
        }
        
        updatePortletPreview(isNew, container, modes, content, foreground) {
            log.warn("Not implemented!");
        }
        
        updatePortletView(isNew, container, modes, content, foreground) {
            log.warn("Not implemented!");
        }

        portletRemoved(containers) {
            log.warn("Not implemented!");
        }
        
        /**
         * Update the title of the portlet with the given id.
         *
         * @param {string} portletId the portlet id
         * @param {string} title the new title
         */
        updatePortletViewTitle(portletId, title) {
            log.warn("Not implemented!");
        }

        /**
         * Update the modes of the portlet with the given id.
         * 
         * @param {string} portletId the portlet id
         * @param {string[]} modes the modes
         */
        updatePortletModes(portletId, modes) {
            log.warn("Not implemented!");
        }
        
        showEditDialog(container, modes, content) {
            log.warn("Not implemented!");
        }
        
        notification(content, options) {
            log.warn("Not implemented!");
        }
        
        // Send methods

        /**
         * Sends a notification for changing the language to the server.
         * 
         * @param {string} locale the id of the selected locale
         */
        sendSetLocale(locale) {
            thePortal.send("setLocale", locale);
        };

        /**
         * Sends a notification that requests the rendering of a portlet.
         * 
         * @param {string} portletId the portlet id
         * @param {string} mode the requested render mode
         * @param {boolean} foreground if the portlet is to be put in
         * the foreground after rendering
         */
        sendRenderPortlet(portletId, mode, foreground) {
            thePortal.send("renderPortlet", portletId, mode, foreground);
        };

        /**
         * Sends a notification that requests the addition of a portlet.
         * 
         * @param {string} portletType the type of the portlet to add
         */
        sendAddPortlet(portletType) {
            thePortal.send("addPortlet", portletType, "Preview");
        };

        /**
         * Send a notification that request the removal of a portlet.
         * 
         * @param {string} portletId the id of the portlet to be deleted
         */
        sendDeletePortlet(portletId) {
            thePortal.send("deletePortlet", portletId);
        };

        sendLayout(previewLayout, tabLayout, xtraInfo) {
            if (!thePortal.isConfigured) {
                return;
            }
            thePortal.send("portalLayout", previewLayout, tabLayout, xtraInfo);
        };

        send(method, ...params) {
            thePortal.send(method, ...params);
        };
        
        // Utility methods.

        /**
         * Find the <code>div</code>s that display the preview or view of the
         * portlet with the given id.
         * 
         * @param {string} portletId the portlet id
         */
        findPortletRepresentations(portletId) {
            return $( ".portlet[data-portlet-id='" + portletId + "']" ).get();
        };

        /**
         * Find the <code>div</code> that displays the preview of the
         * portlet with the given id.
         * 
         * @param {string} portletId the portlet id
         */
        findPortletPreview(portletId) {
            let matches = $( ".portlet-preview[data-portlet-id='" + portletId + "']" );
            if (matches.length === 1) {
                return $( matches[0] );
            }
            return undefined;
        };

        findPreviewIds() {
            let ids = $( ".portlet-preview[data-portlet-id]" ).map(function() {
                return $( this ).attr("data-portlet-id");
            }).get();
            return ids;
        }
        
        /**
         * Find the <code>div</code> that displays the view of the
         * portlet with the given id.
         * 
         * @param {string} portletId the portlet id
         */
        findPortletView(portletId) {
            let matches = $( ".portlet-view[data-portlet-id='" + portletId + "']" );
            if (matches.length === 1) {
                return $( matches[0] );
            }
            return undefined;
        };

        findViewIds() {
            let ids = $( ".portlet-view[data-portlet-id]" ).map(function() {
                return $( this ).attr("data-portlet-id");
            }).get();
            return ids;
        }
        
        /**
         * Utility method to format a memory size to a maximum
         * of 4 digits for the integer part by appending the
         * appropriate unit.
         * 
         * @param {integer} size the size value to format
         * @param {integer} digits the number of digits of the factional part
         * @param {string} lang the language (BCP 47 code, 
         * used to determine the delimiter)
         */
        formatMemorySize(size, digits, lang) {
            if (lang === undefined) {
                lang = digits;
                digits = -1;
            }
            let scale = 0;
            while (size > 10000 && scale < 5) {
                    size = size / 1024;
                    scale += 1;
            }
            let unit = "PiB";
            switch (scale) {
            case 0:
                unit = "B";
                break;
            case 1:
                unit = "kiB";
                break;
            case 2:
                unit = "MiB";
                break;
            case 3:
                unit = "GiB";
                break;
            case 4:
                unit = "TiB";
                break;
            default:
                break;
            }
            if (digits >= 0) {
                return new Intl.NumberFormat(lang, {
                    minimumFractionDigits: digits,
                    maximumFractionDigits: digits
                }).format(size) + " " + unit;
            }
            return new Intl.NumberFormat(lang).format(size) + " " + unit;
        }
        
    }
    
    class Portal {
        
        constructor() {
            let self = this;
            this._isConfigured = false;
            this._sessionRefreshInterval = 0;
            this._sessionInactivityTimeout = 0;
            this._renderer = null;
            this._webSocket = new PortalWebSocket(this);
            this._portletFunctionRegistry = {};
            this._previewTemplate = $('<section class="portlet portlet-preview"></section>');
            this._viewTemplate = $( '<article class="portlet portlet-view"></article>');
            this._editTemplate = $( '<div class="portlet portlet-edit"></div>');
            this._resourceManager = new ResourceManager(this);
            this._webSocket.addMessageHandler('addPageResources', 
                    function(cssUris, cssSource, scriptResources) {
                self._resourceManager.addPageResources(cssUris, cssSource, scriptResources);
            });
            this._webSocket.addMessageHandler('addPortletType',
                    function (portletType, displayName, cssUris, scriptResources,
                            isInstantiable) {
                self._resourceManager.addPageResources(cssUris, null, scriptResources);
                self._renderer.addPortletType(portletType, displayName, isInstantiable);
            });
            this._webSocket.addMessageHandler('lastPortalLayout',
                    function(previewLayout, tabsLayout, xtraInfo) {
                // Should we wait with further actions?
                self._resourceManager.lockWhileLoading();
                self._renderer.lastPortalLayout(previewLayout, tabsLayout, xtraInfo);
            });
            this._webSocket.addMessageHandler('notifyPortletView',
                    function notifyPortletView(portletClass, portletId, method, params) {
                let classRegistry = self._portletFunctionRegistry[portletClass];
                if (classRegistry) {
                    let f = classRegistry[method];
                    if (f) {
                        f(portletId, params);
                    }
                }
            });
            this._webSocket.addMessageHandler('portalConfigured',
                    function portalConfigured() {
                self._isConfigured = true;
                self._renderer.portalConfigured();
            });
            this._webSocket.addMessageHandler('updatePortlet',
                    function(portletId, mode, modes, content, foreground) {
                if (mode === "Preview" || mode === "DeleteablePreview") {
                    self._updatePreview(portletId, modes, mode, content, foreground);
                } else if (mode === "View") {
                    self._updateView(portletId, modes, content, foreground);
                } else if (mode === "Edit") {
                    let container = self._editTemplate.clone();
                    container.attr("data-portlet-id", portletId);
                    self._renderer.showEditDialog(container, modes, content);
                    self._execOnLoad(container);
                }
            });
            this._webSocket.addMessageHandler('deletePortlet',
                    function deletePortlet(portletId) {
                let portletDisplays = self._renderer.findPortletRepresentations(portletId);
                if (portletDisplays.length > 0) {
                    self._renderer.portletRemoved(portletDisplays);
                }
            });
            this._webSocket.addMessageHandler('displayNotification',
                    function (content, options) {
                self._renderer.notification(content, options);
            });
            this._webSocket.addMessageHandler('retrieveLocalData',
                    function retrieveLocalData(path) {
                let result = [];
                try {
                    for(let i = 0; i < localStorage.length; i++) {
                        let key = localStorage.key(i);
                        if (!path.endsWith("/")) {
                            if (key !== path) {
                                continue;
                            }
                        } else {
                            if (!key.startsWith(path)) {
                                continue;
                            }
                        }
                        let value = localStorage.getItem(key);
                        result.push([ key, value ])
                    }
                } catch (e) {
                    log.error(e);
                }
                self._webSocket.send({"jsonrpc": "2.0", "method": "retrievedLocalData",
                    "params": [ result ]});
            });
            this._webSocket.addMessageHandler('storeLocalData',
                    function storeLocalData(actions) {
                try {
                    for (let i in actions) {
                        let action = actions[i];
                        if (action[0] === "u") {
                            localStorage.setItem(action[1], action[2]);
                        } else if (action[0] === "d") {
                            localStorage.removeItem(action[1]);
                        }
                    }
                } catch (e) {
                    log.error(e);
                }
            });
            this._webSocket.addMessageHandler('reload',
                    function() { 
                window.location.reload(true);
            });
        }

        init(portalSessionId, refreshInterval, inactivityTimeout, renderer) {
            sessionStorage.setItem("org.jgrapes.portal.base.sessionId", portalSessionId);
            this._sessionRefreshInterval = refreshInterval;
            this._sessionInactivityTimeout = inactivityTimeout;
            this._renderer = renderer;
            JGPortal.renderer = renderer;
            
            // Everything set up, can connect web socket now.
            this._webSocket.connect();

            // More initialization.
            this._renderer.init();
            
            // With everything prepared, send portal ready
            this.send("portalReady");
        }

        get isConfigured() {
            return this._isConfigured;
        }
        
        get sessionRefreshInterval() {
            return this._sessionRefreshInterval;
        }
        
        get sessionInactivityTimeout() {
            return this._sessionInactivityTimeout;
        }
        
        get renderer() {
            return this._renderer;
        }
        
        connectionLost() {
            this._renderer.connectionLost();
        }
        
        connectionRestored() {
            this._renderer.connectionRestored();
        }

        connectionSuspended(resume) {
            this._renderer.connectionSuspended(resume);
        }
        
        /**
         * Increses the lock count on the receiver. As long as
         * the lock count is greater than 0, the invocation of
         * handlers is suspended.
         */
        lockMessageQueue() {
            this._webSocket.lockMessageReceiver();
        }
        
        /**
         * Decreases the lock count on the receiver. When the
         * count reaches 0, the invocation of handlers is resumed.
         */
        unlockMessageQueue() {
            this._webSocket.unlockMessageReceiver();
        }

        // Portlet management
        
        _execOnLoad(container) {
            container.find("[data-on-load]").each(function() {
                let onLoad = $(this).data("onLoad");
                let segs = onLoad.split(".");
                let obj = window;
                while (obj && segs.length > 0) {
                    obj = obj[segs.shift()];
                }
                if (obj && typeof obj === "function") {
                    obj.apply(null, [ $(this) ]);
                }
            });
        }

        _updatePreview(portletId, modes, mode, content, foreground) {
            let container = this._renderer.findPortletPreview(portletId);
            let isNew = !container;
            if (isNew) {
                container = this._previewTemplate.clone();
                container.attr("data-portlet-id", portletId);
            }
            if (mode === "DeleteablePreview") {
                container.addClass('portlet-deleteable')
            } else {
                container.removeClass('portlet-deleteable')
            }
            this._renderer.updatePortletPreview(isNew, container, modes, 
                    content, foreground);
            this._execOnLoad(container);
        };
        
        _updateView(portletId, modes, content, foreground) {
            let container = this._renderer.findPortletView(portletId);
            let isNew = !container;
            if (isNew) {
                container = this._viewTemplate.clone();
                container.attr("data-portlet-id", portletId);
            }
            this._renderer.updatePortletView(isNew, container, modes, 
                    content, foreground);
            this._execOnLoad(container);
        };
        
        /**
         * Registers a portlet method that to be invoked if a
         * JSON RPC notification with method <code>notifyPortletView</code>
         * is received.
         * 
         * @param {string} portletClass the portlet type for which
         * the method is registered
         * @param {string} methodName the method that is registered
         * @param {function} method the function to invoke
         */
        registerPortletMethod(portletClass, methodName, method) {
            let classRegistry = this._portletFunctionRegistry[portletClass];
            if (!classRegistry) {
                classRegistry = {};
                this._portletFunctionRegistry[portletClass] = classRegistry;
            }
            classRegistry[methodName] = method;
        }

        // Send methods
        
        send(method, ...params) {
            if (params.length > 0) {
                this._webSocket.send({"jsonrpc": "2.0", "method": method,
                    "params": params});   
            } else {
                this._webSocket.send({"jsonrpc": "2.0", "method": method});
            }
        }
        
        /**
         * Send a notification with method <code>notifyPortletModel</code>
         * and the given portlet id, method and parameters as the 
         * notification's parameters to the server.
         * 
         * @param {string} portletId the id of the portlet to send to
         * @param {string} method the method to invoke
         * @param params the parameters to send
         */
        notifyPortletModel(portletId, method, ...params) {
            if (params === undefined) {
                thePortal.send("notifyPortletModel", portletId, method);
            } else {
                thePortal.send("notifyPortletModel", portletId, method, params);
            }
        };
        
        // Utility methods
        
        // https://gist.github.com/jed/982883
        generateUUID() {
            var r = crypto.getRandomValues(new Uint8Array(16));
            r[6] = r[6] & 0x0f | 0x40;
            r[8] = r[8] & 0x3f | 0x80;
            return (r[0].toString(16) + r[1].toString(16) +
                r[2].toString(16) + r[3].toString(16) +
                "-" + r[4].toString(16) + r[5].toString(16) +
                "-" + r[6].toString(16) + r[7].toString(16) +
                "-" + r[8].toString(16) + r[9].toString(16) +
                "-" + r[0].toString(10) + r[0].toString(11) +
                r[0].toString(12) + r[0].toString(13) +
                r[0].toString(14) + r[0].toString(15));
        };
        
    }
    
    var thePortal = new Portal();
    
    JGPortal.init = function(...params) {
        thePortal.init(...params);
    }
    
    JGPortal.registerPortletMethod = function(...params) {
        thePortal.registerPortletMethod(...params);
    }
    
    JGPortal.notifyPortletModel = function(...params) {
        return thePortal.notifyPortletModel(...params);
    }
    
})();
