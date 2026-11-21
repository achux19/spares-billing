import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Branding } from '../api/types'

export function useBranding() {
  return useQuery({
    queryKey: ['branding'],
    staleTime: 5 * 60 * 1000,
    queryFn: async () => (await api.get<Branding>('/branding')).data,
  })
}

const blobToDataUrl = (blob: Blob) =>
  new Promise<string>((resolve, reject) => {
    const r = new FileReader()
    r.onload = () => resolve(String(r.result))
    r.onerror = () => reject(r.error)
    r.readAsDataURL(blob)
  })

// Images are fetched with the login token (a plain <img src> carries no token, so the server would not know
// which shop's picture to send) and kept in memory as data URLs.
function useBrandImage(kind: 'logo' | 'favicon', has: boolean | undefined, version: number | undefined): string | null {
  const q = useQuery({
    queryKey: [kind, version],
    enabled: !!has,
    staleTime: Infinity,
    queryFn: async () => blobToDataUrl((await api.get<Blob>(`/branding/${kind}`, { responseType: 'blob' })).data),
  })
  return has ? q.data ?? null : null
}

/** The shop's print logo as a data URL (null when none). Kept in memory so printing never waits for it. */
export function useLogo(): string | null {
  const { data: b } = useBranding()
  return useBrandImage('logo', b?.hasLogo, b?.version)
}

/** The shop's browser-tab / menu icon as a data URL (null when none). */
export function useFavicon(): string | null {
  const { data: b } = useBranding()
  return useBrandImage('favicon', b?.hasFavicon, b?.version)
}
