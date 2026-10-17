/**
 * Provides a tree view.
 *
 * @module AashTreeView
 */
import Alpine from 'alpinejs';
import { AashComponent, Attribute, Data, Renderer,
  expressionConverter, booleanConverter, functionConverter
} from '../../AashUtil';
import { Controller, TreeNode, ToggleVetoer, LabelSupplier
} from './Controller';
// Force loading, defines web component and defintions
import './AashTreeViewGroup';
import './AashTreeViewItem';

/**
 * @internal AlpineJS data definition for the tree view component.
 */
export interface TreeViewData {
  ctrl: Controller;
  collectPath: (el: HTMLElement) => string[];
  label: (node: TreeNode) => string;
  onClick: (event: Event) => void;
  onKey: (event: KeyboardEvent) => void;
  $el: HTMLElement;
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
@AashComponent()
export class AashTreeView extends HTMLElement {

  /** @internal Reactive state managed by AlpineJS */
  private readonly ajsData: TreeViewData;

  constructor() {
    super();
    const this_ = this;
    this.ajsData = Alpine.reactive({
      ctrl: new Controller(
        [],
        ((_path, newState, _event) => newState) as ToggleVetoer,
        () => {},
        () => {},
        false
      ),

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
        this_.onClick(event);
      },

      onKey(event: KeyboardEvent) {
        this_.onKey(event);
      }
    } as TreeViewData);
  }

  protected connectedCallback() {
    this.ajsData.ctrl.setDomRoot(this);
  }

  @Data()
  protected data(): object {
    return this.ajsData;
  }
  
  /** @internal Renders the tree with Alpine directives. */
  @Renderer()
  protected render() {
    this.innerHTML =
      `<ul role="tree" x-data="{ items: ctrl.roots.length }" x-cloak>
        <template x-for="(node, index) in ctrl.roots" :key="index">
          <li x-data="AashTreeViewItem" x-bind="AashTreeViewItem">
            <span x-text="label(node)"></span>
            <template x-if="node.children">
              <aash2-tree-view-group></aash2-tree-view-group>
            </template>
          </li>
        </template>
      </ul>`;
  }
  
  onClick(event: Event) {
    const ctrl = this.ajsData.ctrl;
    const path = ctrl.collectPath(event.target as HTMLElement);
    ctrl.setFocus(path);
    if (ctrl.isExpandable(path)) {
      ctrl.toggleExpanded(path, event);
    } else {
      ctrl.onSelected(path, event);
    }
    ctrl.updateDomFocus();
  }
  
  onKey(event: KeyboardEvent) {
    const ctrl = this.ajsData.ctrl;
    let path = ctrl.collectPath(event.target as HTMLElement);
    if (event.key === 'Enter' || event.key === ' ') {
      ctrl.setFocus(path);
      if (!ctrl.isExpandable(path)) {
        ctrl.onSelected(path, event);
      }
      return;
    }
    if (event.key === 'ArrowRight') {
      if (ctrl.isExpandable(path)) {
        if (!ctrl.isExpanded(path)) {
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
    if (event.key === 'ArrowLeft') {
      if (ctrl.isExpanded(path)) {
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
    if (event.key === 'ArrowDown') {
      if (!ctrl.nextDown(path)) {
        return;
      }
      ctrl.setFocus(path);
      event.preventDefault();
      ctrl.updateDomFocus();
      return;
    }
    if (event.key === 'ArrowUp') {
      if (!ctrl.prevUp(path)) {
        return;
      }
      ctrl.setFocus(path);
      event.preventDefault();
      ctrl.updateDomFocus();
      return;
    }
    if (event.key === 'Home') {
      const roots = ctrl.roots;
      if (roots.length > 0) {
        path = [roots[0].segment];
        ctrl.setFocus(path);
        event.preventDefault();
        ctrl.updateDomFocus();
      }
      return;
    }
    if (event.key === 'End') {
      const roots = ctrl.roots;
      if (roots.length > 0) {
        path = [roots[roots.length - 1].segment];
        while (ctrl.nextDown(path)) {
          /* Everything happens in the condition. */
        }
        ctrl.setFocus(path);
        event.preventDefault();
        ctrl.updateDomFocus();
      }
    }
  }

  /**
   * Replaces the root tree nodes.
   * @param roots the new root nodes
   */
  @Attribute('roots')
  set roots(roots: TreeNode[]) {
    this.ajsData.ctrl.resetState(roots);
  }

  /**
   * Sets the toggle vetoer callback.
   * @param onToggle the vetoer function
   */
  @Attribute('on-toggle', expressionConverter)
  set onToggle(value: ToggleVetoer) {
    (this.ajsData.ctrl as unknown as Record<string, unknown>)._onToggle =
      value;
  }

  /**
   * Sets the focus callback.
   * @param value the focus handler
   */
  @Attribute('on-focus', expressionConverter)
  set onFocus(value: (path: string[]) => void) {
    (this.ajsData.ctrl as unknown as Record<string, unknown>)._onFocus =
      value;
  }

  /**
   * Sets the selection callback.
   * @param value the selection handler
   */
  @Attribute('on-selected', expressionConverter)
  set onSelected(value: (path: string[], event: Event) => void) {
    (this.ajsData.ctrl as unknown as Record<string, unknown>)._onSelected =
      value;
  }

  /**
   * Whether opening a path closes sibling paths.
   */
  @Attribute('single-path', booleanConverter)
  set singlePath(value: boolean) {
    (this.ajsData.ctrl as unknown as Record<string, unknown>)._singlePath =
      value;
  }
}

customElements.define('aash2-tree-view', AashTreeView);
