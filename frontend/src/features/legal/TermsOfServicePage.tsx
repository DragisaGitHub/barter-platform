import { useTranslation } from "react-i18next";
import { LegalPageLayout } from "./LegalPageLayout";
import { LegalSection } from "./LegalSection";

export function TermsOfServicePage() {
  const { t } = useTranslation("legal");

  const sections = [
    "about", "acceptance", "eligibility", "accounts",
    "userResponsibilities", "platformRole", "content", "termination", "changes",
  ] as const;

  return (
    <LegalPageLayout title={t("termsOfService.title")}>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        {t("betaNotice")}
      </p>
      {sections.map((key) => (
        <LegalSection key={key} ns="termsOfService" sectionKey={key} />
      ))}
    </LegalPageLayout>
  );
}
