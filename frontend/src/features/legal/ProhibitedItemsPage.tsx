import { useTranslation } from "react-i18next";
import { LegalPageLayout } from "./LegalPageLayout";
import { LegalSection } from "./LegalSection";

export function ProhibitedItemsPage() {
  const { t } = useTranslation("legal");

  const sections = ["intro", "categories", "reporting", "consequences"] as const;

  return (
    <LegalPageLayout title={t("prohibitedItems.title")}>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {t("betaNotice")}
      </p>
      {sections.map((key) => (
        <LegalSection key={key} ns="prohibitedItems" sectionKey={key} />
      ))}
    </LegalPageLayout>
  );
}
