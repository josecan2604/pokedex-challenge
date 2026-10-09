import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { LocalPokemon } from '../api/types'
import { useUpdateLocalPokemon } from '../hooks/useLocalPokemon'
import {
  LIMITS,
  parseTags,
  toCustomFields,
  validateCustomFields,
  type CustomFieldsForm,
  type FieldErrors,
} from '../utils/validation'
import { Field } from './Field'
import { Notice } from './Status'

export function LocalPokemonForm({ pokemon, onReload }: { pokemon: LocalPokemon; onReload: () => void }) {
  const [form, setForm] = useState<CustomFieldsForm>(() => ({
    localizedName: pokemon.localizedName ?? '',
    region: pokemon.region ?? '',
    tags: pokemon.tags.join(', '),
    notes: pokemon.notes ?? '',
  }))
  const [clientErrors, setClientErrors] = useState<FieldErrors>({})
  const update = useUpdateLocalPokemon(pokemon.id)

  const serverError = update.error instanceof ApiError ? update.error : null
  const fieldErrors: FieldErrors = { ...(serverError?.fieldErrors as FieldErrors | undefined), ...clientErrors }
  const isConflict = serverError?.status === 409

  const change = (field: keyof CustomFieldsForm) => (value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
    if (clientErrors[field]) {
      setClientErrors((current) => ({ ...current, [field]: undefined }))
    }
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const errors = validateCustomFields(form)
    setClientErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }
    update.mutate({ fields: toCustomFields(form), version: pokemon.version })
  }

  const previewTags = parseTags(form.tags)

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <div className="form__grid">
        <Field label="Localized name" hint="e.g. the Japanese or Spanish name" error={fieldErrors.localizedName}>
          {(control) => (
            <input {...control} value={form.localizedName} maxLength={LIMITS.localizedName}
              onChange={(e) => change('localizedName')(e.target.value)} />
          )}
        </Field>
        <Field label="Region" hint="Where it can be found" error={fieldErrors.region}>
          {(control) => (
            <input {...control} value={form.region} maxLength={LIMITS.region}
              onChange={(e) => change('region')(e.target.value)} />
          )}
        </Field>
      </div>

      <Field label="Tags" hint={`Comma separated, up to ${LIMITS.tags}. Letters, numbers and dashes.`} error={fieldErrors.tags}>
        {(control) => (
          <input {...control} value={form.tags} placeholder="starter, fan-favorite"
            onChange={(e) => change('tags')(e.target.value)} />
        )}
      </Field>
      {previewTags.length > 0 && (
        <ul className="tag-list" aria-label="Tag preview">
          {previewTags.map((tag) => <li key={tag} className="tag">{tag}</li>)}
        </ul>
      )}

      <Field label="Notes" hint={`${form.notes.trim().length}/${LIMITS.notes}`} error={fieldErrors.notes}>
        {(control) => (
          <textarea {...control} rows={4} value={form.notes}
            onChange={(e) => change('notes')(e.target.value)} />
        )}
      </Field>

      <div aria-live="polite">
        {update.isSuccess && <Notice kind="success">Saved.</Notice>}
        {isConflict && (
          <Notice kind="error">
            Someone else changed this Pokémon while you were editing.{' '}
            <button type="button" className="btn btn--link" onClick={() => { update.reset(); onReload() }}>
              Load the latest version
            </button>
          </Notice>
        )}
        {serverError && !isConflict && Object.keys(serverError.fieldErrors).length === 0 && (
          <Notice kind="error">{serverError.message}</Notice>
        )}
      </div>

      <div className="form__actions">
        <button type="submit" className="btn btn--primary" disabled={update.isPending || isConflict}>
          {update.isPending ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </form>
  )
}
