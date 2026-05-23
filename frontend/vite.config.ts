import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // host: true expoe o dev server na rede local — permite abrir no celular
    // (use o endereco "Network" mostrado ao rodar `npm run dev`).
    host: true,
  },
  build: {
    // O vendor-antd fica ~1,1 MB num chunk proprio (de proposito): e uma lib
    // estavel, cacheada entre deploys. O aviso padrao de 500 kB nao se aplica.
    chunkSizeWarningLimit: 1300,
    rollupOptions: {
      output: {
        // Separa as libs pesadas em chunks proprios — assim o bundle inicial
        // fica menor e o navegador cacheia os vendors entre deploys.
        manualChunks: {
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-antd': ['antd', '@ant-design/icons', '@ant-design/v5-patch-for-react-19'],
          'vendor-data': ['@tanstack/react-query', 'axios', 'zustand'],
        },
      },
    },
  },
});
