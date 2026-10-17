/**
 * AashTreeViewItem defintions.
 *
 * @module AashTreeView
 */
import Alpine from 'alpinejs';
import { TreeViewData } from './AashTreeView';
import { Controller, TreeNode } from './Controller';

interface TreeViewItemData extends TreeViewData {
    // Added by ul
    items: number,

    // Added by item
    expanded: boolean,
    segment: string,
    path: string[],
    
    // Added by x-for
    index: number,
    node: TreeNode
}

Alpine.data('AashTreeViewItem', () => ({
  expanded: false,
  segment: "",
  path: [],
    
  init() {
    const this_ = this as any;
    this_.segment = this_.node.segment;
    this_.path = this_.collectPath(this_.$el);
  }
}));

Alpine.bind('AashTreeViewItem', () => ({
  ':role'() {
    return 'treeitem';
  },
  
  ':aria-level'() {
    return (this as any as TreeViewItemData).path.length;
  },
  
  ':aria-setsize'() {
    const data = this as any as TreeViewItemData;
    return data.items;
  },
  
  ':aria-posinset'() {
    const data = this as any as TreeViewItemData;
    return data.index + 1;
  },
  
  ':aria-expanded'() {
    const data = this as any as TreeViewItemData;
    if (data.node.children) {
      return data.expanded;
    }
    return null;
  },
  
  ':tabindex'() {
    const data = this as any as TreeViewItemData;
    return data.ctrl.hasFocus(data.collectPath(data.$el)) ? 0 : -1;
  },
  
  '@click.stop'(e: Event) {
    (this as any as TreeViewItemData).onClick(e);
  },
  
  '@keydown.stop'(e: KeyboardEvent) {
    (this as any as TreeViewItemData).onKey(e);
  }
}));
  
