/**
 * Google Analytics 4 helper module.
 *
 * - Loads GA only in production builds.
 * - Gracefully no-ops when the measurement ID is missing.
 * - Provides typed helpers so components never call gtag directly.
 */

const GA_MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID?.trim() || '';

/** Whether GA is active (production build + measurement ID configured) */
const isEnabled = import.meta.env.PROD && GA_MEASUREMENT_ID.length > 0;

/**
 * Injects the GA4 script tags into the document head.
 * Safe to call multiple times — will only inject once.
 */
export function initGA(): void {
  if (!isEnabled) return;
  if (document.getElementById('ga-script')) return;

  // Global gtag dataLayer
  window.dataLayer = window.dataLayer || [];
  window.gtag = function gtag(...args: unknown[]) {
    // eslint-disable-next-line prefer-rest-params
    (window.dataLayer as unknown[]).push(args);
  };
  window.gtag('js', new Date());
  window.gtag('config', GA_MEASUREMENT_ID, { send_page_view: true });

  // Async script loader
  const script = document.createElement('script');
  script.id = 'ga-script';
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`;
  document.head.appendChild(script);
}

/**
 * Track a custom event.
 */
export function trackEvent(
  eventName: string,
  params?: Record<string, string | number | boolean>,
): void {
  if (!isEnabled) return;
  window.gtag?.('event', eventName, params);
}

/**
 * Track a page view (useful if SPA routing is added later).
 */
export function trackPageView(path?: string): void {
  if (!isEnabled) return;
  window.gtag?.('event', 'page_view', {
    page_path: path || window.location.pathname,
  });
}

/**
 * Track CTA click leading to the demo application.
 */
export function trackCtaClick(label: string): void {
  trackEvent('cta_click', {
    event_category: 'engagement',
    event_label: label,
  });
}

