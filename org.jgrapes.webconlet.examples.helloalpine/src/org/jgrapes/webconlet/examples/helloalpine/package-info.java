/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
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

/**
 * Pattern for AlpineJS-based conlets.
 *
 * <p>This conlet serves as a template for writing conlets that use AlpineJS
 * for reactive UI updates driven by a centralised data store. The pattern
 * separates server-side state management (Java model) from client-side
 * reactive rendering (AlpineJS directives) while keeping both sides in sync.</p>
 *
 * <h2>Architecture</h2>
 *
 * <p>The pattern consists of three layers that communicate through well-defined
 * channels:</p>
 *
 * <ol>
 *   <li><b>Server model</b> -- Java POJO holding the conlet's state, persisted
 *       via the key-value store.</li>
 *   <li><b>Data store</b> -- TypeScript class extending {@code Map} that holds
 *       {@code Alpine.reactive()} wrapped state objects, keyed by conlet ID.
 *       Lives as a global object on {@code window}.</li>
 *   <li><b>Preview/View templates</b> -- FreeMarker templates that use Alpine
 *       directives ({@code x-data}, {@code x-init}, {@code x-show},
 *       {@code @click}) to bind the UI to the data store.</li>
 * </ol>
 *
 * <h2>Browser-Side Structure</h2>
 *
 * <p>The TypeScript file ({@code *-functions.ts}) defines four elements:</p>
 *
 * <h3>1. State Interface</h3>
 * <p>An interface describing the shape of the client-side state per conlet
 * instance. The corresponding server model properties should mirror this
 * structure.</p>
 * <pre>
 * interface ConletState {
 *     worldVisible: boolean;
 * }
 * </pre>
 *
 * <h3>2. Data Store Class</h3>
 * <p>A class extending {@code Map&lt;string, ConletState&gt;} that:</p>
 * <ul>
 *   <li>Holds reactive state objects ({@code Alpine.reactive(...)}) per conlet
 *       instance, keyed by conlet ID.</li>
 *   <li>Declares {@code onLoad} and {@code onUnload} as function-typed
 *       properties (using definite assignment assertion {@code !:}) for use
 *       by the framework's {@code data-jgwc-on-load} and
 *       {@code data-jgwc-on-unload} hook attributes. These must be plain
 *       functions -- not class methods -- because the framework invokes them
 *       without {@code this} binding.</li>
 *   <li>Provides implementation methods ({@code addConletState},
 *       {@code removeConletState}) that the hook functions delegate to.</li>
 * </ul>
 *
 * <h3>3. Global Registration</h3>
 * <p>An instance of the data store is assigned to a {@code window} property
 * (declared via {@code declare global}). The {@code onLoad} and {@code onUnload}
 * properties are assigned as standalone functions that close over the store
 * instance and delegate to its methods:</p>
 * <pre>
 * const conletStates = new ConletDataStore();
 * window.orgJGrapesConletsExampleMyConlet = conletStates;
 *
 * conletStates.onLoad = function(conletEl, _isUpdate) {
 *     conletStates.addConletState(conletEl);
 * };
 *
 * conletStates.onUnload = function(conletEl, isUpdate) {
 *     conletStates.removeConletState(conletEl, isUpdate);
 * };
 * </pre>
 *
 * <h3>4. Server Notification Handler</h3>
 * <p>A function registered via {@code JGConsole.registerConletFunction()} that
 * receives state updates from the server (pushed via
 * {@code NotifyConletView}). It modifies the reactive state in the data store,
 * triggering Alpine's automatic DOM updates:</p>
 * <pre>
 * JGConsole.registerConletFunction(
 *     "org.jgrapes.webconlet.examples.myconlet.MyConlet",
 *     "updateState",
 *     function(conletId, newState) {
 *         const state = conletStates.get(conletId);
 *         if (state) {
 *             state.someProperty = newState.someProperty;
 *         }
 *     });
 * </pre>
 *
 * <h2>View Template</h2>
 *
 * <p>The view template ({@code *-view.ftl.html}) is the reactive UI. It uses
 * three Alpine-related attributes on the root element:</p>
 *
 * <dl>
 *   <dt>{@code data-jgwc-on-load="globalObj.onLoad"}</dt>
 *   <dd>Initialises the conlet's state in the data store when the view is
 *       first rendered. The framework calls this before Alpine processes
 *       {@code x-init}.</dd>
 *
 *   <dt>{@code x-data="{ conletId: null, state: null }"}</dt>
 *   <dd>Declares local Alpine variables. The {@code state} reference will
 *       point to the reactive object from the global data store.</dd>
 *
 *   <dt>{@code x-init="conletId = ...; state = globalObj.get(conletId);"}</dt>
 *   <dd>Runs after {@code onLoad}, obtains the conlet ID from the nearest
 *       {@code [data-conlet-id]} ancestor, and stores a reference to the
 *       global reactive state. Alpine directives in child elements then
 *       react to changes in {@code state}.</dd>
 * </dl>
 *
 * <p>UI elements use Alpine directives against {@code state}:</p>
 * <ul>
 *   <li>{@code x-show="state.visible"} -- reactive visibility toggle</li>
 *   <li>{@code x-bind:class="..."} -- reactive class binding</li>
 *   <li>{@code @click="state.value = !state.value;
 *       JGConsole.notifyConletModel(conletId, 'toggleValue')"} --
 *       local reactive change + server notification</li>
 * </ul>
 *
 * <h2>Preview Template</h2>
 *
 * <p>The preview ({@code *-preview.ftl.html}) is static content shown in the
 * conlet picker dialog. It carries
 * {@code data-jgwc-on-unload="globalObj.onUnload"} so that when the conlet is
 * removed from the page (not just layout-refreshed), the data store cleans up
 * its state entry and optionally shows a notification. The {@code isUpdate}
 * parameter distinguishes a layout refresh ({@code true}) from actual removal
 * ({@code false}).</p>
 *
 * <h2>Server-Side Communication Flow</h2>
 *
 * <p>State changes flow in two directions:</p>
 *
 * <h3>Client to Server (user action)</h3>
 * <ol>
 *   <li>User clicks a button with an {@code @click} handler.</li>
 *   <li>The handler mutates the local reactive state (immediate UI update)
 *       and calls {@code JGConsole.notifyConletModel(conletId, 'methodName')}.
 *   </li>
 *   <li>The server's {@code doUpdateConletState} method handles the event,
 *       updates the Java model, persists it, and responds with
 *       {@code NotifyConletView} to push the new state back.</li>
 *   <li>The registered conlet function receives the update and mutates the
 *       reactive state (Alpine re-renders affected bindings).</li>
 * </ol>
 *
 * <h3>Server to Client (initial render + push)</h3>
 * <ol>
 *   <li>On initial view render, the server's {@code doRenderConlet} sends
 *       a {@code NotifyConletView} event with the current model state after
 *       sending the rendered HTML.</li>
 *   <li>The registered conlet function populates the data store's reactive
 *       state with the server's values.</li>
 *   <li>Alpine's bindings reflect the server-provided initial state.</li>
 * </ol>
 *
 * <h2>Build Configuration</h2>
 *
 * <p>The jdbld project config ({@code _jdbld/src/jdbld/conlet/HelloAlpine.java})
 * declares a dependency on {@code AlpineJs} (provider) and sets up a TypeScript
 * build via {@code NpmExecutor}. The TypeScript file is compiled to ESM and
 * output to {@code build/generated/resources/}, packaged into the JAR as a
 * conlet resource.</p>
 */
package org.jgrapes.webconlet.examples.helloalpine;
