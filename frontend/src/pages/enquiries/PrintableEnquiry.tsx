import type { EnquiryFull, ShopSettings } from '../../api/types'
import { fmtDate } from '../../utils/format'

/** Supplier enquiry: only part number, description and quantity. No prices. */
export default function PrintableEnquiry({ e, shop }: { e: EnquiryFull; shop: ShopSettings | undefined }) {
  return (
    <div className="a4" style={{ fontSize: 12 }}>
      <style>{'@page { size: A4 portrait; margin: 12mm; }'}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700 }}>{shop?.shopName}</div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{shop?.address}{shop?.pincode ? ` - ${shop.pincode}` : ''}</div>
          <div>{[shop?.phone && `Ph: ${shop.phone}`, shop?.gstin && `GSTIN: ${shop.gstin}`].filter(Boolean).join('   |   ')}</div>
        </div>
        <div style={{ border: '2px solid #000', padding: '6px 12px', textAlign: 'center', minWidth: 200 }}>
          <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: 1 }}>QUOTATION REQUEST</div>
          <div style={{ fontSize: 12, fontWeight: 700 }}>{e.enquiryNo}</div>
          <div>Date: {fmtDate(e.enquiryDate)}</div>
        </div>
      </div>

      <div style={{ marginTop: 10 }}>
        <b>To:</b> {e.supplierName || '__________________________'}
      </div>
      <div style={{ marginTop: 6 }}>
        Please send your best price and availability for the parts listed below.
      </div>

      <table style={{ marginTop: 10 }}>
        <thead>
          <tr><th style={{ width: 40 }}>Sr</th><th style={{ width: 160 }}>Part number</th><th>Description</th><th style={{ width: 90 }}>Quantity</th></tr>
        </thead>
        <tbody>
          {e.items.map((l, i) => (
            <tr key={i}>
              <td className="center">{i + 1}</td>
              <td>{l.partNumber}</td>
              <td>{l.description}</td>
              <td className="num">{l.quantity} {l.unit}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {e.notes && <div style={{ marginTop: 10, whiteSpace: 'pre-wrap' }}><b>Note:</b> {e.notes}</div>}
    </div>
  )
}
