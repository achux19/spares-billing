export interface AdminShop {
  id: number
  code: string
  name: string
  status: 'ACTIVE' | 'SUSPENDED'
  createdAt: string
  features: string[]
  userCount: number
  activeUserCount: number
  invoiceCount: number
  lastInvoiceDate: string | null
  lastLoginAt: string | null
}

export interface AdminShopUser {
  id: number
  username: string
  fullName: string
  role: string
  active: boolean
  lastLoginAt: string | null
}

export interface FeatureInfo {
  key: string
  label: string
}

export const when = (iso: string | null) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—'
