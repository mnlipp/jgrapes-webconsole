/*
* JGrapes Event Driven Framework
* Copyright (C) 2026  Michael N. Lipp
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

import JGConsole, { PageComponentSpecification, RenderMode, Conlet,
    Console, parseHtml } from "jgconsole";
import { GridStack, GridStackWidget, GridStackElement } from "gridstack";
import { AashTablist, createFragment } from "aash-alpinejs";
import AlpineJsRenderer from "./AlpineJsRenderer";
import Alpine from "alpinejs";

var log = JGConsole.Log;

class ConletView {
    id: string;
    title: string | (() => string) = '';
    
    constructor(id: string, title: string | (() => string)) {
        this.id = id;
        this.title = title;
    }
    
    get panelId() {
        return "conlet-panel-" + this.id;        
    }
    
    evalTitle() {
        if (typeof  this.title === 'function') {
            return this.title();
        }
        return this.title;
    }
}

class ConletHeader {
    // Storing Console causes "too much recursion"
    console: () => Console;
    conletId: string;
    title: string | (() => string) = '';
    modes: RenderMode[] = [];

    constructor(console: Console, conletId: string) {
        this.console = () => console;
        this.conletId = conletId;
    }
    
    evalTitle() {
        if (typeof  this.title === 'function') {
            return this.title();
        }
        return this.title;
    }

    isEditable () {
        return this.modes.includes(RenderMode.Edit);
    }

    isRemovable () {
        return !this.modes.includes(RenderMode.StickyPreview);
    }

    hasHelp () {
        return this.modes.includes(RenderMode.Help);
    }

    hasView () {
        return this.modes.includes(RenderMode.View);
    }

    edit() {
        this.console().renderConlet(
            this.conletId, [RenderMode.Edit, RenderMode.Foreground]);
    }
    
    removePreview() {
        this.console().removePreview(this.conletId)
    }
    
    showView() {
        this.console().renderConlet(
            this.conletId, [RenderMode.View, RenderMode.Foreground]);
    }
    
    showHelp() {
        this.console().renderConlet(
            this.conletId, [RenderMode.Help, RenderMode.Foreground]);
    }
}

export class ConletManager {
    private renderer: AlpineJsRenderer;
    private console: Console;
    private lastXtraInfo: any = {};
    private previewGrid: GridStack | null = null;
    private conletViews: ConletView[] = Alpine.reactive([]);

    constructor(renderer: AlpineJsRenderer, console: Console) {
        this.console = console;
        this.renderer = renderer;
        Alpine.data('conletHeader',
            (conletId: string) => new ConletHeader(this.console, conletId));
    }

    init () {
        var options = {
            cellHeight: 80,
            verticalMargin: 10,
            draggable: {
                handle: '.ui-draggable-handle' 
            },
            // alwaysShowResizeHandle: 'mobile' // now default 
            // /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
            disableOneColumnMode: true,
        };
        this.previewGrid = GridStack.init(options, '#consolePreviews');
        if (!this.previewGrid) {
            log.error("AlpineJsConsole: Creating preview grid failed.")
        }
    }
    
    private consoleTabs() {
        return (<AashTablist>document.querySelector("#consoleTabs"))!;
    }
    
    lastConsoleLayout(previewLayout: string[], tabsLayout: string[],
            xtraInfo: Object) {
        this.lastXtraInfo = xtraInfo;
    }
    
    layoutChanged() {
        let gridItems: HTMLElement[] = [];
        document.querySelectorAll("#consolePreviews .grid-stack-item")
            .forEach((element) => {
                gridItems.push(<HTMLElement>element);
        });
        gridItems.sort(function(a: HTMLElement, b: HTMLElement) {
            if (+a.getAttribute("gs-y")! != +b.getAttribute("gs-y")!) {
                return +a.getAttribute("gs-y")! - +b.getAttribute("gs-y")!;
            }
            return +a.getAttribute("gs-x")! - +b.getAttribute("gs-x")!;
        });

        let previewLayout:string[] = [];
        let xtraInfo: any = {};
        gridItems.forEach(function(item) {
            let conletId = (<HTMLElement>item.querySelector
                (".conlet-preview[data-conlet-id]")!).dataset["conletId"]!;
            previewLayout.push(conletId);
            xtraInfo[conletId] = [item.getAttribute("gs-x"), 
                item.getAttribute("gs-y"), item.getAttribute("gs-w"),
                item.getAttribute("gs-h")];
        });

        let tabsLayout = this.views.map(v => v.id);
        this.console.updateLayout(previewLayout, tabsLayout, xtraInfo);
    }
    
    updatePreview(isNew: boolean, conlet: Conlet, 
        modes: RenderMode[], content: HTMLElement[], foreground: boolean) {
        // Container is:
        //     <section class='conlet conlet-preview' data-conlet-id='...' 
        //     data-conlet-grid-columns='...' data-conlet-grid-rows='   '></section>"
        let _this = this;
        let container = conlet.element();
        let conletId = conlet.id();
        if (isNew) {
            container.append(createFragment(container, `
            <header class="ui-draggable-handle"
              x-data="conletHeader('${conletId}')">
              <p x-text="evalTitle()"></p>
              <button x-show="hasHelp()"
                type='button' class='fa fa-question-circle-o' @click="showHelp()"
              ></button><button x-show="isEditable"
                type='button' class='fa fa-wrench' @click="edit()"
              ></button><button x-show="isRemovable()" 
                type="button" class="fa fa-times" @click="removePreview()"
              ></button><button x-show="hasView()"
                type="button" class="fa fa-expand" @click="showView()"
              ></button>
            </header>
            <section></section>`));
            this.renderer.awaitInDom(container);

            // Get grid info
            let options: GridStackWidget = {};
            if (conletId in this.lastXtraInfo) {
                options.autoPosition = false;
                options.x = +this.lastXtraInfo[conletId][0];
                options.y = +this.lastXtraInfo[conletId][1];
                options.w = +this.lastXtraInfo[conletId][2];
                options.h = +this.lastXtraInfo[conletId][3];
            } else {
                options.autoPosition = true;
                options.w = 4;
                options.h = 4;
                if (content[0].dataset["conletGridColumns"]) {
                    options.w = +content[0].dataset["conletGridColumns"];
                }
                if (content[0].dataset["conletGridRows"]) {
                    options.h = +content[0].dataset["conletGridRows"];
                }
                if (window.innerWidth < 1200) {
                    let winWidth = Math.max(320, window.innerWidth);
                    let width = options.w;
                    width = Math.round(width + (12 - width) 
                        * (1 - (winWidth - 320) / (1200 - 320)));
                    options.w = width;
                }
            }

            // Put into grid item wrapper
            let gridItem = parseHtml
                ('<div class="grid-stack-item" role="gridcell"></div>')[0];
            container.classList.add('grid-stack-item-content');
            gridItem.append(container);

            // Finally add to grid
            this.previewGrid!.makeWidget(gridItem, options);
            this.layoutChanged();

            // Initialize header
            Alpine.initTree(container.querySelector(":scope > header")!);
        }
        const conletHeader
          = container.querySelector(":scope > header")! as HTMLElement;
        const headerData = Alpine.$data(conletHeader) as any
        headerData.title = _this._evaluateTitle(container, content[0]);
        headerData.modes = modes;
        
        let previewContent = container.querySelector("section")!;
        while (previewContent.firstChild) {
            previewContent.removeChild(previewContent.lastChild!);
        }
        previewContent.append(...content);
        if (foreground) {
            this.consoleTabs().selectPanel("consoleOverviewPanel");
        }
    }
    
    get views() {
        return this.conletViews;
    }
    
    updateView(isNew: boolean, conlet: Conlet, 
        modes: string[], content: HTMLElement[], foreground: boolean) {
        // Container is 
        //     <article class="conlet conlet-view 
        //              data-conlet-id='...'"></article>"
        let _this = this;
        let conletId = conlet.id();
        let panelId = "conlet-panel-" + conletId;
        let container = conlet.element();
        if (isNew) {
            container.setAttribute("id", panelId);
            container.setAttribute("hidden", "");
            container.append(...content);
            this.renderer.awaitInDom(container);            
            let consolePanels = <HTMLElement>document.querySelector("#consolePanels");
            consolePanels.append(container);
            // Add to tab list
            this.views.push(new ConletView(conletId,
                _this._evaluateTitle(container, content[0])));
            this.layoutChanged();
        } else {
            while (container.firstChild) {
                container.removeChild(container.lastChild!);
            }
            container.append(...content);
            const index = this.views
                .findIndex(view => view.id === conletId);
            if (index !== -1) {
                this.views[index].title
                    = _this._evaluateTitle(container, content[0]);
            }
        }
        if (foreground) {
            this.consoleTabs().selectPanel(panelId);
        }
    }

    private _evaluateTitle(
        container: HTMLElement, content: HTMLElement): string | (() => string) {
        let title: string | null | undefined = content.dataset["conletTitle"];
        if (!!title) {
            return title;
        }
        let conletType = container.dataset["conletType"];
        if (conletType) {
            return () => this.renderer.conletTypeMgr.displayName(conletType)
                || "(Untitled)";
        }
        return title || "(Untitled)";
    }

    removeView(conletId: string) {
        const index = this.views
            .findIndex(view => view.id === conletId);
        if (index !== -1) {
            this.views.splice(index, 1);
        }
        this.console.removeView(conletId);        
    }
    
    remove(conlets: Conlet[]) {
        let _this = this;
        conlets.forEach(function(conlet) {
            if (conlet.isPreview()) {
                let gridItem = conlet.element().closest(".grid-stack-item");
                _this.previewGrid!.removeWidget(<GridStackElement>gridItem);
            }
            if (conlet.isView()) {
                const index = _this.views
                    .findIndex(view => view.id === conlet.id());
                if (index !== -1) {
                    _this.views.splice(index, 1);
                }
                conlet.element().remove();
            }
        });
        this.consoleTabs().selectPanel("consoleOverviewPanel");
        this.layoutChanged();
    }
    
    updateTitle(conletId: string, title: string) {
        let preview = this.renderer.findConletPreview(conletId);
        if (preview) {
            let conletHeader = <HTMLElement>preview.element()
                .querySelector("section:first-child > header")!;
            (Alpine.$data(conletHeader) as any).title = title;
        }
        const index = this.views.findIndex(view => view.id === conletId);
        if (index !== -1) {
            this.views[index].title = title;
        }
    }
    
    updateModes(conletId: string, modes: RenderMode[]) {
        let conlet = this.renderer.findConletPreview(conletId);
        if (!conlet) {
            return;
        }
        let conletHeader = conlet.element()
            .querySelector(":scope > header") as HTMLElement;
        (Alpine.$data(conletHeader) as any).modes = modes;
    }
}
