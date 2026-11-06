import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Chip, CircularProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { api, errorMessage } from '../../api/client'
import { fmtDate } from '../../utils/format'
import { when, type AdminShop } from './adminTypes'
import { CreateShopDialog, EditShopDialog, FeaturesDialog, ShopUsersDialog } from './AdminDialogs'

export default function AdminShopsPage() {
  const qc = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<AdminShop | null>(null)
  const [features, setFeatures] = useState<AdminShop | null>(null)
  const [usersOf, setUsersOf] = useState<AdminShop | null>(null)
  const [error, setError] = useState('')

  const shops = useQuery({
    queryKey: ['admin-shops'],
    queryFn: async () => (await api.get<AdminShop[]>('/admin/shops')).data,
  })

  const toggle = useMutation({
    mutationFn: (s: AdminShop) => api.post(`/admin/shops/${s.id}/${s.status === 'ACTIVE' ? 'suspend' : 'activate'}`),
    onSuccess: () => { setError(''); qc.invalidateQueries({ queryKey: ['admin-shops'] }) },
    onError: (e) => setError(errorMessage(e)),
  })

  const confirmToggle = (s: AdminShop) => {
    const msg = s.status === 'ACTIVE'
      ? `Suspend ${s.name}? Nobody in this shop will be able to log in until you activate it again. Their data is kept.`
      : `Activate ${s.name}?`
    if (window.confirm(msg)) toggle.mutate(s)
  }

  const list = shops.data ?? []

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>Shops ({list.length})</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>New shop</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {shops.isLoading && <CircularProgress />}
      {shops.error && <Alert severity="error">{errorMessage(shops.error)}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Shop</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Users</TableCell>
              <TableCell align="right">Bills</TableCell>
              <TableCell>Last bill</TableCell>
              <TableCell>Last login</TableCell>
              <TableCell>Modules</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {list.map((s) => (
              <TableRow key={s.id} hover>
                <TableCell>
                  <Typography sx={{ fontWeight: 600 }}>{s.name}</Typography>
                  <Typography variant="caption" color="text.secondary">code: {s.code}</Typography>
                </TableCell>
                <TableCell>
                  <Chip size="small" label={s.status === 'ACTIVE' ? 'Active' : 'Suspended'}
                    color={s.status === 'ACTIVE' ? 'success' : 'error'} />
                </TableCell>
                <TableCell align="right">{s.activeUserCount} / {s.userCount}</TableCell>
                <TableCell align="right">{s.invoiceCount}</TableCell>
                <TableCell>{s.lastInvoiceDate ? fmtDate(s.lastInvoiceDate) : '—'}</TableCell>
                <TableCell>{when(s.lastLoginAt)}</TableCell>
                <TableCell sx={{ maxWidth: 260 }}>
                  {s.features.length === 0
                    ? <Typography variant="caption" color="text.secondary">none</Typography>
                    : s.features.map((k) => <Chip key={k} size="small" label={k} sx={{ mr: 0.5, mb: 0.5 }} />)}
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  <Button size="small" onClick={() => setFeatures(s)}>Modules</Button>
                  <Button size="small" onClick={() => setUsersOf(s)}>Users</Button>
                  <Button size="small" onClick={() => setEditing(s)}>Edit</Button>
                  <Button size="small" color={s.status === 'ACTIVE' ? 'error' : 'success'}
                    disabled={toggle.isPending} onClick={() => confirmToggle(s)}>
                    {s.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
        You see counts only. A shop's bills, customers and products are never shown here.
      </Typography>

      {creating && <CreateShopDialog onClose={() => setCreating(false)} />}
      {editing && <EditShopDialog shop={editing} onClose={() => setEditing(null)} />}
      {features && <FeaturesDialog shop={features} onClose={() => setFeatures(null)} />}
      {usersOf && <ShopUsersDialog shop={usersOf} onClose={() => setUsersOf(null)} />}
    </>
  )
}
