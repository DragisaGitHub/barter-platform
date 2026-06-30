import { APP_URL } from "@/config/app";

const footerLinks = [
  { label: "Otvori aplikaciju", href: APP_URL, external: true },
  { label: "Kontakt (uskoro)", href: "#" },
  { label: "Uslovi korišćenja (uskoro)", href: "#" },
  { label: "Politika privatnosti (uskoro)", href: "#" },
];

export function Footer() {
  return (
    <footer
      className="border-t py-10"
      style={{ background: "white", borderColor: "var(--border-subtle)" }}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        <div className="flex flex-col md:flex-row items-center md:items-start justify-between gap-6">
          {/* Brand */}
          <div className="flex flex-col items-center md:items-start gap-1.5">
            <div className="flex items-center gap-2">
              <div
                className="w-7 h-7 rounded-lg flex items-center justify-center text-white text-xs"
                style={{ background: "var(--primary)" }}
              >
                Z
              </div>
              <span className="font-bold" style={{ color: "var(--foreground)" }}>
                Zameni.rs
              </span>
            </div>
            <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
              Platforma za razmenu i poklanjanje stvari.
            </p>
            <span
              className="text-xs px-2.5 py-1 rounded-full font-medium mt-1"
              style={{ background: "var(--primary-light)", color: "var(--primary)" }}
            >
              Rano izdanje — dobrodošli svi.
            </span>
          </div>

          {/* Links */}
          <nav className="flex flex-wrap justify-center md:justify-end gap-x-6 gap-y-2">
            {footerLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                target={link.external ? "_blank" : undefined}
                rel={link.external ? "noopener noreferrer" : undefined}
                className="text-sm transition-colors hover:text-primary"
                style={{ color: "var(--muted-foreground)" }}
              >
                {link.label}
              </a>
            ))}
          </nav>
        </div>

        <div
          className="mt-8 pt-6 border-t text-center"
          style={{ borderColor: "var(--border-subtle)" }}
        >
          <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>
            © {new Date().getFullYear()} Zameni.rs · Razmeni. Pokloni. Produži život stvarima. · Sva prava zadržana.
          </p>
        </div>
      </div>
    </footer>
  );
}
