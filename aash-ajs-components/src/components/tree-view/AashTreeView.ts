/**
 * Provides a tree view.
 *
 * @module AashTreeView
 */
import Alpine from 'alpinejs';
import { Renderer } from '../../AashUtil';

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
  segment: string;
  /** The label to show in the view */
  label: LabelSupplier;
  /** The node's child nodes */
  children: TreeNode[];
};

/**
 * The interface provided by the component.
 *
 * `setRoots(roots: TreeNode[]): void` replaces the root tree nodes.
 *
 * @memberof module:AashTreeView
 */
export interface Api {
  setRoots(roots: TreeNode[]): void;
}

/** @internal Presentation state for tree expansion tracking */
interface Presentation {
  children: Map<string, Presentation> | null;
  expanded: boolean;
}

/**
 * A function invoked with the desired new state before a tree node
 * is expanded. Return the new state (may veto the change).
 *
 * @memberof module:AashTreeView
 */
export type ToggleVetoer = (
  path: string[],
  expand: boolean,
  event: Event
) => boolean;

/**
 * @internal AlpineJS data definition for the tree view component.
 */
interface TreeViewData {
  ctrl: Controller;
  collectPath: (el: HTMLElement) => string[];
  label: (node: TreeNode) => string;
  onClick: (event: Event) => void;
  onKey: (event: KeyboardEvent) => void;
}

/**
 * @internal Controller class managing tree state and navigation logic.
 */
class Controller {
  private _roots: TreeNode[];
  private _domRoot: HTMLElement | null = null;
  private _onToggle: ToggleVetoer;
  private _onFocus: (path: string[]) => void;
  private _onSelected: (path: string[], event: Event) => void;
  private _singlePath: boolean;
  private _expanded: Presentation = { children: null, expanded: false };
  private _focusHolder: string[] = [];

  constructor(
    roots: TreeNode[],
    onToggle: ToggleVetoer,
    onFocus: (path: string[]) => void,
    onSelected: (path: string[], event: Event) => void,
    singlePath: boolean
  ) {
    this._roots = Alpine.reactive(roots);
    if (roots.length > 0) {
      this._focusHolder.push(roots[0].segment);
    }
    this._onToggle = onToggle;
    this._onFocus = onFocus;
    this._onSelected = onSelected;
    this._singlePath = singlePath;
  }

  setDomRoot(root: HTMLElement) {
    this._domRoot = root;
  }

  get roots() {
    return this._roots;
  }

  toNode(path: string[]): TreeNode | null {
    let node: TreeNode | null = null;
    let children = this._roots;
    for (const segment of path) {
      node = null;
      for (const child of children) {
        if (child.segment === segment) {
          node = child;
          children = node.children;
        }
      }
    }
    return node;
  }

  nextSibling(path: string[]): boolean {
    if (path.length === 0) {
      return false;
    }
    const lastSeg = path.pop()!;
    const nodes =
      path.length === 0 ? this._roots : this.toNode(path)!.children;
    for (let i = 0; i < nodes.length - 1; i++) {
      if (nodes[i].segment === lastSeg) {
        path.push(nodes[i + 1].segment);
        return true;
      }
    }
    path.push(lastSeg);
    return false;
  }

  nextDown(path: string[]): boolean {
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

  prevSibling(path: string[]): boolean {
    if (path.length === 0) {
      return false;
    }
    const lastSeg = path.pop()!;
    const nodes =
      path.length === 0 ? this._roots : this.toNode(path)!.children;
    for (let i = nodes.length - 1; i > 0; i--) {
      if (nodes[i].segment === lastSeg) {
        path.push(nodes[i - 1].segment);
        return true;
      }
    }
    path.push(lastSeg);
    return false;
  }

  prevUp(path: string[]): boolean {
    if (this.prevSibling(path)) {
      while (this.isExpanded(path)) {
        const node = this.toNode(path)!;
        path.push(node.children[node.children.length - 1].segment);
      }
    } else {
      if (path.length <= 1) {
        return false;
      }
      path.pop();
    }
    return true;
  }

  collectPath(leaf: HTMLElement): string[] {
    const path: string[] = [];
    let cur: HTMLElement | null | undefined = leaf;
    while (
      !cur ||
      cur.getAttribute('role')?.toLowerCase() !== 'tree'
    ) {
      if (cur?.dataset['segment']) {
        path.unshift(cur.dataset['segment']!);
      }
      cur = cur?.parentElement ?? null;
    }
    return path;
  }

  isExpandable(path: string[]): boolean {
    const node = this.toNode(path);
    return !!(node && node.children && node.children.length > 0);
  }

  isExpanded(path: string[]): boolean {
    let cur = this._expanded;
    for (let i = 0; ; i++) {
      if (i === path.length) {
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
      if (i === path.length) {
        if (!cur.expanded && !this.isExpandable(path)) {
          return;
        }
        const newState = this._onToggle(path, !cur.expanded, event);
        if (newState === cur.expanded) {
          return;
        }
        cur.expanded = !cur.expanded;
        return;
      }
      if (!cur.children) {
        cur.children = new Map();
      }
      if (!cur.children.has(path[i])) {
        cur.children.set(path[i], { children: null, expanded: false });
      }
      if (this._singlePath) {
        for (const [key, value] of cur.children) {
          if (key !== path[i] && value.expanded) {
            value.expanded = this._onToggle(
              path.slice(i + 1),
              false,
              event
            );
          }
        }
      }
      cur = cur.children.get(path[i])!;
    }
  }

  onSelected(path: string[], event: Event) {
    this._onSelected(path, event);
  }

  hasFocus(path: string[]): boolean {
    if (this._focusHolder.length !== path.length) {
      return false;
    }
    for (let i = 0; i < this._focusHolder.length; i++) {
      if (this._focusHolder[i] !== path[i]) {
        return false;
      }
    }
    return true;
  }

  setFocus(path: string[]) {
    if (this._focusHolder.length === path.length) {
      let foundDiff = false;
      for (let seg = 0; seg < path.length; seg++) {
        if (this._focusHolder[seg] !== path[seg]) {
          foundDiff = true;
          break;
        }
      }
      if (!foundDiff) {
        return;
      }
    }
    this._focusHolder.length = 0;
    this._focusHolder.push(...path);
    this._onFocus(path);
  }

  updateDomFocus() {
    requestAnimationFrame(() => {
      let cur: Element | null | undefined = this._domRoot;
      for (let i = 0; i < this._focusHolder.length; i++) {
        if (cur == null) {
          return;
        }
        cur = cur.querySelector(
          ":scope [data-segment='" + this._focusHolder[i] + "']"
        );
      }
      cur = cur?.querySelector(':scope [tabindex]');
      (cur as HTMLElement | null)?.focus();
    });
  }

  ariaExpanded(path: string[]): string | null {
    if (!this.isExpandable(path)) {
      return null;
    }
    return String(this.isExpanded(path));
  }

  resetState(roots: TreeNode[]) {
    this._roots.length = 0;
    this._roots.push(...roots);
    this._focusHolder.length = 0;
    if (roots.length > 0) {
      this._focusHolder.push(roots[0].segment);
    }
    this._expanded = { children: null, expanded: false };
  }
}

/**
 * @classdesc
 * Generates a tree view.
 *
 * Example:
 * ```html
 * <aash-tree-view id="myTree" single-path>
 * </aash-tree-view>
 * <script>
 *   const tree = document.getElementById('myTree');
 *   tree.setRoots([
 *     { segment: 'a', label: 'A',
 *       children: [
 *         { segment: 'a1', label: 'A1', children: [] }
 *       ]}
 *   ]);
 * </script>
 * ```
 *
 * @class AashTreeView
 */
export class AashTreeView extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: TreeViewData;

  /** @internal Controller instance */
  private readonly ctrl: Controller;

  constructor() {
    super();
    const element = this;
    this.ctrl = new Controller(
      [],
      (_path, newState, _event) => newState,
      () => {},
      () => {},
      false
    );
    this.ajsData = Alpine.reactive({
      ctrl: this.ctrl,

      collectPath(el: HTMLElement): string[] {
        return this.ctrl.collectPath(el);
      },

      label(node: TreeNode): string {
        if (typeof node.label === 'function') {
          return node.label();
        }
        return node.label;
      },

      onClick(event: Event) {
        const path = this.ctrl.collectPath(
          event.target as HTMLElement
        );
        this.ctrl.setFocus(path);
        if (this.ctrl.isExpandable(path)) {
          this.ctrl.toggleExpanded(path, event);
        } else {
          this.ctrl.onSelected(path, event);
        }
        element.updateAriaExpanded();
        this.ctrl.updateDomFocus();
      },

      onKey(event: KeyboardEvent) {
        if (event.key === 'Enter' || event.key === ' ') {
          this.onClick(event);
          return;
        }
        let path = this.ctrl.collectPath(
          event.target as HTMLElement
        );
        if (event.key === 'ArrowRight') {
          if (this.ctrl.isExpandable(path)) {
            if (!this.ctrl.isExpanded(path)) {
              this.ctrl.toggleExpanded(path, event);
              element.updateAriaExpanded();
            } else {
              path.push(
                this.ctrl.toNode(path)!.children[0].segment
              );
              this.ctrl.setFocus(path);
              event.preventDefault();
              this.ctrl.updateDomFocus();
            }
          }
          return;
        }
        if (event.key === 'ArrowLeft') {
          if (this.ctrl.isExpanded(path)) {
            this.ctrl.toggleExpanded(path, event);
            element.updateAriaExpanded();
            return;
          }
          if (path.length > 1) {
            path.pop();
            this.ctrl.setFocus(path);
            event.preventDefault();
            this.ctrl.updateDomFocus();
          }
          return;
        }
        if (event.key === 'ArrowDown') {
          if (!this.ctrl.nextDown(path)) {
            return;
          }
          this.ctrl.setFocus(path);
          event.preventDefault();
          this.ctrl.updateDomFocus();
          return;
        }
        if (event.key === 'ArrowUp') {
          if (!this.ctrl.prevUp(path)) {
            return;
          }
          this.ctrl.setFocus(path);
          event.preventDefault();
          this.ctrl.updateDomFocus();
          return;
        }
        if (event.key === 'Home') {
          const roots = this.ctrl.roots;
          if (roots.length > 0) {
            path = [roots[0].segment];
            this.ctrl.setFocus(path);
            event.preventDefault();
            this.ctrl.updateDomFocus();
          }
          return;
        }
        if (event.key === 'End') {
          const roots = this.ctrl.roots;
          if (roots.length > 0) {
            path = [roots[roots.length - 1].segment];
            while (this.ctrl.nextDown(path)) {
              /* Everything happens in the condition. */
            }
            this.ctrl.setFocus(path);
            event.preventDefault();
            this.ctrl.updateDomFocus();
          }
        }
      }
    } as TreeViewData);
  }

  /** @internal Updates aria-expanded attributes after toggle */
  private updateAriaExpanded() {
    const items = this.querySelectorAll(
      '[data-segment][role="treeitem"]'
    );
    for (const item of items) {
      const path = this.ctrl.collectPath(item as HTMLElement);
      const val = this.ctrl.ariaExpanded(path);
      if (val === null) {
        (item as HTMLElement).removeAttribute('aria-expanded');
      } else {
        (item as HTMLElement).setAttribute('aria-expanded', val);
      }
    }
  }

  protected connectedCallback() {
    /* Attach ctrl to root for nested x-for scope access. */
    (this as unknown as Record<string, unknown>)._aashCtrl = this.ctrl;
    Alpine.addScopeToNode(
      this,
      this.ajsData as unknown as Record<string, unknown>
    );
    this.ctrl.setDomRoot(this);
    this.render();
    Alpine.initTree(this);
  }

  /** @internal Renders the tree with Alpine directives. */
  @Renderer()
  protected render() {
    this.innerHTML =
      `<ul role="tree"
          @click="onClick" @keydown="onKey" x-cloak>
        <template x-for="(node, index) in ctrl.roots">
          <li x-bind:role="ctrl.isExpandable(
              [node.segment]) ? 'treeitem' : 'none'"
              x-bind:data-segment="node.segment"
              x-bind:aria-level="1"
              x-bind:aria-setsize="ctrl.roots.length"
              x-bind:aria-posinset="index + 1"
              x-bind:aria-expanded="ctrl.ariaExpanded(
                  [node.segment])"
              x-bind:tabindex="ctrl.hasFocus(
                  [node.segment]) ? 0 : -1">
            <span @click.stop x-text="label(node)"></span>
            <template x-if="node.children">
              <ul role="group">
                <template x-for="(child, ci) in node.children">
                  <li x-bind:role="ctrl.isExpandable(
                      [node.segment, child.segment])
                      ? 'treeitem' : 'none'"
                      x-bind:data-segment="child.segment"
                      x-bind:aria-level="2"
                      x-bind:aria-setsize="node.children.length"
                      x-bind:aria-posinset="ci + 1"
                      x-bind:aria-expanded="ctrl.ariaExpanded(
                          [node.segment, child.segment])"
                      x-bind:tabindex="ctrl.hasFocus(
                          [node.segment, child.segment])
                          ? 0 : -1">
                    <span @click.stop
                        x-text="label(child)"></span>
                  </li>
                </template>
              </ul>
            </template>
          </li>
        </template>
      </ul>`;
  }

  /**
   * Replaces the root tree nodes.
   * @param roots the new root nodes
   */
  setRoots(roots: TreeNode[]): void {
    this.ctrl.resetState(roots);
    this.render();
    Alpine.initTree(this);
    this.updateAriaExpanded();
  }

  /**
   * Sets the toggle vetoer callback.
   * @param onToggle the vetoer function
   */
  set onToggle(value: ToggleVetoer) {
    (this.ctrl as unknown as Record<string, unknown>)._onToggle =
      value;
  }

  /**
   * Sets the focus callback.
   * @param value the focus handler
   */
  set onFocus(value: (path: string[]) => void) {
    (this.ctrl as unknown as Record<string, unknown>)._onFocus =
      value;
  }

  /**
   * Sets the selection callback.
   * @param value the selection handler
   */
  set onSelected(value: (path: string[], event: Event) => void) {
    (this.ctrl as unknown as Record<string, unknown>)._onSelected =
      value;
  }

  /**
   * Whether opening a path closes sibling paths.
   */
  set singlePath(value: boolean) {
    (this.ctrl as unknown as Record<string, unknown>)._singlePath =
      value;
  }
}

customElements.define('aash-tree-view', AashTreeView);
