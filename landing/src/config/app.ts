const DEFAULT_APP_URL = 'https://app.zameni.rs'

const configuredAppUrl = import.meta.env.VITE_APP_URL?.trim()

export const APP_URL = configuredAppUrl && configuredAppUrl.length > 0
  ? configuredAppUrl
  : DEFAULT_APP_URL
