import { createRoot } from "react-dom/client";
import App from "./app/App.tsx";
import "./styles/index.css";
import { initGA } from "./lib/analytics";

// Initialize Google Analytics 4 (only active in production with VITE_GA_MEASUREMENT_ID set)
initGA();

createRoot(document.getElementById("root")!).render(<App />);
