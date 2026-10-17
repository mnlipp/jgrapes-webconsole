import Alpine from 'alpinejs';

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
 * @internal Controller class managing tree state and navigation logic.
 * @memberof module:AashTreeView
 */
export class Controller {
  private _roots: TreeNode[];
  private _domRoot: HTMLElement | null = null;
  private _onToggle: ToggleVetoer;
  private _onFocus: (path: string[]) => void;
  private _onSelected: (path: string[], event: Event) => void;
  private _singlePath: boolean;
  private _focusHolder: string[] = [];
  private _lastExpanded: string[] = [];

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
  
  collectPath(item: HTMLElement): string[] {
    const path: string[] = [];
    let cur: HTMLElement | null | undefined = item;
    while (cur && cur.getAttribute('role')?.toLowerCase() !== 'tree') {
      if (cur.tagName.toLowerCase() === 'li') {
        const data = Alpine.$data(cur) as any;
        if (data.segment) {
          path.unshift(data.segment);
        }
      }
      cur = cur.parentElement ?? null;
    }
    return path;
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

  toItemElement(path: string[]): HTMLElement | null {
    let list: HTMLElement | null = this._domRoot!.querySelector(":scope ul");
    let cur: HTMLElement | null = null;
    for (const segment of path) {
      cur = null;
      if (!list) {
        break;
      }
      for (const item of list!.querySelectorAll<HTMLElement>(":scope li")) {
        const data = Alpine.$data(item) as any;
        if (data.segment && data.segment === segment) {
          cur = item;
          list = item.querySelector(":scope ul");
          break;
        }
      }
    }
    return cur;
  }
  
  toItemData(path: string[]): any | null {
    const item = this.toItemElement(path);
    if (!item) {
        return null;
    }
    return Alpine.$data(item) as any;
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

  isExpandable(path: string[]): boolean {
    const node = this.toNode(path);
    return !!(node && node.children && node.children.length > 0);
  }

  isExpanded(path: string[]): boolean {
    return this.toItemData(path)?.expanded || false;
  }

  toggleExpanded(path: string[], event: Event) {
    const data = this.toItemData(path);
    if (!data) {
        return;
    }
    const newState = this._onToggle(path, !data.expanded, event);
    if (newState === data.expanded) {
        return;
    }
    if (newState) {
        if (this._singlePath && this._lastExpanded
          && (path.length < this._lastExpanded.length
              || !this._lastExpanded.every(
                (value, index) => path[index] === value))) {
          const last = this.toItemData(this._lastExpanded);
          last.expanded = false;
        }
        this._lastExpanded = path;
    }
    data.expanded = newState;
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
      this.toItemElement(this._focusHolder)?.focus();
    });
  }

  ariaExpanded(path: string[]): string | null {
    if (!this.isExpandable(path)) {
      return null;
    }
    return String(this.isExpanded(path));
  }

  resetState(roots: TreeNode[]) {
    this._roots = roots;
    this._focusHolder.length = 0;
    if (roots.length > 0) {
      this._focusHolder.push(roots[0].segment);
    }
  }
}
