import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      dts: 'src/components.d.ts',
      resolvers: [
        ElementPlusResolver({
          importStyle: 'css',
          directives: true,
        }),
      ],
    }),
  ],
  optimizeDeps: {
    // Current workspace path contains non-ASCII characters on Windows.
    // Disabling dep discovery avoids Rolldown panic during dev-time optimization.
    noDiscovery: true,
    // Element Plus pulls in dayjs as CommonJS, so keep a minimal safe pre-bundle list.
    include: [
      '@paper-design/shaders-react',
      'dayjs',
      'dayjs/plugin/customParseFormat.js',
      'dayjs/plugin/localeData.js',
      'dayjs/plugin/advancedFormat.js',
      'dayjs/plugin/weekOfYear.js',
      'dayjs/plugin/weekYear.js',
      'dayjs/plugin/dayOfYear.js',
      'dayjs/plugin/isSameOrAfter.js',
      'dayjs/plugin/isSameOrBefore.js',
      'react',
      'react-dom',
      'react-dom/client',
    ],
  },
  server: {
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
      },
    },
  },
})
