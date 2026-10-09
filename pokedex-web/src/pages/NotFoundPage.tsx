import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="empty">
      <h1>Page not found</h1>
      <p>This page wandered into the tall grass.</p>
      <Link to="/" className="btn btn--primary">Back to the Pokédex</Link>
    </section>
  )
}
