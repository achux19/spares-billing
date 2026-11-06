import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function AdminRoute() {
  const { user, isSuperAdmin } = useAuth()
  if (!user) return <Navigate to="/admin/login" replace />
  if (!isSuperAdmin) return <Navigate to="/" replace />
  return <Outlet />
}
