/** The path prefix the app is served under, without a trailing slash: '' locally, '/tracker' behind the gateway. */
export const BASE_PATH = import.meta.env.BASE_URL.replace(/\/$/, '')

/** The site's home page, when the app runs under a prefix next to other apps on the same domain. */
export const HOME_URL = BASE_PATH ? '/' : null
