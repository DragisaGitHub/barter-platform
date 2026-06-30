import { Banknote, Repeat2, ShieldAlert, Rocket } from "lucide-react";

const benefits = [
  {
    icon: Banknote,
    title: "Razmena i poklanjanje",
    desc: "Platforma je prvenstveno namenjena razmeni i poklanjanju predmeta.",
  },
  {
    icon: Repeat2,
    title: "Jednostavna razmena",
    desc: "Pošalji ponudu i dogovori se direktno.",
  },
  {
    icon: ShieldAlert,
    title: "Prijava problema timu",
    desc: "Greške i predlozi stižu timu koji razvija platformu.",
  },
  {
    icon: Rocket,
    title: "Dostupno svima",
    desc: "Registruj se i počni odmah — besplatno, bez čekanja.",
  },
];

export function TrustStrip() {
  return (
    <section className="border-y" style={{ borderColor: "var(--border-subtle)", background: "white" }}>
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          {benefits.map(({ icon: Icon, title, desc }) => (
            <div key={title} className="flex flex-col items-center text-center gap-2">
              <div
                className="w-11 h-11 rounded-xl flex items-center justify-center"
                style={{ background: "var(--primary-light)" }}
              >
                <Icon size={20} style={{ color: "var(--primary)" }} />
              </div>
              <p className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>
                {title}
              </p>
              <p className="text-xs" style={{ color: "var(--muted-foreground)", lineHeight: 1.6 }}>
                {desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
