import type { CustomFields } from '../api/types'

/**
 * Client-side copy of the domain rules in LocalPokemon.java, for instant feedback.
 * The server stays the source of truth: its 400 field errors are shown the same way.
 */
export const LIMITS = {
  localizedName: 100,
  region: 50,
  tags: 10,
  tagLength: 30,
  notes: 500,
} as const

const TAG_PATTERN = /^[a-z0-9][a-z0-9-]{0,29}$/

export interface CustomFieldsForm {
  localizedName: string
  region: string
  tags: string
  notes: string
}

export type FieldErrors = Partial<Record<keyof CustomFieldsForm, string>>

/** "Starter, gen-1, starter" -> ["starter", "gen-1"] */
export function parseTags(input: string): string[] {
  const tags = input
    .split(',')
    .map((tag) => tag.trim().toLowerCase())
    .filter((tag) => tag.length > 0)
  return [...new Set(tags)]
}

export function validateCustomFields(form: CustomFieldsForm): FieldErrors {
  const errors: FieldErrors = {}
  if (form.localizedName.trim().length > LIMITS.localizedName) {
    errors.localizedName = `Must be at most ${LIMITS.localizedName} characters`
  }
  if (form.region.trim().length > LIMITS.region) {
    errors.region = `Must be at most ${LIMITS.region} characters`
  }
  const tags = parseTags(form.tags)
  const invalid = tags.find((tag) => !TAG_PATTERN.test(tag))
  if (invalid) {
    errors.tags = `"${invalid}" must be 1-${LIMITS.tagLength} characters of a-z, 0-9 or "-"`
  } else if (tags.length > LIMITS.tags) {
    errors.tags = `At most ${LIMITS.tags} tags`
  }
  if (form.notes.trim().length > LIMITS.notes) {
    errors.notes = `Must be at most ${LIMITS.notes} characters`
  }
  return errors
}

export function toCustomFields(form: CustomFieldsForm): CustomFields {
  const orNull = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    localizedName: orNull(form.localizedName),
    region: orNull(form.region),
    tags: parseTags(form.tags),
    notes: orNull(form.notes),
  }
}

export interface RegisterForm {
  username: string
  email: string
  password: string
}

export function validateRegistration(form: RegisterForm): Partial<Record<keyof RegisterForm, string>> {
  const errors: Partial<Record<keyof RegisterForm, string>> = {}
  if (!/^[a-zA-Z0-9_.-]{3,30}$/.test(form.username.trim())) {
    errors.username = "3-30 characters: letters, digits, '_', '.' or '-'"
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
    errors.email = 'Enter a valid email address'
  }
  if (form.password.length < 8 || form.password.length > 72) {
    errors.password = 'Between 8 and 72 characters'
  } else if (!/[a-zA-Z]/.test(form.password) || !/\d/.test(form.password)) {
    errors.password = 'Include at least one letter and one digit'
  }
  return errors
}
