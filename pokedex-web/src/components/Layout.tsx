import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function Layout() {
  const { session, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="app">
      <a className="skip-link" href="#main">Skip to content</a>
      <header className="header">
        <div className="header__inner">
          <Link to="/" className="brand" aria-label="Pokedex home">
            <img src="/pokeball.svg" alt="" width={28} height={28} />
            <span>Pokedex</span>
          </Link>
          <nav className="nav" aria-label="Main">
            <NavLink to="/" end className="nav__link">Browse</NavLink>
            <NavLink to="/collection" className="nav__link">My Pokedex</NavLink>
          </nav>
          <div className="account">
            {session ? (
              <>
                <span className="account__user" title={isAdmin ? 'Administrator' : 'Trainer'}>
                  {session.username}
                  <span className={`role role--${isAdmin ? 'admin' : 'user'}`}>{isAdmin ? 'Admin' : 'Trainer'}</span>
                </span>
                <button type="button" className="btn btn--ghost" onClick={handleLogout}>Sign out</button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn--ghost">Sign in</Link>
                <Link to="/register" className="btn btn--primary">Register</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main id="main" className="main">
        <Outlet />
      </main>
      <footer className="footer">
        Data from <a href="https://pokeapi.co" target="_blank" rel="noreferrer">PokeAPI</a>. Pokémon and Pokémon character names are trademarks of Nintendo.
      </footer>
    </div>
  )
}
