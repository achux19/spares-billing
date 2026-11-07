import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { EnquiryFull, ShopSettings } from '../../api/types'
import { pdfName, printAs } from '../../utils/printTitle'
import PrintableEnquiry from './PrintableEnquiry'

/** Loads one enquiry, opens the print dialog once, then calls onDone. */
export default function EnquiryPrintHost({ id, onDone }: { id: number; onDone: () => void }) {
  const enq = useQuery({
    queryKey: ['enquiry', id],
    queryFn: async () => (await api.get<EnquiryFull>(`/enquiries/${id}`)).data,
  })
  const shop = useQuery({
    queryKey: ['shop-print'],
    queryFn: async () => (await api.get<ShopSettings>('/shop')).data,
  })
  const ready = !!enq.data && !!shop.data

  useEffect(() => {
    if (!ready || !enq.data) return
    const t = setTimeout(() => {
      printAs(pdfName(enq.data!.enquiryNo, enq.data!.enquiryDate, enq.data!.supplierName))
      onDone()
    }, 200)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready])

  useEffect(() => { if (enq.error) onDone() }, [enq.error]) // eslint-disable-line react-hooks/exhaustive-deps

  return ready && enq.data
    ? createPortal(<div id="print-root"><PrintableEnquiry e={enq.data} shop={shop.data} /></div>, document.body)
    : null
}
