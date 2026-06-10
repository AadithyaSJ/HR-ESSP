<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppLayout from './components/AppLayout.vue';

const route = useRoute();
const isPublicRoute = computed(() => !!route.meta.public);
</script>

<template>
  <!-- Public auth screens direct render -->
  <div v-if="isPublicRoute">
    <router-view />
  </div>
  <!-- Main shell layout wrap for internal pages -->
  <AppLayout v-else />
</template>

<style scoped></style>
