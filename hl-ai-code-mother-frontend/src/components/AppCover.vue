<template>
  <div class="app-cover" :class="{ 'app-cover--zoom': zoomOnHover }" :style="coverStyle">
    <img v-if="src" :src="src" :alt="altText" class="app-cover-image" />
    <div v-else class="app-cover-placeholder" :class="{ featured }" :style="placeholderStyle">
      <span>{{ title || '未命名应用' }}</span>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  src?: string
  title?: string
  alt?: string
  featured?: boolean
  zoomOnHover?: boolean
  width?: string
  height?: string
  radius?: string
  padding?: string
  fontSize?: string
}

const props = withDefaults(defineProps<Props>(), {
  alt: '应用封面',
  featured: false,
  zoomOnHover: false,
  width: '100%',
  height: '220px',
  radius: '18px',
  padding: '20px',
  fontSize: '28px',
})

const altText = computed(() => props.alt || props.title || '应用封面')

const coverStyle = computed(() => ({
  width: props.width,
  height: props.height,
  borderRadius: props.radius,
}))

const placeholderStyle = computed(() => ({
  padding: props.padding,
  fontSize: props.fontSize,
}))
</script>

<style scoped>
.app-cover {
  position: relative;
  overflow: hidden;
  background: #f3f6fb;
}

.app-cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-cover--zoom:hover .app-cover-image {
  transform: scale(1.03);
}

.app-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background:
    linear-gradient(145deg, rgba(12, 24, 68, 0.88), rgba(73, 145, 255, 0.4)),
    linear-gradient(135deg, #eff6ff, #e0f2fe);
  color: #ffffff;
  font-weight: 600;
  line-height: 1.45;
  text-align: center;
}

.app-cover-placeholder.featured {
  background:
    linear-gradient(145deg, rgba(65, 16, 125, 0.88), rgba(234, 88, 12, 0.45)),
    linear-gradient(135deg, #fff7ed, #fae8ff);
}
</style>



