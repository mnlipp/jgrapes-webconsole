import Console from "./Console";
import Log from "./Log"
import { parseHtml } from "./Util";

export interface ScriptResource {
    id: string;
    type: string;
    uri: string;
    source: string;
    requires: string[];
    provides: string[];
}


export default class ResourceManager {

    private _console: Console;
    private _debugLoading = false;
    private _providedScriptResources = new Set<string>();
    private _unresolvedScriptRequests: ScriptResource[] = [];
    private _loadingScripts = new Set<string>(); // uris (src attribute)
    private _unlockMessageQueueAfterLoad = false;
    private _loadingTimeoutHandler: ReturnType<typeof setInterval> | null = null;
    private _scriptResourceSnippet = 0;

    constructor(console: Console) {
        this._console = console;
        document.querySelectorAll("script[data-jgwc-provides]").forEach(s => {
            s.getAttribute("data-jgwc-provides")!.split(",").forEach(
                n => this._providedScriptResources.add(n.trim()));
        });
        if (this._debugLoading) {
            Log.debug("Initially provided: "
             + [...this._providedScriptResources].join(", "));
        }
    }

    private _loadingMsg(msg: () => string) {
        if (this._debugLoading) {
            Log.debug(msg());
        }
    }

    private _mayBeStartScriptLoad(scriptResource: ScriptResource) {
        let _this = this;
        if (_this._debugLoading) {
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
            if (!_this._providedScriptResources.has(required)) {
                scriptResource.requires.push(required);
            }
        });
        if (scriptResource.requires.length > 0) {
            _this._loadingMsg(function() {
                return "Not (yet) loading: " + scriptResource.id
                    + ", missing: " + scriptResource.requires.join(", ")
            });
            _this._unresolvedScriptRequests.push(scriptResource);
            return;
        }
        this._startScriptLoad(scriptResource);
    }

    _startScriptLoad(scriptResource: ScriptResource) {
        let _this = this;
        let head = document.querySelector("head")!;
        let script = document.createElement("script");
        if (scriptResource.id) {
            script.setAttribute("id", scriptResource.id);
        }
        if (scriptResource.type) {
            script.setAttribute("type", scriptResource.type);
        }
        if (scriptResource.source) {
            // Script source is part of request, add and proceed as if loaded.
            script.text = scriptResource.source;
            head.appendChild(script);
            _this._scriptResourceLoaded(scriptResource);
            return;
        }
        if (!scriptResource.uri) {
            return;
        }
        // Asynchronous loading.
        script.src = scriptResource.uri;
        script.async = true;
        script.addEventListener('load', function(event: Event) {
            // Remove this from loading
            _this._loadingScripts.delete(script.src);
            _this._scriptResourceLoaded(scriptResource);
        });
        // Put on script load queue to indicate load in progress
        _this._loadingMsg(function() { return "Loading: " + scriptResource.id });
        _this._loadingScripts.add(script.src);
        if (this._loadingTimeoutHandler === null) {
            this._loadingTimeoutHandler = setInterval(
                function() {
                    Log.warn("Still waiting for: "
                        + Array.from(_this._loadingScripts).join(", "));
                }, 5000)
        }
        head.appendChild(script);
    }

    _scriptResourceLoaded(scriptResource: ScriptResource) {
        let _this = this;
        // Whatever it provides is now provided
        this._loadingMsg(function() { return "Loaded: " + scriptResource.id });
        scriptResource.provides.forEach(function(res) {
            _this._providedScriptResources.add(res);
        });
        // Re-evaluate
        let nowProvided = new Set(scriptResource.provides);
        let stillUnresolved = _this._unresolvedScriptRequests;
        _this._unresolvedScriptRequests = [];
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
                _this._startScriptLoad(reqRes);
            } else {
                // Back to still unresolved
                _this._unresolvedScriptRequests.push(reqRes);
            }
        });
        // All done?
        if (_this._loadingScripts.size == 0) {
            if (this._loadingTimeoutHandler !== null) {
                clearInterval(this._loadingTimeoutHandler);
                this._loadingTimeoutHandler = null;
            }
            if (_this._unlockMessageQueueAfterLoad) {
                _this._loadingMsg(function() { return "All loaded, unlocking message queue." });
                _this._console.unlockMessageQueue();
            }
        }
    }

    addPageResources(cssUris: string[], cssSource: string | null,
            scriptResources: ScriptResource[]) {
        for (let index in cssUris) {
            let uri = cssUris[index];
            let links = document.querySelectorAll("head > link[href='" + uri + "']");
            if (links.length === 0) {
                let sheets = document.querySelectorAll(
                    "head link[rel='stylesheet']")!;
                sheets[0].parentNode!.insertBefore(parseHtml(
                    "<link rel='stylesheet' href='" + uri + "'>")[0],
                    sheets[sheets.length - 1].nextSibling);
            }
        }
        if (cssSource) {
            let style = document.createElement("style");
            style.textContent = cssSource;
            let last = document.querySelector("head link[rel='stylesheet']:last")!;
            last.parentNode!.insertBefore(style, last.nextSibling);
        }
        // Don't use jquery, https://stackoverflow.com/questions/610995/cant-append-script-element
        for (let index in scriptResources) {
            let scriptResource = scriptResources[index];
            if (scriptResource.uri) {
                if (document.querySelector("head > script[src='" 
                        + scriptResource.uri + "']")) {
                    continue;
                }
            }
            this._mayBeStartScriptLoad(scriptResource);
        }
    }

    lockWhileLoading() {
        if (this._loadingScripts.size > 0 && !this._unlockMessageQueueAfterLoad) {
            this._console.lockMessageQueue();
            this._loadingMsg(function() { return "Locking message queue until all loaded." });
            this._unlockMessageQueueAfterLoad = true;
            return true;
        }
        return false;
    }
}

