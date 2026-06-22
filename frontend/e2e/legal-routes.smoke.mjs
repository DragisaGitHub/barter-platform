/**
 * Legal Routes Smoke Test
 *
 * Verifies that all legal pages render real content (no blank/spinner state).
 * Run: node e2e/legal-routes.smoke.mjs
 * Requires: Vite dev server running on localhost:5173
 */
import { chromium } from 'playwright';

const BASE = process.env.BASE_URL || 'http://localhost:5173';
const LEGAL_ROUTES = [
  { path: '/terms', expectedText: 'termsOfService' },
  { path: '/privacy', expectedText: 'privacyPolicy' },
  { path: '/community-guidelines', expectedText: 'communityGuidelines' },
  { path: '/prohibited-items', expectedText: 'prohibitedItems' },
  { path: '/safety-tips', expectedText: 'safetyTips' },
];

let exitCode = 0;

(async () => {
  // Wait for server to be ready
  for (let i = 0; i < 15; i++) {
    try {
      await fetch(`${BASE}/`);
      break;
    } catch {
      if (i === 14) {
        console.error(`❌ Server not reachable at ${BASE}`);
        process.exit(1);
      }
      await new Promise(r => setTimeout(r, 1000));
    }
  }

  const browser = await chromium.launch({ headless: true });

  for (const { path } of LEGAL_ROUTES) {
    const page = await browser.newPage();
    const jsErrors = [];
    page.on('pageerror', err => jsErrors.push(err.message));

    try {
      await page.goto(`${BASE}${path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await new Promise(r => setTimeout(r, 5000));

      const rootHTML = await page.evaluate(() => document.getElementById('root')?.innerHTML || '');
      const hasSpinner = rootHTML.includes('animate-spin') || rootHTML.includes('Loading...');
      const isEmpty = rootHTML.length < 100;
      // Legal pages should contain a heading, nav bar, and section content
      const hasContent = rootHTML.includes('font-bold') && rootHTML.length > 1000;

      if (isEmpty) {
        console.error(`❌ ${path} — BLANK (root HTML length: ${rootHTML.length})`);
        exitCode = 1;
      } else if (hasSpinner) {
        console.error(`❌ ${path} — SPINNER state (not rendering content)`);
        exitCode = 1;
      } else if (!hasContent) {
        console.error(`❌ ${path} — No legal content detected (HTML: ${rootHTML.length} chars)`);
        exitCode = 1;
      } else {
        console.log(`✅ ${path} — OK (${rootHTML.length} chars, real content)`);
      }

      if (jsErrors.length > 0) {
        console.error(`   JS errors: ${jsErrors.join('; ')}`);
        exitCode = 1;
      }
    } catch (e) {
      console.error(`❌ ${path} — TIMEOUT/ERROR: ${e.message}`);
      exitCode = 1;
    }

    await page.close();
  }

  await browser.close();
  console.log(exitCode === 0 ? '\n✅ All legal routes pass' : '\n❌ Some routes failed');
  process.exit(exitCode);
})();

