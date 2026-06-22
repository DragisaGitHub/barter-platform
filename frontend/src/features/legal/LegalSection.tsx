import { useTranslation } from "react-i18next";

interface LegalSectionProps {
  ns: string;
  sectionKey: string;
}

export function LegalSection({ ns, sectionKey }: LegalSectionProps) {
  const { t } = useTranslation("legal");

  const prefix = `${ns}.sections.${sectionKey}`;
  const heading = t(`${prefix}.heading`);
  const body = t(`${prefix}.body`, { defaultValue: "" });
  const contact = t(`${prefix}.contact`, { defaultValue: "" });

  // Check if items array exists by trying to access the first item
  const firstItem = t(`${prefix}.items.0`, { defaultValue: "" });
  const hasItems = firstItem !== "";

  // Collect all items
  const items: string[] = [];
  if (hasItems) {
    let idx = 0;
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const item = t(`${prefix}.items.${idx}`, { defaultValue: "" });
      if (!item) break;
      items.push(item);
      idx++;
    }
  }

  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-100">
        {heading}
      </h2>
      {body && (
        <p className="mt-2 leading-7 text-slate-600 dark:text-slate-300">
          {body}
        </p>
      )}
      {items.length > 0 && (
        <ul className="mt-3 list-disc space-y-1.5 pl-6 text-slate-600 dark:text-slate-300">
          {items.map((item, idx) => (
            <li key={idx} className="leading-7">{item}</li>
          ))}
        </ul>
      )}
      {contact && (
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
          {contact}
        </p>
      )}
    </section>
  );
}

