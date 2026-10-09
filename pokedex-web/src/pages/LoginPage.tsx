import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Field } from '../components/Field'
import { Notice } from '../components/Status'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string; registered?: string } | null)?.from ?? '/collection'
  const registered = (location.state as { registered?: string } | null)?.registered

  const [username, setUsername] = useState(registered ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (session) {
    return <Navigate to={from} replace />
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    if (!username.trim() || !password) {
      setError('Enter your username and password.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await login(username.trim(), password)
      navigate(from, { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Sign in failed.')
      setSubmitting(false)
    }
  }

  return (
    <section className="auth">
      <h1>Sign in</h1>
      {registered && <Notice kind="success">Account created. Sign in to continue.</Notice>}
      <form className="form panel" onSubmit={handleSubmit} noValidate>
        <Field label="Username">
          {(control) => <input {...control} autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />}
        </Field>
        <Field label="Password">
          {(control) => <input {...control} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />}
        </Field>
        {error && <Notice kind="error">{error}</Notice>}
        <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="muted demo-hint">
          Demo accounts: <code>ash / Pikachu123</code> (trainer) · <code>admin / Admin1234</code> (admin)
        </p>
      </form>
      <p>New here? <Link to="/register">Create an account</Link></p>
    </section>
  )
}
