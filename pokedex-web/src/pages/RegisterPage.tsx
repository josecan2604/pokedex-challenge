import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { authApi } from '../api/pokedexApi'
import { Field } from '../components/Field'
import { Notice } from '../components/Status'
import { validateRegistration, type RegisterForm } from '../utils/validation'

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterForm>({ username: '', email: '', password: '' })
  const [errors, setErrors] = useState<Partial<Record<keyof RegisterForm, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const change = (field: keyof RegisterForm) => (value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const validation = validateRegistration(form)
    setErrors(validation)
    setFormError(null)
    if (Object.keys(validation).length > 0) {
      return
    }
    setSubmitting(true)
    try {
      const user = await authApi.register(form.username.trim(), form.email.trim(), form.password)
      navigate('/login', { state: { registered: user.username } })
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) {
        setErrors(e.fieldErrors)
      } else {
        setFormError(e instanceof Error ? e.message : 'Registration failed.')
      }
      setSubmitting(false)
    }
  }

  return (
    <section className="auth">
      <h1>Create an account</h1>
      <form className="form panel" onSubmit={handleSubmit} noValidate>
        <Field label="Username" error={errors.username} hint="3-30 letters, digits, '_', '.' or '-'">
          {(control) => <input {...control} autoComplete="username" value={form.username} onChange={(e) => change('username')(e.target.value)} />}
        </Field>
        <Field label="Email" error={errors.email}>
          {(control) => <input {...control} type="email" autoComplete="email" value={form.email} onChange={(e) => change('email')(e.target.value)} />}
        </Field>
        <Field label="Password" error={errors.password} hint="At least 8 characters, with a letter and a digit">
          {(control) => <input {...control} type="password" autoComplete="new-password" value={form.password} onChange={(e) => change('password')(e.target.value)} />}
        </Field>
        {formError && <Notice kind="error">{formError}</Notice>}
        <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p>Already have an account? <Link to="/login">Sign in</Link></p>
    </section>
  )
}
