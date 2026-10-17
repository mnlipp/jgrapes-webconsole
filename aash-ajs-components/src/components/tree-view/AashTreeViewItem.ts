/**
 * AashTreeViewItem defintions.
 *
 * @module AashTreeView
 */
import Alpine from 'alpinejs';
import { TreeViewData } from './AashTreeView';
import { Controller, TreeNode } from './Controller';

/**
 * @internal AlpineJS data shape for a tree view item. Extends the base
 * TreeViewData with item-specific properties.
 */
interface TreeViewItemData extends TreeViewData {
  /** Number of sibling items in the same group (injected by parent <ul>) */
  items: number;
  /** Whether the item's child group is currently expanded */
  expanded: boolean;
  /** Segment identifier of this node */
  segment: string;
  /** Full path from root to this node */
  path: string[];

  /** Zero-based index among siblings (injected by x-for) */
  index: number;
  /** The tree node data for this item */
  node: TreeNode
}

/** @internal Alpine data registration: initializes segment and path from node. */
Alpine.data('AashTreeViewItem', () => ({
  /** Initial expansion state */
  expanded: false,
  /** Node segment identifier */
  segment: "",
  /** Full path from root */
  path: [],

  /** @internal Computes segment and path from the node and DOM structure. */
  init() {
    const this_ = this as any;
    this_.segment = this_.node.segment;
    this_.path = this_.collectPath(this_.$el);
  }
}));

/**
 * @internal Alpine bind registration: sets ARIA attributes and event handlers
 * for tree item accessibility.
 */
Alpine.bind('AashTreeViewItem', () => ({
  /** Sets role="treeitem" on the <li> element. */
  ':role'() {
    return 'treeitem';
  },

  /** Sets aria-level to the depth (path length) of the item. */
  ':aria-level'() {
    return (this as any as TreeViewItemData).path.length;
  },

  /** Sets aria-setsize to the number of sibling items. */
  ':aria-setsize'() {
    const data = this as any as TreeViewItemData;
    return data.items;
  },

  /** Sets aria-posinset to the 1-based position among siblings. */
  ':aria-posinset'() {
    const data = this as any as TreeViewItemData;
    return data.index + 1;
  },

  /**
   * Sets aria-expanded for nodes with children. Returns null (omitted) for
   * leaf nodes to indicate they are not expandable.
   */
  ':aria-expanded'() {
    const data = this as any as TreeViewItemData;
    if (data.node.children) {
      return data.expanded;
    }
    return null;
  },

  /** Sets tabindex=0 for the focused item, -1 for all others. */
  ':tabindex'() {
    const data = this as any as TreeViewItemData;
    return data.ctrl.hasFocus(data.collectPath(data.$el)) ? 0 : -1;
  },

  /** Delegated click handler for focus, expansion, and selection. */
  '@click.stop'(e: Event) {
    (this as any as TreeViewItemData).onClick(e);
  },

  /** Delegated keyboard handler for tree navigation. */
  '@keydown.stop'(e: KeyboardEvent) {
    (this as any as TreeViewItemData).onKey(e);
  }
}));
  
