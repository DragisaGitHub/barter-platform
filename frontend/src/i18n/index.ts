import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import {
  DEFAULT_LANGUAGE,
  FALLBACK_LANGUAGE,
  SUPPORTED_LANGUAGES,
} from "./languageMapping";

import srCommon from "./locales/sr/common.json";
import enCommon from "./locales/en/common.json";
import srAuth from "./locales/sr/auth.json";
import enAuth from "./locales/en/auth.json";
import srNavigation from "./locales/sr/navigation.json";
import enNavigation from "./locales/en/navigation.json";
import srCatalog from "./locales/sr/catalog.json";
import enCatalog from "./locales/en/catalog.json";
import srTrade from "./locales/sr/trade.json";
import enTrade from "./locales/en/trade.json";
import srProfile from "./locales/sr/profile.json";
import enProfile from "./locales/en/profile.json";
import srNotifications from "./locales/sr/notifications.json";
import enNotifications from "./locales/en/notifications.json";
import srAdmin from "./locales/sr/admin.json";
import enAdmin from "./locales/en/admin.json";
import srReporting from "./locales/sr/reporting.json";
import enReporting from "./locales/en/reporting.json";
import srErrors from "./locales/sr/errors.json";
import enErrors from "./locales/en/errors.json";
import srDashboard from "./locales/sr/dashboard.json";
import enDashboard from "./locales/en/dashboard.json";
import srLanding from "./locales/sr/landing.json";
import enLanding from "./locales/en/landing.json";
import srFeedback from "./locales/sr/feedback.json";
import enFeedback from "./locales/en/feedback.json";

export const translationResources = {
  sr: {
    common: srCommon,
    auth: srAuth,
    navigation: srNavigation,
    catalog: srCatalog,
    trade: srTrade,
    profile: srProfile,
    notifications: srNotifications,
    admin: srAdmin,
    reporting: srReporting,
    errors: srErrors,
    dashboard: srDashboard,
    landing: srLanding,
    feedback: srFeedback,
  },
  en: {
    common: enCommon,
    auth: enAuth,
    navigation: enNavigation,
    catalog: enCatalog,
    trade: enTrade,
    profile: enProfile,
    notifications: enNotifications,
    admin: enAdmin,
    reporting: enReporting,
    errors: enErrors,
    dashboard: enDashboard,
    landing: enLanding,
    feedback: enFeedback,
  },
} as const;

void i18n.use(initReactI18next).init({
  resources: translationResources,
  lng: DEFAULT_LANGUAGE,
  fallbackLng: FALLBACK_LANGUAGE,
  supportedLngs: SUPPORTED_LANGUAGES,
  defaultNS: "common",
  ns: [
    "common",
    "auth",
    "navigation",
    "catalog",
    "trade",
    "profile",
    "notifications",
    "admin",
    "reporting",
    "errors",
    "dashboard",
    "landing",
    "feedback",
  ],
  interpolation: {
    escapeValue: false,
  },
  returnEmptyString: false,
});

export default i18n;

