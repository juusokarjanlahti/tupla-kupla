import { Outlet } from 'react-router-dom'
import '../App.css'

export function AppShell() {
  return (
    <main className="app-shell">
      <header className="app-header">
        <h1>Tupla Kupla</h1>
      </header>
      <Outlet />
    </main>
  )
}
