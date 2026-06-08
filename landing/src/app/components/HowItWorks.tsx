const steps = [
  {
    number: "01",
    title: "Postavi oglas",
    desc: "Fotografiši predmet, dodaj kratak opis i objavi oglas u nekoliko jednostavnih koraka.",
    emoji: "📝",
  },
  {
    number: "02",
    title: "Pronađi zanimljiv predmet",
    desc: "Pregledaj dostupne oglase i pronađi nešto što ti odgovara za razmenu ili poklanjanje.",
    emoji: "🔍",
  },
  {
    number: "03",
    title: "Pošalji ponudu za razmenu",
    desc: "Odaberi koji od tvojih predmeta nudiš u zamenu i pošalji ponudu vlasniku. Možeš dodati i kratku poruku.",
    emoji: "✉️",
  },
  {
    number: "04",
    title: "Dogovori se sa korisnikom",
    desc: "Vlasnik može da prihvati ili odbije tvoju ponudu. Ako se dogovorite, dalje detalje razmene organizujete međusobno.",
    emoji: "🤝",
  },
];

export function HowItWorks() {
  return (
    <section id="kako-funkcionise" className="py-20 md:py-28" style={{ background: "var(--background)" }}>
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-14">
          <span
            className="inline-block text-xs font-semibold tracking-widest uppercase mb-3"
            style={{ color: "var(--primary)" }}
          >
            Proces
          </span>
          <h2 style={{ fontSize: "clamp(1.6rem, 4vw, 2.5rem)", color: "var(--foreground)" }}>
            Kako funkcioniše
          </h2>
          <p className="mt-3 text-base" style={{ color: "var(--muted-foreground)", lineHeight: 1.7 }}>
            Četiri jednostavna koraka do dogovora oko razmene.
          </p>
        </div>

        {/* Steps */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((step, i) => (
            <div key={step.number} className="relative">
              {/* Connector line (hidden on last item) */}
              {i < steps.length - 1 && (
                <div
                  className="hidden lg:block absolute top-10 left-[calc(50%+2.5rem)] right-[-50%] h-px"
                  style={{ background: "var(--border)" }}
                />
              )}

              <div
                className="relative rounded-2xl p-6 h-full border transition-shadow hover:shadow-md"
                style={{
                  background: "white",
                  borderColor: "var(--border-subtle)",
                  boxShadow: "var(--shadow-sm)",
                }}
              >
                {/* Number badge */}
                <div className="flex items-start justify-between mb-4">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center text-2xl"
                    style={{ background: "var(--primary-light)" }}
                  >
                    {step.emoji}
                  </div>
                  <span
                    className="text-2xl font-black opacity-10 select-none"
                    style={{ color: "var(--primary)" }}
                  >
                    {step.number}
                  </span>
                </div>

                <h3 className="text-base font-semibold mb-2" style={{ color: "var(--foreground)" }}>
                  {step.title}
                </h3>
                <p className="text-sm leading-relaxed" style={{ color: "var(--muted-foreground)" }}>
                  {step.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
