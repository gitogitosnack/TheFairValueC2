// vitest.config.js
import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // src/main/resources/static/js の階層構造をミラーした src/test/resources/static/js 内の
    // .test.js または .spec.js を対象にする
    include: ['src/test/resources/static/js/**/*.{test,spec}.js'],
    environment: 'jsdom',
  },
})