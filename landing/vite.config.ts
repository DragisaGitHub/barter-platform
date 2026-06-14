import { defineConfig, type Plugin } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'


function figmaAssetResolver() {
  return {
    name: 'figma-asset-resolver',
    resolveId(id: string) {
      if (id.startsWith('figma:asset/')) {
        const filename = id.replace('figma:asset/', '')
        return path.resolve(__dirname, 'src/assets', filename)
      }
    },
  }
}

/**
 * Strips the GA4 script block from index.html when VITE_GA_MEASUREMENT_ID is
 * not set, so no broken requests are made in dev/preview without a valid ID.
 */
function conditionalGA(): Plugin {
  return {
    name: 'conditional-ga',
    transformIndexHtml(html: string) {
      const id = process.env.VITE_GA_MEASUREMENT_ID?.trim();
      if (!id) {
        return html.replace(
          /<!-- Google Analytics 4[\s\S]*?<\/script>\s*<\/script>/,
          '<!-- Google Analytics 4 — disabled (no VITE_GA_MEASUREMENT_ID) -->',
        );
      }
      return html;
    },
  };
}

export default defineConfig({
  plugins: [
    figmaAssetResolver(),
    conditionalGA(),
    // The React and Tailwind plugins are both required for Make, even if
    // Tailwind is not being actively used – do not remove them
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      // Alias @ to the src directory
      '@': path.resolve(__dirname, './src'),
    },
  },

  // File types to support raw imports. Never add .css, .tsx, or .ts files to this.
  assetsInclude: ['**/*.svg', '**/*.csv'],
})
