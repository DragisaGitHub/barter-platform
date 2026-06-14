/**
 * Google Analytics 4 helper module.
 *
 * GA is loaded via the standard gtag.js snippet in index.html.
 * This module only provides typed helpers for custom events —
 * it never injects scripts itself.
 *
 * All helpers gracefully no-op when window.gtag is unavailable
 * (e.g. in dev builds without VITE_GA_MEASUREMENT_ID, or when
 * blocked by an ad-blocker).
 */

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
    dataLayer?: unknown[];
  }
}

/**
 * Track a custom event.
 */
export function trackEvent(
  eventName: string,
  params?: Record<string, string | number | boolean>,
): void {
  window.gtag?.('event', eventName, params);
}

/**
 * Track a page view (useful if SPA routing is added later).
 * NOTE: The initial page_view is already sent by the gtag config in index.html,
 * so only call this for subsequent navigations.
 */
export function trackPageView(path?: string): void {
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

