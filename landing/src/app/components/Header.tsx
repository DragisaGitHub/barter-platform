import { useState, useEffect } from "react";
import { Menu, X, ArrowRight } from "lucide-react";
import { DEMO_URL } from "@/config/demo";
import { trackCtaClick } from "@/lib/analytics";

const navLinks = [
  { label: "Kako funkcioniše", href: "#kako-funkcionise" },
  { label: "Funkcionalnosti", href: "#funkcionalnosti" },
  { label: "Beta testiranje", href: "#beta-testiranje" },
  { label: "FAQ", href: "#faq" },
];

export function Header() {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 12);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled ? "bg-white/95 backdrop-blur-md shadow-sm border-b border-[var(--border-subtle)]" : "bg-transparent"
      }`}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16 md:h-18">
          {/* Logo */}
          <a href="#" className="flex items-center gap-2 group">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-sm"
              style={{ background: "var(--primary)" }}
            >
              Z
            </div>
            <span className="text-lg font-bold" style={{ color: "var(--foreground)" }}>
              Zameni.rs
            </span>
          </a>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-6">
            {navLinks.map((link) => (
              <a
                key={link.href}
                href={link.href}
                className="text-sm transition-colors duration-150 hover:text-primary"
                style={{ color: "var(--muted-foreground)" }}
              >
                {link.label}
              </a>
            ))}
          </nav>

          {/* Desktop CTA */}
          <a
            href={DEMO_URL}
            target="_blank"
            rel="noopener noreferrer"
            onClick={() => trackCtaClick('header_isprobaj_beta')}
            className="hidden md:inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm text-white transition-all duration-150 hover:opacity-90 active:scale-95"
            style={{ background: "var(--primary)" }}
          >
            Isprobaj javnu beta
            <ArrowRight size={15} />
          </a>

          {/* Mobile menu toggle */}
          <button
            className="md:hidden p-2 rounded-lg transition-colors"
            style={{ color: "var(--foreground)" }}
            onClick={() => setOpen(!open)}
            aria-label={open ? "Zatvori meni" : "Otvori meni"}
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {open && (
        <div
          className="md:hidden border-t px-4 py-4 space-y-3"
          style={{ background: "white", borderColor: "var(--border-subtle)" }}
        >
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="block py-2 text-sm"
              style={{ color: "var(--muted-foreground)" }}
              onClick={() => setOpen(false)}
            >
              {link.label}
            </a>
          ))}
          <a
            href={DEMO_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="block w-full text-center px-4 py-2.5 rounded-xl text-sm text-white"
            style={{ background: "var(--primary)" }}
            onClick={() => { trackCtaClick('header_mobile_isprobaj_beta'); setOpen(false); }}
          >
            Isprobaj javnu beta
          </a>
        </div>
      )}
    </header>
  );
}
