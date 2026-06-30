import { ArrowRight, Bug, Lightbulb, MessageCircle } from "lucide-react";
import { APP_URL } from "@/config/app";
import { trackCtaClick } from "@/lib/analytics";

const feedbackPerks = [
  {
    icon: Bug,
    title: "Prijavi grešku",
    desc: "Svaka prijavljena greška pomaže nam da platforma bude stabilnija i jasnija za korišćenje.",
  },
  {
    icon: Lightbulb,
    title: "Predloži poboljšanje",
    desc: "Tvoje iskustvo nam pomaže da odredimo šta dalje unapređujemo u proizvodu.",
  },
  {
    icon: MessageCircle,
    title: "Isprobaj sve funkcije",
    desc: "Postavi predmet, pošalji ponudu i dogovori se oko razmene — sve je dostupno odmah.",
  },
];

export function BetaSection() {
  return (
    <section
      id="beta-testiranje"
      className="py-20 md:py-28 relative overflow-hidden"
      style={{ background: "var(--primary)" }}
    >
      {/* Subtle decorative circles */}
      <div
        className="absolute -top-24 -right-24 w-72 h-72 rounded-full pointer-events-none opacity-10"
        style={{ background: "white" }}
      />
      <div
        className="absolute -bottom-16 -left-16 w-56 h-56 rounded-full pointer-events-none opacity-10"
        style={{ background: "white" }}
      />

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6">
        {/* Header */}
        <div className="text-center max-w-2xl mx-auto mb-12">
          <span
            className="inline-block text-xs font-semibold tracking-widest uppercase mb-3 px-3 py-1 rounded-full"
            style={{ background: "rgba(255,255,255,0.15)", color: "rgba(255,255,255,0.9)" }}
          >
            Rano izdanje
          </span>
          <h2
            style={{ fontSize: "clamp(1.6rem, 4vw, 2.5rem)", color: "white" }}
          >
            Pomogni nam da napravimo bolju platformu
          </h2>
          <p className="mt-4 text-base" style={{ color: "rgba(255,255,255,0.8)", lineHeight: 1.7 }}>
            Zameni.rs je sada dostupan svima. Možeš da napraviš nalog, postaviš predmet i odmah počneš da razmenjuješ.
            Platforma je u ranom izdanju — neke funkcije se još razvijaju, a svaka prijava greške, predlog ili komentar pomažu da Zameni.rs bude korisniji i pouzdaniji.
          </p>
        </div>

        {/* Perks */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5 mb-10">
          {feedbackPerks.map(({ icon: Icon, title, desc }) => (
            <div
              key={title}
              className="rounded-2xl p-5"
              style={{ background: "rgba(255,255,255,0.1)", backdropFilter: "blur(8px)" }}
            >
              <div
                className="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
                style={{ background: "rgba(255,255,255,0.2)" }}
              >
                <Icon size={18} style={{ color: "white" }} />
              </div>
              <p className="text-sm font-semibold mb-1" style={{ color: "white" }}>
                {title}
              </p>
              <p className="text-sm" style={{ color: "rgba(255,255,255,0.75)", lineHeight: 1.6 }}>
                {desc}
              </p>
            </div>
          ))}
        </div>

        {/* CTA */}
        <div className="flex justify-center">
          <a
            href={APP_URL}
            target="_blank"
            rel="noopener noreferrer"
            onClick={() => trackCtaClick('beta_section_otvori_app')}
            className="inline-flex items-center gap-2.5 px-8 py-4 rounded-xl font-semibold text-base transition-all duration-150 hover:opacity-95 active:scale-95"
            style={{
              background: "white",
              color: "var(--primary)",
              boxShadow: "0 4px 20px rgba(0,0,0,0.15)",
            }}
          >
            Otvori aplikaciju
            <ArrowRight size={18} />
          </a>
        </div>
      </div>
    </section>
  );
}
