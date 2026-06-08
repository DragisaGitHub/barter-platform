const DEFAULT_DEMO_URL = 'https://barter-platform-dev.duckdns.org/'

const configuredDemoUrl = import.meta.env.VITE_DEMO_URL?.trim()

export const DEMO_URL = configuredDemoUrl && configuredDemoUrl.length > 0
  ? configuredDemoUrl
  : DEFAULT_DEMO_URL

