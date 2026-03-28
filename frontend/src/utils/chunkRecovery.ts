const CHUNK_RELOAD_ONCE_KEY = 'mrs_chunk_reload_once'
const CHUNK_REDIRECT_ONCE_KEY = 'mrs_chunk_redirect_once'

const CHUNK_ERROR_PATTERNS = [
  'failed to fetch dynamically imported module',
  'importing a module script failed',
  'dynamically imported module',
  'chunkloaderror',
  'loading css chunk',
]

function toErrorText(error: unknown): string {
  if (typeof error === 'string') return error
  if (error instanceof Error) return `${error.name}: ${error.message}`
  try {
    return JSON.stringify(error)
  } catch {
    return String(error ?? '')
  }
}

function hasSessionStorage(): boolean {
  return typeof window !== 'undefined' && !!window.sessionStorage
}

export function isChunkLoadError(error: unknown): boolean {
  const message = toErrorText(error).toLowerCase()
  return CHUNK_ERROR_PATTERNS.some((pattern) => message.includes(pattern))
}

export function tryReloadForChunkError(targetPath?: string): boolean {
  if (typeof window === 'undefined') return false
  if (!hasSessionStorage()) return false

  if (window.sessionStorage.getItem(CHUNK_RELOAD_ONCE_KEY) === '1') {
    return false
  }

  window.sessionStorage.setItem(CHUNK_RELOAD_ONCE_KEY, '1')
  const fallbackTarget = `${window.location.pathname}${window.location.search}${window.location.hash}`
  const next = targetPath || fallbackTarget
  window.location.replace(next)
  return true
}

export function tryRedirectToLoginOnce(): boolean {
  if (typeof window === 'undefined') return false
  if (!hasSessionStorage()) return false

  if (window.sessionStorage.getItem(CHUNK_REDIRECT_ONCE_KEY) === '1') {
    return false
  }

  window.sessionStorage.setItem(CHUNK_REDIRECT_ONCE_KEY, '1')
  window.location.replace('/login?force=1&recover=chunk')
  return true
}

export function clearChunkRecoveryMarkers() {
  if (!hasSessionStorage()) return
  window.sessionStorage.removeItem(CHUNK_RELOAD_ONCE_KEY)
  window.sessionStorage.removeItem(CHUNK_REDIRECT_ONCE_KEY)
}
