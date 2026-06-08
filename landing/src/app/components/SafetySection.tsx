import { ShieldCheck, Database, EyeOff, ClipboardList, Wrench } from "lucide-react";

const notices = [
  {
    icon: Database,
    title: "Beta okruženje",
    desc: "Zameni.rs je trenutno u javnoj beta fazi. Platforma se aktivno razvija i moguće su greške ili promene u radu.",
  },
  {
    icon: ShieldCheck,
    title: "Podaci mogu biti obrisani",
    desc: "Test podaci iz beta verzije mogu biti izmenjeni ili potpuno obrisani pre lansiranja produkcione verzije.",
  },
  {
    icon: EyeOff,
    title: "Bez osetljivih podataka",
    desc: "Unosi samo podatke koji su neophodni za testiranje i dogovor. Ne deli bankovne podatke ni druge osetljive lične informacije.",
  },
  {
    icon: ClipboardList,
    title: "Tim pregleda prijave",
    desc: "Tim platforme pregleda prijave grešaka i predloge koje korisnici šalju tokom beta faze.",
  },
  {
    icon: Wrench,
    title: "Produkcija može biti drugačija",
    desc: "Finalna verzija platforme može se razlikovati od trenutne beta verzije po funkcionalnostima, pravilima i izgledu.",
  },
];

export function SafetySection() {
  return (
    <section className="py-16 md:py-20" style={{ background: "var(--background)" }}>
      <div className="max-w-5xl mx-auto px-4 sm:px-6">
        {/* Header */}
        <div className="text-center max-w-xl mx-auto mb-10">
          <span
            className="inline-block text-xs font-semibold tracking-widest uppercase mb-3"
            style={{ color: "var(--muted-foreground)" }}
          >
            Važne napomene
          </span>
          <h2 style={{ fontSize: "clamp(1.4rem, 3vw, 2rem)", color: "var(--foreground)" }}>
            Šta treba da znate o beta fazi
          </h2>
        </div>

        {/* Notice cards */}
        <div
          className="rounded-2xl border overflow-hidden divide-y"
          style={{
            background: "white",
            borderColor: "var(--border-subtle)",
            boxShadow: "var(--shadow-sm)",
          }}
        >
          {notices.map(({ icon: Icon, title, desc }, i) => (
            <div
              key={i}
              className="flex gap-4 p-5 sm:p-6"
            >
              <div
                className="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center mt-0.5"
                style={{ background: "var(--primary-light)" }}
              >
                <Icon size={18} style={{ color: "var(--primary)" }} />
              </div>
              <div>
                <p className="text-sm font-semibold mb-0.5" style={{ color: "var(--foreground)" }}>
                  {title}
                </p>
                <p className="text-sm leading-relaxed" style={{ color: "var(--muted-foreground)" }}>
                  {desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
