/**
 * Provides a group within a tree view.
 *
 * @module AashTreeView
 */
import Alpine from 'alpinejs';
import { AashComponent, Renderer } from '../../AashUtil';

/**
 * @classdesc
 * A group within a tree view.
 *
 * @class AashTreeViewGroup
 */
@AashComponent()
export class AashTreeViewGroup extends HTMLElement {

  /** @internal Renders the tree with Alpine directives. */
  @Renderer()
  protected render() {
    this.innerHTML =
      `<ul role="group" x-data="{ group: ctrl.toNode(path), items: 0 }"
        x-init="items = group.children.length"
        x-show="expanded">
        <template x-for="(node, index) in group.children" :key="index">
          <li x-data="AashTreeViewItem" x-bind="AashTreeViewItem">
            <span x-text="label(node)"></span>
            <template x-if="node.children">
              <aash2-tree-view-group></aash2-tree-view-group>
            </template>
          </li>
        </template>
      </ul>`;
  }

}

customElements.define('aash2-tree-view-group', AashTreeViewGroup);
