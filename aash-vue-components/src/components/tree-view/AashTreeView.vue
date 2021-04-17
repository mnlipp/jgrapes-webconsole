<template>
  <ul v-if="_path.length == 0" ref="domRoot" :id="id" :role="'tree'" 
    @click="toggleExpanded($event)" @keydown="onKey($event)">
    <li v-for="(node, index) in nodes"
      :role="isExpandable(node.path) ? 'treeitem' : 'none'"
      :data-segment="node.segment"
      :aria-level="_path.length + 1" :aria-setsize="nodes.length" 
      :aria-posinset="index + 1" 
      :aria-expanded="ariaExpanded(node.path)">
      <slot :label="label(node)" :tabindex="hasFocus(node.path) ? 0 : -1"
        ><span :tabindex="hasFocus(node.path) ? 0 : -1"
        >{{ label(node) }}</span></slot>
      <aash-tree-view v-if="node['children']"
        :_controller="ctrl" :_path="node.path" 
        :_nodes="node.children">
        <template v-for="(_, slot) of $slots" 
            v-slot:[slot]="scope"><slot :name="slot" v-bind="scope"/></template>
      </aash-tree-view>
    </li>
  </ul>
  <ul v-else :role="'group'">
    <li v-for="(node, index) in nodes"
      :role="isExpandable(node.path) ? 'treeitem' : 'none'"
      :data-segment="node.segment"
      :aria-level="_path.length + 1" :aria-setsize="nodes.length" 
      :aria-posinset="index + 1" 
      :aria-expanded="ariaExpanded(node.path)">
      <slot :label="label(node)" :tabindex="hasFocus(node.path) ? 0 : -1"
        ><span :tabindex="hasFocus(node.path) ? 0 : -1"
        >{{ label(node) }}</span></slot>
      <aash-tree-view v-if="node['children']"
        :_controller="ctrl" :_path="node.path" 
        :_nodes="node.children">
        <template v-for="(_, slot) of $slots" 
            v-slot:[slot]="scope"><slot :name="slot" v-bind="scope"/></template>
      </aash-tree-view>
    </li>
  </ul>
</template>

<script lang="ts" src="./AashTreeView.ts"></script>

<style>
[role="tree"] [role="treeitem"][aria-expanded="false"] > ul {
    display: none;
}
</style>