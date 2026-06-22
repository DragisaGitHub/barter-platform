import { useTranslation } from "react-i18next";
import { LegalPageLayout } from "./LegalPageLayout";
import { LegalSection } from "./LegalSection";

export function SafetyTipsPage() {
  const { t } = useTranslation("legal");

  const sections = [
    "intro", "meeting", "checking", "payments", "reporting", "communication",
  ] as const;

  return (
    <LegalPageLayout title={t("safetyTips.title")}>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {t("betaNotice")}
      </p>
      {sections.map((key) => (
        <LegalSection key={key} ns="safetyTips" sectionKey={key} />
      ))}
    </LegalPageLayout>
  );
}
