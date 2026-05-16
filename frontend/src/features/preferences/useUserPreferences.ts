import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { updateCurrentUserPreferences } from "@/api/userPreferencesApi";
import type { FrontendLanguage } from "@/i18n/languageMapping";
import { mapFrontendToBackendLanguage } from "@/i18n/languageMapping";
import { useAuth } from "@/auth/AuthContext";
import { useTranslation } from "react-i18next";

export function useUserPreferences() {
  const { user, replaceUser } = useAuth();
  const { t } = useTranslation(["common"]);

  return useMutation({
    mutationFn: async (language: FrontendLanguage) => {
      const response = await updateCurrentUserPreferences({
        preferredLanguage: mapFrontendToBackendLanguage(language),
      });

      return { language, response };
    },
    onSuccess: async ({ response }) => {
      if (user) {
        await replaceUser({
          ...user,
          preferredLanguage: response.preferredLanguage,
        });
      }

      toast.success(t("common:languageChanged"));
    },
  });
}

