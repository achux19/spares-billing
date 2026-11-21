import type { ShopSettings } from '../api/types'
import { useBranding, useLogo } from '../hooks/useBranding'

/**
 * Top of an A4 invoice / credit note: the document title in the middle, then the shop's details.
 * With a logo: logo on the left, shop name and address on the right. Without one: all centred, as before.
 */
export default function A4Header({ title, shop }: { title: string; shop: ShopSettings }) {
  const logo = useLogo()
  const { data } = useBranding()
  const extra = [shop.phone && `Ph: ${shop.phone}`, shop.gstin && `GSTIN: ${shop.gstin}`,
    shop.stateName && `State: ${shop.stateName}`].filter(Boolean).join('   |   ')

  return (
    <>
      <div className="center" style={{ fontSize: 18, fontWeight: 700 }}>{title}</div>
      {logo ? (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginTop: 6 }}>
          <div style={{ width: `${data?.logoWidthMm ?? 60}mm`, height: `${data?.logoHeightMm ?? 18}mm`, flexShrink: 0 }}>
            <img src={logo} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain', objectPosition: 'left top' }} />
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 16, fontWeight: 700 }}>{shop.shopName}</div>
            <div style={{ whiteSpace: 'pre-line' }}>{shop.address}</div>
            <div>{extra}</div>
          </div>
        </div>
      ) : (
        <>
          <div className="center" style={{ fontSize: 16, fontWeight: 700, marginTop: 4 }}>{shop.shopName}</div>
          <div className="center">{shop.address}</div>
          <div className="center">{extra}</div>
        </>
      )}
    </>
  )
}
