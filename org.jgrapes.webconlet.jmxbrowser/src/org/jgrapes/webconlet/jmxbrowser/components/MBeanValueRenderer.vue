<template>
  <ul v-if="value instanceof Array">
    <li v-for="item in value">
       <mbean-value-renderer :value="item"></mbean-value-renderer>
    </li>
  </ul>
  <template v-else-if="typeof value !== 'object' || value === null">
    {{ value }}
  </template>
  <pre v-else>
{{ JSON.stringify(value, null, 2) }}
  </pre>
</template>

<script lang="ts" src="./MBeanValueRenderer.ts"></script>
