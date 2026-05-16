import { useId } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/auth/AuthContext";
import {
  mapBackendToFrontendLanguage,
  type FrontendLanguage,
} from "@/i18n/languageMapping";
import { useUserPreferences } from "./useUserPreferences";

export function LanguageSwitcher() {
  const languageSelectId = useId();
  const { user, isAuthenticated } = useAuth();
  const { t } = useTranslation(["common"]);
  const updatePreferences = useUserPreferences();

  if (!isAuthenticated || !user) {
    return null;
  }

  const currentLanguage = mapBackendToFrontendLanguage(user.preferredLanguage);

  const handleChange = async (event: React.ChangeEvent<HTMLSelectElement>) => {
    const nextLanguage = event.target.value as FrontendLanguage;

    if (nextLanguage === currentLanguage || updatePreferences.isPending) {
      return;
    }

    await updatePreferences.mutateAsync(nextLanguage);
  };

  return (
    <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 dark:border-slate-800 dark:bg-slate-900">
      <label
        htmlFor={languageSelectId}
        className="text-xs font-medium text-slate-500 dark:text-slate-400"
      >
        {t("common:language")}
      </label>
      <select
        id={languageSelectId}
        value={currentLanguage}
        onChange={(event) => {
          void handleChange(event);
        }}
        disabled={updatePreferences.isPending}
        aria-label={t("common:language")}
        className="min-w-24 bg-transparent text-sm font-medium text-slate-700 outline-none disabled:cursor-not-allowed disabled:opacity-60 dark:text-slate-200"
      >
        <option value="sr">{t("common:srpski")}</option>
        <option value="en">{t("common:english")}</option>
      </select>
    </div>
  );
}

