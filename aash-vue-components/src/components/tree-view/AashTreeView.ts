/**
 * Provides a tree view.
 *
 * @module AashTreeView
 */
import { defineComponent, PropType, reactive, computed, nextTick,
    onMounted, ref } from 'vue'
import { provideApi } from "../../AashUtil";

/**
 * A label can either be provided literally or by a function. 
 *
 * @memberof module:AashTreeView
 */
export type LabelSupplier = string | (() => string);

/**
 * The information about a tree node managed by the view. 
 *
 * @memberof module:AashTreeView
 */
export type TreeNode = {
    /** The name of the branch */
    segment: string,
    /** The label to show in the view */
    label: LabelSupplier,
    /** The node's child nodes */
    children: TreeNode[]
}

/**
 * The interface provided by the component.
 *
 * * `setRoots(roots: TreeNode[]): void`: replaces the root tree nodes
 *
 * @memberof module:AashTreeView
 */
export interface Api {
  setRoots(roots: TreeNode[]): void;
}

interface Expanded {
    children: Map<string,Expanded> | null;
    expanded: boolean;
}

/**
 * A function that is invoked with the desired new state before a
 * tree node is expanded. 
 *
 * @memberof module:AashTreeView
 */
export type ToggleVetoer = (path: string[], expand: boolean, 
                event: Event) => boolean;

class Controller {
    private _roots: TreeNode[];
    private _domRoot: HTMLElement | null = null;
    private _onToggle: ToggleVetoer;
    private _onFocus: (path: string[]) => void;
    private _expanded: Expanded = reactive({ children: null, expanded: false });
    private _focusHolder: string[] = reactive([]);

    constructor(roots: TreeNode[], onToggle: ToggleVetoer, 
        onFocus: (path: string[]) => void) {
        this._roots = reactive(roots);
        if (roots.length > 0) {
            this._focusHolder.push(roots[0].segment);
        }
        this._onToggle = onToggle;
        this._onFocus = onFocus;
    }

    setDomRoot(root: HTMLElement) {
        this._domRoot = root;
    }

    get roots() {
        return this._roots;
    }

    toNode(path: string[]) {
        let node: TreeNode | null = null;
        let children = this._roots;
        for (let segment of path) {
            node = null;
            for (let child of children) {
                if (child.segment == segment) {
                    node = child;
                    children = node.children;
                }
            }
        }
        return node;
    }

    nextSibling(path: string[]) {
        if (path.length == 0) {
            return false;
        }
        let lastSeg = path.pop()!;
        let nodes = path.length == 0 ? this._roots : this.toNode(path)!.children;
        for (let i = 0; i < nodes.length - 1; i++) {
            if (nodes[i].segment == lastSeg) {
                path.push(nodes[i+1].segment);
                return true;
            }
        }
        path.push(lastSeg);
        return false;
    }

    nextDown(path: string[]) {
        if (this.isExpanded(path)) {
            path.push(this.toNode(path)!.children[0].segment);
            return true;
        }
        if (this.nextSibling(path)) {
            return true;
        }
        while (path.length > 1) {
            path.pop();
            if (this.nextSibling(path)) {
                return true;
            }
        }
        return false;
    }

    prevSibling(path: string[]) {
        if (path.length == 0) {
            return false;
        }
        let lastSeg = path.pop()!;
        let nodes = path.length == 0 ? this._roots : this.toNode(path)!.children;
        for (let i = nodes.length - 1; i > 0; i--) {
            if (nodes[i].segment == lastSeg) {
                path.push(nodes[i-1].segment);
                return true;
            }
        }
        path.push(lastSeg);
        return false;
    }

    prevUp(path: string[]) {
        if (this.prevSibling(path)) {
            while (this.isExpanded(path)) {
                let node = this.toNode(path)!;
                path.push(node.children[
                    node.children.length-1].segment);
            }
        } else {
            if (path.length <= 1) {
                return false;
            }
            path.pop();
        }
        return true;
    }

    collectPath(leaf: HTMLElement) {
        let path: string[] = [];
        while (!leaf.getAttribute("role")
            || leaf.getAttribute("role")?.toLowerCase() != "tree") {
            if (leaf.dataset["segment"]) {
                path.unshift(leaf.dataset["segment"]!);
            }
            leaf = leaf.parentElement!;
        }
        return path;
    }

    isExpandable(path: string[]) {
        let node = this.toNode(path); 
        return node && node["children"] && node.children.length > 0;
    }
    
    isExpanded(path: string[]) {
        let cur = this._expanded;
        for (let i = 0; ; i++) {
            if (i == path.length) {
                return cur.expanded;
            }
            if (!cur.children || !cur.children.has(path[i])) {
                return false;
            }
            cur = cur.children.get(path[i])!;
        }
    }

    toggleExpanded(path: string[], event: Event) {
        let cur = this._expanded;
        for (let i = 0; ; i++) {
            if (i == path.length) {
                if (!cur.expanded && !this.isExpandable(path)) {
                    return;
                }
                let newState = this._onToggle(path, !cur.expanded, event);
                if (newState == cur.expanded) {
                    return;
                }
                cur.expanded = !cur.expanded;
                return;
            }
            if (!cur.children) {
                cur.children = new Map();
            }
            if (!cur.children.has(path[i])) {
                cur.children.set(path[i], { children: null, expanded: false })
            }
            cur = cur.children.get(path[i])!;
        }
        
    }

    hasFocus(path: string[]) {
        if (this._focusHolder.length != path.length) {
            return false;
        }
        for (let i = 0; i < this._focusHolder.length; i++) {
            if (this._focusHolder[i] != path[i]) {
                return false;
            }
        }
        return true;
    }
    
    setFocus(path: string[]) {
        this._focusHolder.length = 0;
        this._focusHolder.push(...path);
        this._onFocus(path);
    }
    
    updateDomFocus() {
        nextTick (() => {
            let cur: Element | null | undefined = this._domRoot;
            for (let i = 0; i < this._focusHolder.length; i++) {
                if (cur == null) {
                    return;
                }
                cur = cur.querySelector(":scope [data-segment='"
                    + this._focusHolder[i] + "']");
            }
            cur = cur?.querySelector(":scope [tabindex]");
            (<HTMLElement>cur)?.focus();
        });
    }
}

/**
 * @classdesc
 * Generates a tree view.
 *
 * @class AashTreeViewComponent
 * @param {Object} props the properties
 * @param {string} props.id the id for the enclosing `div` (optional)
 * @param {TreeNode[]} props.roots the top level tree nodes
 * @param {ToggleVetoer} props.onToggle an optional vetoer which is
 *      invoked before a node is opened or closed
 * @param {Function} props.onFocus a function ((path: string[]) => void)
 *      that is invoked when a node receives the focus (by clicking or
 *      by using the navigation keys)
 */
export default defineComponent({
    props: {
        id: { type: String, required: false, default: null },
        roots: { type: Array as PropType<TreeNode[]>, required: false, default: [] },
        onToggle: {
            type: Function as PropType<ToggleVetoer>,
            default: (path: string[], newStateOpen: boolean, event: Event) => {
                return newStateOpen;
            }
        },
        onFocus: {
            type: Function as PropType<(path: string[]) => void>,
            default: (path: string[]) => {}
        },
        _controller: { type: Controller, required: false },
        _path: { type: Array as PropType<string[]>, required: false, default: [] },
        _nodes: { type: Array as PropType<TreeNode[]>, required: false },
    },
    
    setup(props) {
        let ctrl = props._controller 
            || new Controller(props.roots, props.onToggle, props.onFocus);

        const nodes = computed(() => {
            return (props._path.length == 0 ? ctrl.roots : props._nodes)!
                .map((node) => { 
                    return {...node, path: props._path.concat(node.segment)} });
        });
        
        const isExpandable = (path: string[]) => {
            return ctrl.isExpandable(path);
        };

        const isExpanded = (path: string[]) => {
            return ctrl.isExpanded(path);
        };

        const ariaExpanded = (path: string[]) => {
            if (!isExpandable(path)) {
                return null;
            }
            return new Boolean(isExpanded(path)).toString();
        };

        const hasFocus = (path: string[]) => {
            return ctrl.hasFocus(path);
        };

        const label = (node: TreeNode) => {
            if(typeof node.label === 'function') {
                return node.label();
            }
            return node.label;
        }
        
        const toggleExpanded = (event: Event) => {
            event.preventDefault();
            let path = ctrl.collectPath(<HTMLElement>event.target);
            ctrl.toggleExpanded(path, event);
            ctrl.setFocus(path);
            ctrl.updateDomFocus();
        }

        const onKey = (event: KeyboardEvent) => {
            let path = ctrl.collectPath(<HTMLElement>event.target);
            if (event.key === "ArrowRight") {
                if (ctrl.isExpandable(path)) {
                    if (!isExpanded(path)) {
                        ctrl.toggleExpanded(path, event);
                    } else {
                        path.push(ctrl.toNode(path)!.children[0].segment);
                        ctrl.setFocus(path);
                        event.preventDefault();
                        ctrl.updateDomFocus();
                   }
                }
                return;
            }
            if (event.key === "ArrowLeft") {
                if (isExpanded(path)) {
                    ctrl.toggleExpanded(path, event);
                    return;
                }
                if (path.length > 1) {
                    path.pop();
                    ctrl.setFocus(path);
                    event.preventDefault();
                    ctrl.updateDomFocus();
                }
                return;
            }
            if (event.key === "ArrowDown") {
                if (!ctrl.nextDown(path)) {
                    return;
                }
                ctrl.setFocus(path);
                event.preventDefault();
                ctrl.updateDomFocus();
                return;
            }
            if (event.key === "ArrowUp") {
                if (!ctrl.prevUp(path)) {
                    return;
                }
                ctrl.setFocus(path);
                event.preventDefault();
                ctrl.updateDomFocus();
                return;
            }
            if (event.key === "Home") {
                path = [props.roots[0].segment];
                ctrl.setFocus(path);
                event.preventDefault();
                ctrl.updateDomFocus();
                return;
            }
            if (event.key === "End") {
                path = [props.roots[props.roots.length - 1].segment];
                while (ctrl.nextDown(path)) {
                }
                ctrl.setFocus(path);
                event.preventDefault();
                ctrl.updateDomFocus();
                return;
            }
        }

        const domRoot = ref(null);

        onMounted(() => {
            ctrl.setDomRoot(domRoot.value!);
        });

        provideApi(domRoot, { 
            setRoots: (roots: TreeNode[]) => {
                ctrl.roots.length = 0;
                ctrl.roots.push(...roots);
            }
        });
        
        return { domRoot, ctrl, nodes, isExpandable, isExpanded, ariaExpanded,
            hasFocus, label, toggleExpanded, onKey };
    }
});
