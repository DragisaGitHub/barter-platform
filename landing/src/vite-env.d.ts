/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEMO_URL?: string;
  readonly VITE_GA_MEASUREMENT_ID?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Extend Window for gtag / dataLayer
interface Window {
  dataLayer: unknown[];
  gtag: (...args: unknown[]) => void;
}

