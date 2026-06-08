{/* MARKER-MAKE-KIT-INVOKED */}
import { Header } from "./components/Header";
import { Hero } from "./components/Hero";
import { TrustStrip } from "./components/TrustStrip";
import { HowItWorks } from "./components/HowItWorks";
import { Features } from "./components/Features";
import { BetaSection } from "./components/BetaSection";
import { SafetySection } from "./components/SafetySection";
import { FAQ } from "./components/FAQ";
import { Footer } from "./components/Footer";

export default function App() {
  return (
    <div className="min-h-screen" style={{ fontFamily: "system-ui, -apple-system, sans-serif" }}>
      <Header />
      <main>
        <Hero />
        <TrustStrip />
        <HowItWorks />
        <Features />
        <BetaSection />
        <SafetySection />
        <FAQ />
      </main>
      <Footer />
    </div>
  );
}
