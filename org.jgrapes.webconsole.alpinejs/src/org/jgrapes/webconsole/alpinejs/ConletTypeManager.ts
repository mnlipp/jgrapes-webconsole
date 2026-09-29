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

import JGConsole, { PageComponentSpecification, RenderMode } from "jgconsole";
import AlpineJsRenderer from "./AlpineJsRenderer";
import Alpine from "alpinejs";

export interface ConletType {
    type: string;
    renderModes: RenderMode[];
    displayNames: Map<string,string>;
}

export class ConletTypeManager {
    private renderer: AlpineJsRenderer;
    private conletTypes: ConletType[];
    private displayNames = new Map<string,Map<string,string>>();

    constructor(renderer: AlpineJsRenderer) {
        this.renderer = renderer;
        this.conletTypes = Alpine.reactive([]);
    }
        
    add(conletType: string, displayNames: Map<string,string>,
            renderModes: RenderMode[], 
            pageComponents: PageComponentSpecification[]) {
        let _this = this;
        _this.displayNames.set(conletType, displayNames);
        if (renderModes.includes(RenderMode.Preview)
            || renderModes.includes(RenderMode.View)) {
            // Add to menu
            _this.conletTypes.push({ type: conletType,
                renderModes, displayNames} satisfies ConletType);
        }
        // Add embedded to area(s)
        let header = <HTMLElement>document.querySelector("#ajs-console-header");
        for (let item of pageComponents) {
            if (item.area === "headerIcons") {
                this._embedAsHeaderIcon(header, conletType, item);
            }
        }
    }
    
    private _embedAsHeaderIcon(header: HTMLElement, conletType: string,
             spec: PageComponentSpecification) {
        let conlet = document.createElement("div");
        conlet.setAttribute("class", "conlet conlet-content");
        conlet.dataset["conletType"] = conletType;
        for (let prop in spec.properties) {
            conlet.dataset["conlet" + prop.substring(0,1).toUpperCase()
                + prop.substring(1)] = spec.properties[prop];
        }
        let conletPrio = parseInt(spec.properties["priority"] || "0");
        for (let idx = 0; idx < header.children.length; idx++) {
            let ref = <HTMLElement>header.children.item(idx);
            if (!("conletType" in ref.dataset)) {
                continue;
            }
            let refPrio = parseInt(ref.dataset["conletPriority"] || "0");
            if (conletPrio < refPrio || conletPrio == refPrio 
                    && conletType < ref.dataset["conletType"]!) {
                header.insertBefore(conlet, ref);
                return;
            }
        }
        header.append(conlet);
    }        

    update(conletType: string, renderModes: RenderMode[]) {
        // Remove from menu
        const conletTypes = this.conletTypes;
        conletTypes.splice(0, conletTypes.length,
            ...conletTypes.filter(el => el.type != conletType));
        let displayNames = this.displayNames.get(conletType)!;
        
        // Add to menu
        this.conletTypes.push({type: conletType,
            renderModes, displayNames} satisfies ConletType);
    }
    
    types() {
        return this.conletTypes;
    }
    
    menuItems() {
        const _this = this;
        const list = this.conletTypes.filter(
            el => el.renderModes.includes(RenderMode.Preview)
                || el.renderModes.includes(RenderMode.View));
        const locale = _this.renderer.locale();
        list.sort((a, b) => {
            const nameA = <string>JGConsole.forLang(a.displayNames, locale) || "";
            const nameB = <string>JGConsole.forLang(b.displayNames, locale) || "";
            return nameA.localeCompare(nameB, locale);
        });
        return list;
    }

    displayName(conletType: string): string | null {
        const displayNames = this.displayNames.get(conletType)!;
        return <string>JGConsole.forLang(
            displayNames, this.renderer.locale()) || "Conlet"
    }

}
