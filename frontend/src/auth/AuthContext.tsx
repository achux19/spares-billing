import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, tokenStore, setUnauthorizedHandler } from '../api/client'

export interface User {
  username: string
  role: 'SUPER_ADMIN' | 'OWNER' | 'STAFF'
  shopCode?: string   // empty for the platform Super Admin
  shopName?: string
}

interface AuthState {
  user: User | null
  login: (shopCode: string, username: string, password: string) => Promise<void>
  logout: () => void
  isOwner: boolean
  isSuperAdmin: boolean
}

const USER_KEY = 'spares_user'
const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(() => {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw && tokenStore.get() ? (JSON.parse(raw) as User) : null
    } catch {
      return null
    }
  })

  const logout = () => {
    tokenStore.clear()
    localStorage.removeItem(USER_KEY)
    queryClient.clear()   // never keep one shop's screens in memory for the next login
    setUser(null)
  }

  useEffect(() => {
    setUnauthorizedHandler(logout)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (shopCode: string, username: string, password: string) => {
    const { data } = await api.post('/auth/login', { shopCode, username, password })
    const u: User = {
      username: data.username,
      role: data.role,
      shopCode: data.shopCode ?? undefined,
      shopName: data.shopName ?? undefined,
    }
    queryClient.clear()
    tokenStore.set(data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
  }

  return (
    <AuthContext.Provider value={{
      user, login, logout,
      isOwner: user?.role === 'OWNER',
      isSuperAdmin: user?.role === 'SUPER_ADMIN',
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}