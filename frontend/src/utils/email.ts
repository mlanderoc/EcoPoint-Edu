/**
 * Email format: something, an @, a domain and an ending of two or more
 * letters, the same pattern as UserValidator in the backend (professor's
 * review, point 2).
 *
 * @author craulgomez
 */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/

export const EMAIL_MESSAGE = 'El correo debe tener @ y un dominio, por ejemplo usuario@unicesar.edu.co.'

/** Message for an invalid email, or undefined when it is valid. */
export function emailError(email: string): string | undefined {
  const value = email.trim()
  if (!value) return 'El correo electrónico es obligatorio.'
  if (!value.includes('@')) return 'Falta el @ en el correo.'
  return EMAIL_PATTERN.test(value) ? undefined : EMAIL_MESSAGE
}
