import { describe, expect, it } from 'vitest'
import { ADMIN_ROLE_ID, USER_ROLE_ID, isAdminRole, normalizeRoleId } from './roles'

describe('lib/roles', () => {
  it('should treat role id 0 as admin across number and string inputs', () => {
    expect(normalizeRoleId(ADMIN_ROLE_ID)).toBe(ADMIN_ROLE_ID)
    expect(normalizeRoleId('0')).toBe(ADMIN_ROLE_ID)
    expect(isAdminRole(ADMIN_ROLE_ID)).toBe(true)
    expect(isAdminRole('0')).toBe(true)
  })

  it('should fall back to regular user for blank or invalid role ids', () => {
    expect(normalizeRoleId(undefined)).toBe(USER_ROLE_ID)
    expect(normalizeRoleId('')).toBe(USER_ROLE_ID)
    expect(normalizeRoleId('not-a-role')).toBe(USER_ROLE_ID)
    expect(isAdminRole(USER_ROLE_ID)).toBe(false)
  })
})
