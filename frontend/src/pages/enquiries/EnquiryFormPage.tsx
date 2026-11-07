import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, IconButton, MenuItem, Paper, Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Typography,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import { api, errorMessage } from '../../api/client'
import type { EnquiryFull, Product, Supplier } from '../../api/types'
import { todayIST } from '../../utils/dates'
import ProductSearch from '../billing/ProductSearch'
import SupplierPicker from '../purchases/SupplierPicker'
import { RowMover, moveItem, useRowReorder } from '../../components/RowReorder'

interface ELine { key: string; productId: number | null; partNumber: string; description: string; qty: string; unit: string }

const newKey = () => Math.random().toString(36).slice(2)
const fromFull = (e: EnquiryFull): ELine[] =>
  e.items.map((i) => ({
    key: newKey(), productId: i.productId, partNumber: i.partNumber ?? '', description: i.description,
    qty: String(i.quantity), unit: i.unit,
  }))
// suggested order quantity for a low-stock part: back up to twice the alert level
const suggestQty = (p: Product) => Math.max(p.minStock * 2 - p.stockQty, 1)

export default function EnquiryFormPage() {
  const { id } = useParams()
  const [search] = useSearchParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const editId = id ? Number(id) : null
  const copyId = search.get('copy') ? Number(search.get('copy')) : null
  const sourceId = editId ?? copyId

  const [supplier, setSupplier] = useState<Supplier | null>(null)
  const [supplierName, setSupplierName] = useState('')
  const [date, setDate] = useState(todayIST())
  const [notes, setNotes] = useState('')
  const [lines, setLines] = useState<ELine[]>([])
  const [error, setError] = useState('')
  const [touched, setTouched] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  const qtyRefs = useRef<Record<string, HTMLInputElement | null>>({})

  const existing = useQuery({
    queryKey: ['enquiry', sourceId],
    enabled: sourceId !== null,
    queryFn: async () => (await api.get<EnquiryFull>(`/enquiries/${sourceId}`)).data,
  })
  useEffect(() => {
    const e = existing.data
    if (!e) return
    setSupplier(e.supplierId ? { id: e.supplierId, name: e.supplierName ?? '' } as Supplier : null)
    setSupplierName(e.supplierId ? '' : e.supplierName ?? '')
    setNotes(e.notes ?? '')
    setLines(fromFull(e))
    setDate(editId !== null ? e.enquiryDate : todayIST())
  }, [existing.data]) // eslint-disable-line react-hooks/exhaustive-deps

  const reorder = useRowReorder((from, to) => setLines((prev) => moveItem(prev, from, to)))
  const update = (key: string, patch: Partial<ELine>) =>
    setLines((prev) => prev.map((l) => (l.key === key ? { ...l, ...patch } : l)))

  const addProduct = (p: Product) => {
    setError('')
    if (lines.some((l) => l.productId === p.id)) {
      setError(`${p.partNumber} is already in the list.`)
      return
    }
    const line: ELine = {
      key: newKey(), productId: p.id, partNumber: p.partNumber, description: p.name,
      qty: String(Math.max(suggestQty(p), 1)), unit: p.unit ?? 'NOS',
    }
    setLines((prev) => [...prev, line])
    setTimeout(() => { const el = qtyRefs.current[line.key]; el?.focus(); el?.select() }, 60)
  }
  const addCustom = () =>
    setLines((prev) => [...prev, { key: newKey(), productId: null, partNumber: '', description: '', qty: '1', unit: 'NOS' }])

  const addLowStock = async () => {
    setError('')
    try {
      const low = (await api.get<Product[]>('/products/low-stock')).data
      const have = new Set(lines.map((l) => l.productId))
      const fresh = low.filter((p) => !have.has(p.id))
      if (fresh.length === 0) { setError(low.length === 0 ? 'No low-stock parts right now.' : 'All low-stock parts are already in the list.'); return }
      setLines((prev) => [...prev, ...fresh.map((p) => ({
        key: newKey(), productId: p.id, partNumber: p.partNumber, description: p.name,
        qty: String(suggestQty(p)), unit: p.unit ?? 'NOS',
      }))])
    } catch (e) { setError(errorMessage(e)) }
  }

  const badQty = (l: ELine) => !Number.isInteger(Number(l.qty)) || Number(l.qty) < 1
  const invalid = lines.length === 0 || lines.some((l) => badQty(l) || !l.description.trim())

  const save = useMutation({
    mutationFn: async () => {
      const body = {
        enquiryDate: date,
        supplierId: supplier?.id ?? null,
        supplierName: supplier ? null : supplierName.trim() || null,
        notes: notes.trim() || null,
        items: lines.map((l) => ({
          productId: l.productId, partNumber: l.partNumber.trim() || null, description: l.description.trim(),
          quantity: Number(l.qty), unit: l.unit,
        })),
      }
      return (editId !== null ? await api.put<EnquiryFull>(`/enquiries/${editId}`, body)
        : await api.post<EnquiryFull>('/enquiries', body)).data
    },
    onError: (e) => setError(errorMessage(e)),
  })
  const submit = (print: boolean) => {
    setTouched(true)
    setError('')
    if (invalid) { setError('Add at least one item, with a description and a whole-number quantity.'); return }
    save.mutate(undefined, {
      onSuccess: (saved) => {
        qc.invalidateQueries({ queryKey: ['enquiries'] })
        qc.invalidateQueries({ queryKey: ['enquiry', saved.id] })
        navigate(print ? `/enquiries?print=${saved.id}` : '/enquiries')
      },
    })
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        {editId !== null ? `Edit ${existing.data?.enquiryNo ?? 'enquiry'}` : copyId !== null ? 'New enquiry (copy)' : 'New supplier enquiry'}
      </Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {existing.error && <Alert severity="error" sx={{ mb: 2 }}>{errorMessage(existing.error)}</Alert>}

      <Paper sx={{ p: 2, mb: 2, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 2fr 1fr' }, gap: 2 }}>
        <SupplierPicker value={supplier} onChange={(s) => { setSupplier(s); if (s) setSupplierName('') }} label="Supplier (optional)" />
        <TextField label="Or type a supplier name" value={supplierName} disabled={!!supplier}
          onChange={(e) => setSupplierName(e.target.value)} />
        <TextField label="Date" type="date" value={date} onChange={(e) => setDate(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }} />
      </Paper>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <Box sx={{ flexGrow: 1, minWidth: 300 }}>
          <ProductSearch onPick={addProduct} inputRef={searchRef} />
        </Box>
        <Button variant="outlined" onClick={addLowStock}>Add all low-stock parts</Button>
        <Button variant="outlined" onClick={addCustom}>Add custom line</Button>
      </Paper>

      <Paper sx={{ mb: 2, overflow: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 100 }}>Move</TableCell><TableCell>#</TableCell>
              <TableCell sx={{ minWidth: 150 }}>Part number</TableCell><TableCell sx={{ minWidth: 240 }}>Description</TableCell>
              <TableCell align="right" sx={{ width: 110 }}>Quantity</TableCell><TableCell sx={{ width: 100 }}>Unit</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {lines.map((l, i) => (
              <TableRow key={l.key} {...reorder.rowProps(i)}>
                <TableCell sx={{ px: 0.5 }}>
                  <RowMover index={i} count={lines.length} handleProps={reorder.handleProps(i)} onMove={reorder.onMove} />
                </TableCell>
                <TableCell>{i + 1}</TableCell>
                <TableCell>
                  <TextField size="small" fullWidth value={l.partNumber} onChange={(e) => update(l.key, { partNumber: e.target.value })} />
                </TableCell>
                <TableCell>
                  <TextField size="small" fullWidth value={l.description} error={touched && !l.description.trim()}
                    onChange={(e) => update(l.key, { description: e.target.value })} />
                </TableCell>
                <TableCell align="right">
                  <TextField size="small" type="number" value={l.qty} error={badQty(l)}
                    inputRef={(el) => { qtyRefs.current[l.key] = el }}
                    onChange={(e) => update(l.key, { qty: e.target.value })}
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); searchRef.current?.focus() } }}
                    slotProps={{ htmlInput: { min: 1, style: { textAlign: 'right' } } }} />
                </TableCell>
                <TableCell>
                  <TextField size="small" select value={l.unit} onChange={(e) => update(l.key, { unit: e.target.value })}>
                    <MenuItem value="NOS">NOS</MenuItem>
                    <MenuItem value="LTR">LTR</MenuItem>
                  </TextField>
                </TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => setLines((p) => p.filter((x) => x.key !== l.key))}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {lines.length === 0 && (
              <TableRow><TableCell colSpan={7} align="center" sx={{ py: 5, color: 'text.secondary' }}>
                No items yet. Search for a part above, or add all low-stock parts.
              </TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField label="Note to supplier (printed at the bottom)" multiline minRows={2} fullWidth value={notes}
          onChange={(e) => setNotes(e.target.value)} />
      </Paper>

      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
        <Button onClick={() => navigate('/enquiries')}>Cancel</Button>
        <Button variant="outlined" disabled={save.isPending} onClick={() => submit(false)}>Save</Button>
        <Button variant="contained" disabled={save.isPending} onClick={() => submit(true)}>Save and print</Button>
      </Box>
    </>
  )
}
