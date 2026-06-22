import { useTranslation } from "react-i18next";
import { LegalPageLayout } from "./LegalPageLayout";
import { LegalSection } from "./LegalSection";

export function PrivacyPolicyPage() {
  const { t } = useTranslation("legal");

  const sections = [
    "intro", "dataCollected", "howWeUse", "sharing", "retention", "rights", "cookies",
  ] as const;

  return (
    <LegalPageLayout title={t("privacyPolicy.title")}>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {t("betaNotice")}
      </p>
      {sections.map((key) => (
        <LegalSection key={key} ns="privacyPolicy" sectionKey={key} />
      ))}
    </LegalPageLayout>
  );
}
