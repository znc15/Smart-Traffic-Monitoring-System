export const ADMIN_ROLE_ID = 0
export const USER_ROLE_ID = 1

export function normalizeRoleId(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseInt(value.trim(), 10)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }

  return USER_ROLE_ID
}

export function isAdminRole(value: unknown): boolean {
  return normalizeRoleId(value) === ADMIN_ROLE_ID
}
