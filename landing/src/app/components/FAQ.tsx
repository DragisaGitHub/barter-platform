import { useState } from "react";
import { ChevronDown } from "lucide-react";

const faqs = [
  {
    q: "Da li je Zameni.rs besplatan?",
    a: "Trenutno se Zameni.rs koristi bez naknade u javnoj beta fazi. Ako se način korišćenja ili uslovi u budućnosti promene, to će biti jasno istaknuto.",
  },
  {
    q: "Da li se stvari prodaju za novac?",
    a: "Zameni.rs je prvenstveno namenjen razmeni i poklanjanju predmeta. Platforma je osmišljena oko oglasa i ponuda za razmenu, ali ne možemo da garantujemo šta korisnici privatno dogovaraju van platforme, niti posredujemo u bilo kakvom plaćanju.",
  },
  {
    q: "Da li je ovo finalna verzija?",
    a: "Ne. Zameni.rs je trenutno u javnoj beta fazi i aktivno se razvija. Finalna verzija može imati dodatne funkcije, drugačiji izgled i preciznije definisana pravila korišćenja. Beta postoji kako bismo prikupili povratne informacije i unapredili iskustvo.",
  },
  {
    q: "Šta ako primetim grešku?",
    a: "Grešku ili problem možeš prijaviti unutar aplikacije kroz opciju za prijavu problema. Opiši šta se desilo i, ako možeš, dodaj dovoljno detalja da tim lakše proveri situaciju.",
  },
  {
    q: "Da li mogu da koristim pravi email?",
    a: "Možeš, ali preporučujemo oprez i deljenje samo podataka koji su potrebni za korišćenje platforme. Pošto je u pitanju beta okruženje, podaci mogu biti obrisani, pa mnogim korisnicima više odgovara posebna adresa za testiranje.",
  },
  {
    q: "Šta se dešava sa podacima iz beta verzije?",
    a: "Podaci uneti tokom beta faze, uključujući naloge, oglase i ponude, mogu biti izmenjeni ili obrisani pre zvaničnog lansiranja. Zato nemoj unositi sadržaj ili informacije koje ne želiš da izgubiš i sačuvaj kopiju onoga što ti je važno.",
  },
];

function FAQItem({ q, a }: { q: string; a: string }) {
  const [open, setOpen] = useState(false);

  return (
    <div
      className="border-b last:border-b-0"
      style={{ borderColor: "var(--border-subtle)" }}
    >
      <button
        className="w-full flex items-start justify-between gap-4 py-5 text-left"
        onClick={() => setOpen(!open)}
      >
        <span className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>
          {q}
        </span>
        <ChevronDown
          size={18}
          className="flex-shrink-0 mt-0.5 transition-transform duration-200"
          style={{
            color: "var(--muted-foreground)",
            transform: open ? "rotate(180deg)" : "rotate(0deg)",
          }}
        />
      </button>
      {open && (
        <div className="pb-5">
          <p className="text-sm leading-relaxed" style={{ color: "var(--muted-foreground)" }}>
            {a}
          </p>
        </div>
      )}
    </div>
  );
}

export function FAQ() {
  return (
    <section id="faq" className="py-20 md:py-28" style={{ background: "var(--secondary)" }}>
      <div className="max-w-3xl mx-auto px-4 sm:px-6">
        {/* Header */}
        <div className="text-center mb-12">
          <span
            className="inline-block text-xs font-semibold tracking-widest uppercase mb-3"
            style={{ color: "var(--primary)" }}
          >
            Pitanja
          </span>
          <h2 style={{ fontSize: "clamp(1.6rem, 4vw, 2.5rem)", color: "var(--foreground)" }}>
            Često postavljana pitanja
          </h2>
        </div>

        {/* FAQ list */}
        <div
          className="rounded-2xl border overflow-hidden"
          style={{
            background: "white",
            borderColor: "var(--border-subtle)",
            boxShadow: "var(--shadow-sm)",
          }}
        >
          <div className="px-6">
            {faqs.map((faq, i) => (
              <FAQItem key={i} q={faq.q} a={faq.a} />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
