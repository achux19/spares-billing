import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Alert, Box, Button, InputAdornment, Paper, Table, TableBody, TableCell, TableContainer,
  TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { api, errorMessage } from '../../api/client'
import type { EnquirySummary, PageResponse } from '../../api/types'
import { useDebounce } from '../../hooks/useDebounce'
import { fmtDate } from '../../utils/format'
import EnquiryPrintHost from './EnquiryPrintHost'

export default function EnquiriesPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [printId, setPrintId] = useState<number | null>(params.get('print') ? Number(params.get('print')) : null)
  const q = useDebounce(search.trim())

  const list = useQuery({
    queryKey: ['enquiries', q, page, size],
    queryFn: async () =>
      (await api.get<PageResponse<EnquirySummary>>('/enquiries', { params: { q: q || undefined, page, size } })).data,
    placeholderData: keepPreviousData,
  })
  const rows = list.data?.content ?? []

  const remove = useMutation({
    mutationFn: async (id: number) => api.delete(`/enquiries/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['enquiries'] }),
  })

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, gap: 2, flexWrap: 'wrap' }}>
        <Typography variant="h5">Supplier enquiries</Typography>
        <Button variant="contained" onClick={() => navigate('/enquiries/new')}>New enquiry</Button>
      </Box>
      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField size="small" placeholder="Search enquiry number or supplier" value={search} sx={{ minWidth: 300 }}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }} />
      </Paper>

      {list.error && <Alert severity="error" sx={{ mb: 2 }}>{errorMessage(list.error)}</Alert>}
      {remove.error && <Alert severity="error" sx={{ mb: 2 }}>{errorMessage(remove.error)}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Enquiry</TableCell><TableCell>Date</TableCell><TableCell>Supplier</TableCell>
              <TableCell align="right">Items</TableCell><TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell sx={{ fontWeight: 600 }}>{r.enquiryNo}</TableCell>
                <TableCell>{fmtDate(r.enquiryDate)}</TableCell>
                <TableCell>{r.supplierName ?? '-'}</TableCell>
                <TableCell align="right">{r.itemCount}</TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  <Button size="small" onClick={() => setPrintId(r.id)}>Print</Button>
                  <Button size="small" onClick={() => navigate(`/enquiries/${r.id}/edit`)}>Edit</Button>
                  <Button size="small" onClick={() => navigate(`/enquiries/new?copy=${r.id}`)}>Copy</Button>
                  <Button size="small" color="error" disabled={remove.isPending}
                    onClick={() => { if (window.confirm(`Delete ${r.enquiryNo}?`)) remove.mutate(r.id) }}>Delete</Button>
                </TableCell>
              </TableRow>
            ))}
            {!list.isLoading && rows.length === 0 && (
              <TableRow><TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                No enquiries yet. Create one to ask a supplier for prices.
              </TableCell></TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination component="div" count={list.data?.totalElements ?? 0}
          page={page} onPageChange={(_, p) => setPage(p)}
          rowsPerPage={size} onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0) }}
          rowsPerPageOptions={[20, 50, 100]} />
      </TableContainer>

      {printId !== null && (
        <EnquiryPrintHost key={printId} id={printId}
          onDone={() => { setPrintId(null); if (params.get('print')) setParams({}) }} />
      )}
    </>
  )
}
