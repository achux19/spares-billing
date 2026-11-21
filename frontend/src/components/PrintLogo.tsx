import { useBranding, useLogo } from '../hooks/useBranding'

/** Shop logo for the top of a printed page. Draws nothing when the shop has not uploaded one. */
export default function PrintLogo({ height, align = 'center' }: { height?: number; align?: 'center' | 'left' }) {
  const logo = useLogo()
  const { data } = useBranding()
  if (!logo) return null
  const mm = height ?? data?.logoHeightMm ?? 18
  const wmm = height === undefined ? data?.logoWidthMm ?? 60 : undefined   // receipt keeps its small fixed logo
  if (wmm) {
    // A4 pages: a box of the chosen size; the picture fits inside it without being stretched
    return (
      <div style={{ marginBottom: 4 }}>
        <div style={{ width: `${wmm}mm`, height: `${mm}mm`, marginLeft: align === 'center' ? 'auto' : 0, marginRight: align === 'center' ? 'auto' : 0 }}>
          <img src={logo} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain', objectPosition: align === 'center' ? 'center' : 'left top' }} />
        </div>
      </div>
    )
  }
  return (
    <div style={{ textAlign: align, marginBottom: 4 }}>
      <img src={logo} alt="" style={{ maxHeight: `${mm}mm`, maxWidth: wmm ? `${wmm}mm` : '60%', objectFit: 'contain' }} />
    </div>
  )
}
