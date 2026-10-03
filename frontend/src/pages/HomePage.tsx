import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import './HomePage.css'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <>
      <p className="app-subtitle">
        Signed in as {user?.email}
        <button type="button" className="logout-link" onClick={() => logout()}>
          Log out
        </button>
      </p>

      <section aria-labelledby="menu-heading">
        <h2 id="menu-heading">What would you like to do?</h2>
        <div className="menu-grid">
          <Link className="menu-card" to="/about-yourself">
            <span className="menu-card-title">Tell me about yourself</span>
            <span className="menu-card-desc">
              Rate how into different activities you are
            </span>
          </Link>
          <Link className="menu-card" to="/about-partner">
            <span className="menu-card-title">Tell me about your partner</span>
            <span className="menu-card-desc">
              Rate how into different activities they are
            </span>
          </Link>
          <Link className="menu-card menu-card-primary" to="/recommendations">
            <span className="menu-card-title">
              Show recommended activities by tupla-kupla app
            </span>
            <span className="menu-card-desc">
              See what you two should try, ranked by confidence
            </span>
          </Link>
        </div>
      </section>
    </>
  )
}
