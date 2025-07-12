import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// 按需导入插件
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 只配置Element Plus的自动导入
    AutoImport({
      resolvers: [ElementPlusResolver()],
      // 只导入最常用的API
      imports: ['vue', 'vue-router']
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    host: true, // 设置为true将监听所有可用网络接口
    proxy: {
      '/api': {
        target: 'http://0.0.0.0:8081', // 使用0.0.0.0代替localhost
        changeOrigin: true
      }
    }
  },
  // 添加构建优化
  build: {
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 压缩输出
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 生产环境移除console
        drop_debugger: true // 生产环境移除debugger
      }
    },
    // 分块策略
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
        }
      }
    }
  }
})
