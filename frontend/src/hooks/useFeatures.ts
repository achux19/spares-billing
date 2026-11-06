import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** Which modules the logged-in shop has (switched on by the platform admin). */
export function useFeatures() {
  const { user } = useAuth()
  const q = useQuery({
    queryKey: ['features'],
    enabled: !!user && user.role !== 'SUPER_ADMIN',
    staleTime: 60_000,
    queryFn: async () => (await api.get<string[]>('/features')).data,
  })
  // no key = always allowed; unknown or still loading = not allowed yet
  const has = (key?: string) => !key || (q.data?.includes(key) ?? false)
  return { has, isLoading: q.isLoading }
}
