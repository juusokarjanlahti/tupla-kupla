import { useAuth } from '../auth/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <main>
      <h1>tupla-kupla</h1>
      <p>Signed in as {user?.email}</p>
      <button type="button" onClick={() => logout()}>
        Log out
      </button>
    </main>
  )
}
