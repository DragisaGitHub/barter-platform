import { useTranslation } from "react-i18next";
import { LegalPageLayout } from "./LegalPageLayout";
import { LegalSection } from "./LegalSection";

export function CommunityGuidelinesPage() {
  const { t } = useTranslation("legal");

  const sections = [
    "intro", "respectful", "honest", "safe", "prohibited", "reporting", "enforcement",
  ] as const;

  return (
    <LegalPageLayout title={t("communityGuidelines.title")}>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {t("betaNotice")}
      </p>
      {sections.map((key) => (
        <LegalSection key={key} ns="communityGuidelines" sectionKey={key} />
      ))}
    </LegalPageLayout>
  );
}
