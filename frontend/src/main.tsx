
  import { createRoot } from "react-dom/client";
  import { initSentry } from "./lib/sentry";
  import App from "./app/App.tsx";
  import "./i18n";
  import "./styles/index.css";

  // Initialize Sentry before rendering — no-op if DSN is not configured
  initSentry();

  createRoot(document.getElementById("root")!).render(<App />);
  