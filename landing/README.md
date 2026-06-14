# Zameni.rs — Landing Page

This is the public landing page for [zameni.rs](https://zameni.rs). Built with React, Vite, TypeScript, and Tailwind CSS.

Original design: https://www.figma.com/design/SLfctAlQgABZckeLmj5b5h/Create-responsive-landing-page

## Running the code

```bash
yarn install --frozen-lockfile
yarn dev      # start development server
yarn build    # create production build
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_DEMO_URL` | No | Override the default demo application URL |
| `VITE_GA_MEASUREMENT_ID` | No | Google Analytics 4 Measurement ID (e.g. `G-7ND43D5L97`) |

Create a `.env` file in the `landing/` directory:

```env
VITE_GA_MEASUREMENT_ID=G-7ND43D5L97
```

## Google Analytics 4

GA4 is loaded **only in production builds** (`yarn build` + serve). During development (`yarn dev`) no tracking scripts are injected.

If `VITE_GA_MEASUREMENT_ID` is not set or is empty, the application works normally with no runtime errors — all analytics calls become no-ops.

### What is tracked

- **Page view** — automatically on initial load
- **CTA clicks** — `cta_click` event with labels:
  - `hero_isprobaj_beta` (Hero section primary button)
  - `header_isprobaj_beta` (Header desktop CTA)
  - `header_mobile_isprobaj_beta` (Header mobile CTA)
  - `beta_section_udji_beta` (Beta section CTA)

### How to verify tracking

1. Build and serve the production bundle:
   ```bash
   yarn build
   npx serve dist
   ```
2. Open the served page in Chrome.
3. Open **Chrome DevTools → Network** tab and filter by `google` or `gtag`.
4. Verify the `gtag/js` script loads and `collect` requests fire on page load and CTA clicks.
5. Alternatively, use [Google Analytics DebugView](https://analytics.google.com/) (Realtime → DebugView) with the [GA Debugger extension](https://chrome.google.com/webstore/detail/google-analytics-debugger/jnkmfdileelhofjcijamephohjechhna).
