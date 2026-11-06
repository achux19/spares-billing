import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { useFeatures } from '../hooks/useFeatures'

export default function ProtectedRoute({ ownerOnly = false, feature }: { ownerOnly?: boolean; feature?: string }) {
  const { user, isOwner, isSuperAdmin } = useAuth()
  const { has, isLoading } = useFeatures()
  if (!user) return <Navigate to="/login" replace />
  if (isSuperAdmin) return <Navigate to="/admin" replace />      // the platform admin has no shop screens
  if (ownerOnly && !isOwner) return <Navigate to="/" replace />
  if (feature) {
    if (isLoading) return null
    if (!has(feature)) return <Navigate to="/" replace />
  }
  return <Outlet />
}
