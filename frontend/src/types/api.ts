/**
 * Standard API error and response interfaces as defined in Spec 003.
 *
 * @author craulgomez
 */

export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  errorCode: string
  message: string
  path: string
  fieldErrors?: FieldError[]
}

export interface ApiResponse<T> {
  data: T
  status: number
}
