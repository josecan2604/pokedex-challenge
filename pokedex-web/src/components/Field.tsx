import { useId, type ReactNode } from 'react'

export interface ControlProps {
  id: string
  'aria-invalid': boolean
  'aria-describedby': string | undefined
}

interface FieldProps {
  label: string
  error?: string
  hint?: string
  /** Renders the control with the ids and aria attributes that tie it to its label, hint and error. */
  children: (control: ControlProps) => ReactNode
}

export function Field({ label, error, hint, children }: FieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined

  return (
    <div className={`field${error ? ' field--invalid' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children({ id, 'aria-invalid': Boolean(error), 'aria-describedby': describedBy })}
      {hint && <p id={hintId} className="field__hint">{hint}</p>}
      {error && <p id={errorId} className="field__error">{error}</p>}
    </div>
  )
}
