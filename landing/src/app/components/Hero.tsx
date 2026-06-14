import { ArrowRight, ChevronDown, Tag, RefreshCw, MessageCircle, Check } from "lucide-react";
import { DEMO_URL } from "@/config/demo";
import { trackCtaClick } from "@/lib/analytics";

const sampleItems = [
  { emoji: "🎸", name: "Električna gitara", category: "Muzika", user: "marko_ns" },
  { emoji: "📷", name: "Digitalni fotoaparat", category: "Elektronika", user: "ana_bg" },
  { emoji: "🚲", name: "Planinski bicikl", category: "Sport", user: "nikola_nis" },
  { emoji: "📚", name: "Kolekcija knjiga", category: "Knjige", user: "jelena_bk" },
];

function MockupCard() {
  return (
    <div className="relative w-full max-w-sm mx-auto">
      {/* Background cards (stacked effect) */}
      <div
        className="absolute top-3 left-3 right-3 bottom-0 rounded-2xl"
        style={{ background: "var(--primary-light)", opacity: 0.6 }}
      />
      <div
        className="absolute top-1.5 left-1.5 right-1.5 bottom-0 rounded-2xl"
        style={{ background: "var(--primary-light)", opacity: 0.8 }}
      />

      {/* Main card */}
      <div
        className="relative rounded-2xl p-4 space-y-3"
        style={{ background: "white", boxShadow: "var(--shadow-lg)" }}
      >
        {/* Header row */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center text-white text-xs"
              style={{ background: "var(--primary)" }}
            >
              Z
            </div>
            <span className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>
              Zameni.rs
            </span>
          </div>
          <span
            className="text-xs px-2 py-0.5 rounded-full font-medium"
            style={{ background: "var(--primary-light)", color: "var(--primary)" }}
          >
            Beta
          </span>
        </div>

        {/* Items list */}
        <div className="space-y-2">
          {sampleItems.map((item, i) => (
            <div
              key={i}
              className="flex items-center gap-3 p-2.5 rounded-xl border transition-all"
              style={{ borderColor: "var(--border-subtle)", background: i === 0 ? "var(--secondary)" : "transparent" }}
            >
              <div className="w-9 h-9 rounded-lg flex items-center justify-center text-lg" style={{ background: "var(--primary-light)" }}>
                {item.emoji}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate" style={{ color: "var(--foreground)" }}>
                  {item.name}
                </p>
                <div className="flex items-center gap-1.5">
                  <Tag size={10} style={{ color: "var(--muted-foreground)" }} />
                  <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>
                    {item.category}
                  </p>
                </div>
              </div>
              {i === 0 && (
                <div className="flex-shrink-0">
                  <div
                    className="text-xs px-2 py-0.5 rounded-full font-medium"
                    style={{ background: "var(--primary)", color: "white" }}
                  >
                    Zameni
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Exchange offer */}
        <div
          className="rounded-xl p-3 space-y-2"
          style={{ background: "var(--primary-light)" }}
        >
          <div className="flex items-center gap-2">
            <RefreshCw size={14} style={{ color: "var(--primary)" }} />
            <span className="text-xs font-semibold" style={{ color: "var(--primary)" }}>
              Ponuda za razmenu
            </span>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex-1 text-xs p-2 rounded-lg text-center" style={{ background: "white" }}>
              🎸 Gitara
            </div>
            <RefreshCw size={12} style={{ color: "var(--primary)" }} />
            <div className="flex-1 text-xs p-2 rounded-lg text-center" style={{ background: "white" }}>
              📷 Fotoaparat
            </div>
          </div>
          <div className="flex items-center gap-1.5">
            <MessageCircle size={11} style={{ color: "var(--muted-foreground)" }} />
            <span className="text-xs" style={{ color: "var(--muted-foreground)" }}>
              "Zainteresovan, možemo se dogovoriti?"
            </span>
          </div>
          <div className="flex gap-2">
            <button
              className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs text-white"
              style={{ background: "var(--primary)" }}
            >
              <Check size={11} />
              Prihvati
            </button>
            <button
              className="flex-1 py-1.5 rounded-lg text-xs"
              style={{ background: "white", color: "var(--muted-foreground)" }}
            >
              Odbij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export function Hero() {
  return (
    <section className="relative min-h-screen flex items-center overflow-hidden pt-16">
      {/* Subtle background gradient */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background: "radial-gradient(ellipse 80% 60% at 60% 30%, rgba(26,127,90,0.07) 0%, transparent 70%)",
        }}
      />

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-16 md:py-24 w-full">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-12 md:gap-16 items-center">
          {/* Text side */}
          <div className="space-y-6 order-2 md:order-1">
            {/* Beta badge */}
            <div className="inline-flex items-center gap-2">
              <span
                className="px-3 py-1 rounded-full text-xs font-semibold border"
                style={{ background: "var(--primary-light)", color: "var(--primary)", borderColor: "rgba(26,127,90,0.2)" }}
              >
                🚀 Javna beta verzija
              </span>
            </div>

            <div className="space-y-3">
              <h1
                className="leading-tight"
                style={{ fontSize: "clamp(2rem, 5vw, 3.25rem)", fontWeight: 800, color: "var(--foreground)" }}
              >
                Razmeni i pokloni stvari koje ti više{" "}
                <span style={{ color: "var(--primary)" }}>ne trebaju</span>
              </h1>
              <p className="text-lg" style={{ color: "var(--muted-foreground)", lineHeight: 1.7 }}>
                Zameni.rs pomaže ljudima da pronađu novu vrednost u stvarima koje već imaju — kroz razmenu i poklanjanje, umesto da ostanu neiskorišćene.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <a
                href={DEMO_URL}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => trackCtaClick('hero_isprobaj_beta')}
                className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl text-white transition-all duration-150 hover:opacity-90 active:scale-95"
                style={{ background: "var(--primary)", boxShadow: "0 4px 14px rgba(26,127,90,0.3)" }}
              >
                Isprobaj javnu beta
                <ArrowRight size={17} />
              </a>
              <a
                href="#kako-funkcionise"
                className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl border transition-all duration-150 hover:bg-secondary"
                style={{ borderColor: "var(--border)", color: "var(--foreground)" }}
              >
                Kako funkcioniše
                <ChevronDown size={16} />
              </a>
            </div>

            {/* Beta warning */}
            <div
              className="flex gap-3 p-3.5 rounded-xl border text-sm"
              style={{ background: "#fffbeb", borderColor: "#fde68a", color: "#92400e" }}
            >
              <span className="flex-shrink-0 mt-0.5">⚠️</span>
              <span>
                Beta verzija služi za testiranje. Podaci mogu biti izmenjeni ili obrisani, zato ne unosi osetljive ili poverljive lične podatke.
              </span>
            </div>
          </div>

          {/* Mockup side */}
          <div className="order-1 md:order-2 flex justify-center">
            <MockupCard />
          </div>
        </div>
      </div>
    </section>
  );
}
