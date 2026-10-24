/** The path prefix the app is served under, without a trailing slash: '' locally, '/tracker' behind the gateway. */
export const BASE_PATH = import.meta.env.BASE_URL.replace(/\/$/, '')

/** The site's home page, when the app runs under a prefix next to other apps on the same domain. */
export const HOME_URL = BASE_PATH ? '/' : null

/**
 * True on hosts that put the backend to sleep when idle (Render's free tier, VITE_SERVER_SLEEPS=true).
 * Only then does a gateway error mean "starting up". Locally it means the backend isn't running.
 */
export const SERVER_SLEEPS = import.meta.env.VITE_SERVER_SLEEPS === 'true'
