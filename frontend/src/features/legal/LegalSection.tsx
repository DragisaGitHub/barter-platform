import { useTranslation } from "react-i18next";

interface LegalSectionProps {
  ns: string;
  sectionKey: string;
}

export function LegalSection({ ns, sectionKey }: LegalSectionProps) {
  const { t } = useTranslation("legal");

  const prefix = `${ns}.sections.${sectionKey}`;
  const heading = t(`${prefix}.heading`);

  // Use a sentinel to detect missing keys (returnEmptyString:false makes defaultValue:"" unreliable)
  const MISSING = "__MISSING__";
  const body = t(`${prefix}.body`, { defaultValue: MISSING }) === MISSING ? "" : t(`${prefix}.body`);
  const contact = t(`${prefix}.contact`, { defaultValue: MISSING }) === MISSING ? "" : t(`${prefix}.contact`);

  // Retrieve items array using returnObjects to avoid infinite loop
  // (returnEmptyString:false causes t(missingKey, {defaultValue:""}) to return the key string)
  const rawItems = t(`${prefix}.items`, { returnObjects: true, defaultValue: [] as unknown as string });
  const items: string[] = Array.isArray(rawItems) ? rawItems : [];

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

