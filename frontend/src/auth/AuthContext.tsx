import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { useNavigate } from 'react-router-dom'

export interface AuthUser {
  id: number
  email: string
}

interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// Must stay under the backend's access token TTL (app.jwt.expiration-ms, 15 min by
// default) so this fires before the cookie expires rather than after.
const SILENT_REFRESH_INTERVAL_MS = 14 * 60 * 1000

async function parseErrorMessage(
  response: Response,
  fallback: string,
): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? fallback
  } catch {
    return fallback
  }
}

async function refreshAccessToken(): Promise<AuthUser | null> {
  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
    return response.ok ? ((await response.json()) as AuthUser) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false

    fetch('/api/auth/me', { credentials: 'include' })
      .then((response) =>
        response.ok ? (response.json() as Promise<AuthUser>) : refreshAccessToken(),
      )
      .then((currentUser) => {
        if (!cancelled) setUser(currentUser)
      })
      .catch(() => {
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  // Keeps the session alive past the access token's 15-minute lifetime for as long as
  // the tab stays open, by rotating the refresh token in the background. If the refresh
  // token has also expired (or reuse was detected server-side), this signs the user out.
  useEffect(() => {
    if (!user) return

    const interval = setInterval(async () => {
      const refreshedUser = await refreshAccessToken()
      setUser(refreshedUser)
      if (!refreshedUser) navigate('/login', { replace: true })
    }, SILENT_REFRESH_INTERVAL_MS)

    return () => clearInterval(interval)
  }, [user, navigate])

  async function login(email: string, password: string) {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password }),
    })
    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, 'Invalid email or password'),
      )
    }
    setUser((await response.json()) as AuthUser)
  }

  async function register(email: string, password: string) {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password }),
    })
    if (!response.ok) {
      throw new Error(await parseErrorMessage(response, 'Registration failed'))
    }
    setUser((await response.json()) as AuthUser)
  }

  async function logout() {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
    setUser(null)
    navigate('/login', { replace: true })
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
