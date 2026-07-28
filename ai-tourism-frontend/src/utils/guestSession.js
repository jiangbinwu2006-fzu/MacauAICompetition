import { createGuestSession } from './api.js'

const STORAGE_KEYS = {
  token: 'guest_token',
  sessionId: 'guest_session_id',
  userId: 'guest_user_id',
  expiresAt: 'guest_expires_at'
}

let pendingSession = null

export function clearGuestSession() {
  Object.values(STORAGE_KEYS).forEach(key => sessionStorage.removeItem(key))
}

export function getGuestSession() {
  const token = sessionStorage.getItem(STORAGE_KEYS.token)
  const sessionId = sessionStorage.getItem(STORAGE_KEYS.sessionId)
  const userId = sessionStorage.getItem(STORAGE_KEYS.userId)
  const expiresAt = Number(sessionStorage.getItem(STORAGE_KEYS.expiresAt) || 0)
  if (!token || !sessionId || !userId || expiresAt <= Date.now()) {
    clearGuestSession()
    return null
  }
  return { token, sessionId, userId, expiresAt, guest: true }
}

function storeGuestSession(session) {
  sessionStorage.setItem(STORAGE_KEYS.token, session.token)
  sessionStorage.setItem(STORAGE_KEYS.sessionId, session.session_id)
  sessionStorage.setItem(STORAGE_KEYS.userId, session.user.user_id)
  sessionStorage.setItem(STORAGE_KEYS.expiresAt, String(session.expires_at))
  return getGuestSession()
}

export async function ensureGuestSession() {
  if (localStorage.getItem('token')) return null
  const existing = getGuestSession()
  if (existing) return existing
  if (!pendingSession) {
    pendingSession = createGuestSession()
      .then(storeGuestSession)
      .finally(() => { pendingSession = null })
  }
  return pendingSession
}
