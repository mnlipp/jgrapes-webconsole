<template>
  <div>
    <component :is="headerType" 
      :role="headerType.match('[hH][123456789]') ? null : 'header'">
      <component :is="buttonType" :id="sectionId + '-control'" tabindex="0"
        :role="buttonType == 'button' ? null : 'button'"
        :aria-controls="sectionId + '-panel'"
        :aria-expanded="isExpanded ? 'true' : 'false'"
        @click="onClick($event)" @keydown="onKey($event)">
        <slot name="title"><span>{{ title }}</span></slot>
      </component>
    </component>
    <component :is="panelType" :id="sectionId + '-panel'" ref="panel" 
      role="region" :class="panelClass" 
      :style="{ padding: 0, height: height }"
      :aria-labelledby="sectionId + '-control'"
      :hidden="isExpanded ? null : ''"
      :data-transitioning="isTransitioning ? '' : null">
      <div><slot></slot></div>
    </component>
  </div>
</template>

<script lang="ts" src="./AashAccordionSection.ts"></script>
