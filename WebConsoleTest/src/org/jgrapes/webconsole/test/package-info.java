/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2024  Michael N. Lipp
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
 * The following diagram shows the components of the test application.
 *  
 * In framework terms, {@link org.jgrapes.webconsole.test.WebConsoleTest}
 * is the root component of the application.
 * 
 * The position of the components in the component tree is important
 * when writing the configuration file for the WebConsoleTest and therefore
 * shown below. In order to  keep the diagram readable, the 
 * components attached to the 
 * {@link org.jgrapes.webconsole.base.WebConsole} are shown in a 
 * separate diagram further down.
 * 
 * ![Test components](test-components.svg)
 * 
 * Component hierarchy of the web console:
 * 
 * ![Web console components](console-components.svg)
 *
 * The components marked as "&lt;&lt;internal&gt;&gt;" have no 
 * configuration options or use their default values when used 
 * in this application.
 * 
 * As an example, the following YAML configures a different port for the 
 * GUI and some users. The relationship to the component tree should
 * be obvious.  
 * ```
 * "/WebConsoleTest":
 *   "/VueJsConsoleWeblet":
 *     "/WebConsole":
 *       "/ComponentCollector":
 *         "/LoginConlet":
 *           users:
 *             ...
 * ```
 *  
 * Developers may also be interested in the usage of channels
 * by the application's component:
 *
 * ![Main channels](app-channels.svg)
 * 
 * @startuml test-components.svg
 * skinparam component {
 *   BackGroundColor #FEFECE
 *   BorderColor #A80036
 *   BorderThickness 1.25
 *   BackgroundColor<<internal>> #F1F1F1
 *   BorderColor<<internal>> #181818
 *   BorderThickness<<internal>> 1
 * }
 * skinparam packageStyle rectangle
 * 
 * Component NioDispatcher as NioDispatcher <<internal>>
 * [WebConsoleTest] *-up- [NioDispatcher]
 * Component FileSystemWatcher as FileSystemWatcher <<internal>>
 * [WebConsoleTest] *-up- [FileSystemWatcher]
 * Component YamlConfigurationStore as YamlConfigurationStore <<internal>>
 * [WebConsoleTest] *-left- [YamlConfigurationStore]
 *
 * [WebConsoleTest] *-down- [SocketServer:9888]
 * [WebConsoleTest] *-right- [HttpServer]
 * Component PreferencesStore as PreferencesStore <<internal>>
 * [HttpServer] *-up- [PreferencesStore]
 * Component InMemorySessionManager as InMemorySessionManager <<internal>>
 * [HttpServer] *-up- [InMemorySessionManager]
 * Component LanguageSelector as LanguageSelector <<internal>>
 * [HttpServer] *-right- [LanguageSelector]
 * 
 * package "Conceptual WebConsole" {
 *   [ConsoleWeblet] *-- [WebConsole]
 * }
 * [HttpServer] *-- [ConsoleWeblet]
 * @enduml
 * 
 * @startuml console-components.svg
 * skinparam component {
 *   BackGroundColor #FEFECE
 *   BorderColor #A80036
 *   BorderThickness 1.25
 *   BackgroundColor<<internal>> #F1F1F1
 *   BorderColor<<internal>> #181818
 *   BorderThickness<<internal>> 1
 * }
 * skinparam packageStyle rectangle
 * 
 * Component BrowserLocalBackedKVStore as BrowserLocalBackedKVStore <<internal>>
 * [WebConsole] *-up- [BrowserLocalBackedKVStore]
 * Component KVStoreBasedConsolePolicy as KVStoreBasedConsolePolicy <<internal>>
 * [WebConsole] *-up- [KVStoreBasedConsolePolicy]
 * 
 * [WebConsole] *-- [RoleConfigurator]
 * [WebConsole] *-- [RoleConletFilter]
 * [WebConsole] *-left- [LoginConlet]
 * 
 * Component "ComponentCollector\nfor page resources" as cpr <<internal>>
 * [WebConsole] *-- [cpr]
 * Component "ComponentCollector\nfor conlets" as cc <<internal>>
 * [WebConsole] *-- [cc]
 * 
 * package "Providers and Conlets" {
 *   [Some component]
 * }
 * 
 * [cpr] *-- [Some component]
 * [cc] *-- [Some component]
 * @enduml
 * 
 * @startuml app-channels.svg
 * skinparam packageStyle rectangle
 * 
 * () "app" as mgr
 * mgr .left. [FileSystemWatcher]
 * mgr .right. [YamlConfigurationStore]
 * mgr .. [Controller]
 * mgr .up. [WebConsoleTest]
 * mgr .up. [VmWatcher]
 * mgr .. [Reconciler]
 * 
 * () "guiTransport" as hT
 * hT .up. [GuiSocketServer:8080]
 * hT .down. [HttpServer]
 * 
 * [YamlConfigurationStore] -right[hidden]- hT
 * 
 * () "guiHttp" as http
 * http .up. [HttpServer]
 * 
 * [PreferencesStore] .right. http
 * [InMemorySessionManager] .up. http
 * [LanguageSelector] .up. http
 * 
 * package "Conceptual WebConsole" {
 *   [ConsoleWeblet] .left. http
 *   [ConsoleWeblet] *-down- [WebConsole]
 * }
 * 
 * @enduml
 */

package org.jgrapes.webconsole.test;
