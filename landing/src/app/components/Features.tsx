import {
  ImagePlus,
  ArrowLeftRight,
  Gift,
  Bell,
  UserCircle,
  MessageSquareWarning,
} from "lucide-react";

const features = [
  {
    icon: ImagePlus,
    title: "Oglasi sa fotografijama",
    desc: "Dodaj fotografije i osnovne informacije kako bi drugi lakše razumeli šta nudiš.",
  },
  {
    icon: ArrowLeftRight,
    title: "Ponude za razmenu",
    desc: "Šalji i primaj ponude za razmenu na jednom mestu, uz jasan pregled predmeta koje nudite.",
  },
  {
    icon: Gift,
    title: "Poklanjanje stvari",
    desc: "Ako ne tražiš ništa zauzvrat, predmet možeš da ponudiš i kao poklon.",
  },
  {
    icon: Bell,
    title: "Pregled aktivnosti",
    desc: "Lakše pratiš šta se dešava sa tvojim oglasima, ponudama i odgovorima drugih korisnika.",
  },
  {
    icon: UserCircle,
    title: "Profili korisnika",
    desc: "Profil korisnika daje pregled oglasa i iskustava iz razmena, kako bi dogovor bio pregledniji.",
  },
  {
    icon: MessageSquareWarning,
    title: "Prijave i predlozi timu",
    desc: "Ako primetiš problem ili imaš ideju, možeš da pošalješ povratnu informaciju timu platforme.",
  },
];

export function Features() {
  return (
    <section
      id="funkcionalnosti"
      className="py-20 md:py-28"
      style={{ background: "var(--secondary)" }}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-14">
          <span
            className="inline-block text-xs font-semibold tracking-widest uppercase mb-3"
            style={{ color: "var(--primary)" }}
          >
            Šta nudi platforma
          </span>
          <h2 style={{ fontSize: "clamp(1.6rem, 4vw, 2.5rem)", color: "var(--foreground)" }}>
            Funkcionalnosti
          </h2>
          <p className="mt-3 text-base" style={{ color: "var(--muted-foreground)", lineHeight: 1.7 }}>
            Funkcionalnosti koje olakšavaju razmenu i poklanjanje stvari.
          </p>
        </div>

        {/* Cards grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {features.map(({ icon: Icon, title, desc }) => (
            <div
              key={title}
              className="rounded-2xl p-6 border transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md cursor-default"
              style={{
                background: "white",
                borderColor: "var(--border-subtle)",
                boxShadow: "var(--shadow-sm)",
              }}
            >
              <div
                className="w-11 h-11 rounded-xl flex items-center justify-center mb-4"
                style={{ background: "var(--primary-light)" }}
              >
                <Icon size={20} style={{ color: "var(--primary)" }} />
              </div>
              <h3 className="text-base font-semibold mb-2" style={{ color: "var(--foreground)" }}>
                {title}
              </h3>
              <p className="text-sm leading-relaxed" style={{ color: "var(--muted-foreground)" }}>
                {desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
