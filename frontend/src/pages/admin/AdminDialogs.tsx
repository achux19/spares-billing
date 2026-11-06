import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Box, Button, Checkbox, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel,
  Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import { api, errorMessage } from '../../api/client'
import { when, type AdminShop, type AdminShopUser, type FeatureInfo } from './adminTypes'

const refreshKeys = ['admin-shops']

function useRefresh() {
  const qc = useQueryClient()
  return () => { for (const k of refreshKeys) qc.invalidateQueries({ queryKey: [k] }) }
}

function useFeatureCatalog() {
  return useQuery({
    queryKey: ['admin-features'],
    staleTime: Infinity,
    queryFn: async () => (await api.get<FeatureInfo[]>('/admin/features')).data,
  })
}

function FeatureChecks({ catalog, value, onChange }: {
  catalog: FeatureInfo[]
  value: string[]
  onChange: (v: string[]) => void
}) {
  const toggle = (key: string) =>
    onChange(value.includes(key) ? value.filter((k) => k !== key) : [...value, key])
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column' }}>
      {catalog.map((f) => (
        <FormControlLabel key={f.key} label={f.label}
          control={<Checkbox checked={value.includes(f.key)} onChange={() => toggle(f.key)} />} />
      ))}
    </Box>
  )
}

// ---------------------------------------------------------------- create

export function CreateShopDialog({ onClose }: { onClose: () => void }) {
  const refresh = useRefresh()
  const catalog = useFeatureCatalog()
  const [f, setF] = useState({
    name: '', code: '', stateCode: '33', ownerFullName: '', ownerUsername: 'owner', ownerPassword: '',
  })
  const [features, setFeatures] = useState<string[] | null>(null)
  const [error, setError] = useState('')
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value })

  const chosen = features ?? (catalog.data ?? []).map((x) => x.key)   // new shops start with everything on

  const save = useMutation({
    mutationFn: () => api.post('/admin/shops', {
      ...f, code: f.code.trim().toLowerCase(), name: f.name.trim(), features: chosen,
    }),
    onSuccess: () => { refresh(); onClose() },
    onError: (e) => setError(errorMessage(e)),
  })

  const ready = f.name.trim() && f.code.trim() && f.stateCode && f.ownerFullName.trim()
    && f.ownerUsername.trim() && f.ownerPassword.length >= 8

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>New shop</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Stack spacing={1.5} sx={{ mt: 1 }}>
          <TextField label="Shop name" value={f.name} onChange={set('name')} fullWidth autoFocus />
          <Stack direction="row" spacing={1.5}>
            <TextField label="Shop code (used to log in)" value={f.code} onChange={set('code')} fullWidth
              helperText="Small letters, digits and hyphen, for example sri-murugan" />
            <TextField label="State code" value={f.stateCode} onChange={set('stateCode')} sx={{ width: 140 }}
              helperText="33 = Tamil Nadu" />
          </Stack>
          <Typography variant="subtitle2" sx={{ pt: 1 }}>First owner login</Typography>
          <TextField label="Owner's full name" value={f.ownerFullName} onChange={set('ownerFullName')} fullWidth />
          <Stack direction="row" spacing={1.5}>
            <TextField label="Username" value={f.ownerUsername} onChange={set('ownerUsername')} fullWidth />
            <TextField label="Temporary password" value={f.ownerPassword} onChange={set('ownerPassword')} fullWidth
              helperText="At least 8 characters. The owner must change it at first login." />
          </Stack>
          <Typography variant="subtitle2" sx={{ pt: 1 }}>Modules for this shop</Typography>
          <FeatureChecks catalog={catalog.data ?? []} value={chosen} onChange={setFeatures} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!ready || save.isPending} onClick={() => save.mutate()}>
          {save.isPending ? 'Creating…' : 'Create shop'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ---------------------------------------------------------------- edit name / code

export function EditShopDialog({ shop, onClose }: { shop: AdminShop; onClose: () => void }) {
  const refresh = useRefresh()
  const [name, setName] = useState(shop.name)
  const [code, setCode] = useState(shop.code)
  const [error, setError] = useState('')
  const save = useMutation({
    mutationFn: () => api.put(`/admin/shops/${shop.id}`, { name: name.trim(), code: code.trim().toLowerCase() }),
    onSuccess: () => { refresh(); onClose() },
    onError: (e) => setError(errorMessage(e)),
  })
  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Edit shop</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Stack spacing={1.5} sx={{ mt: 1 }}>
          <TextField label="Shop name" value={name} onChange={(e) => setName(e.target.value)} fullWidth autoFocus />
          <TextField label="Shop code" value={code} onChange={(e) => setCode(e.target.value)} fullWidth />
          {code.trim().toLowerCase() !== shop.code && (
            <Alert severity="warning">
              Changing the code changes how this shop's users log in. Tell them the new code.
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!name.trim() || !code.trim() || save.isPending}
          onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  )
}

// ---------------------------------------------------------------- feature switches

export function FeaturesDialog({ shop, onClose }: { shop: AdminShop; onClose: () => void }) {
  const refresh = useRefresh()
  const catalog = useFeatureCatalog()
  const [value, setValue] = useState<string[]>(shop.features)
  const [error, setError] = useState('')
  const save = useMutation({
    mutationFn: () => api.put(`/admin/shops/${shop.id}/features`, { features: value }),
    onSuccess: () => { refresh(); onClose() },
    onError: (e) => setError(errorMessage(e)),
  })
  return (
    <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Modules for {shop.name}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          Switched-off modules disappear from the shop's menu and are refused by the server.
          The shop's data is kept.
        </Typography>
        <FeatureChecks catalog={catalog.data ?? []} value={value} onChange={setValue} />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  )
}

// ---------------------------------------------------------------- users + password reset

export function ShopUsersDialog({ shop, onClose }: { shop: AdminShop; onClose: () => void }) {
  const users = useQuery({
    queryKey: ['admin-shop-users', shop.id],
    queryFn: async () => (await api.get<AdminShopUser[]>(`/admin/shops/${shop.id}/users`)).data,
  })
  const [target, setTarget] = useState<AdminShopUser | null>(null)
  const [pw, setPw] = useState('')
  const [error, setError] = useState('')
  const [done, setDone] = useState('')

  const reset = useMutation({
    mutationFn: () => api.post(`/admin/shops/${shop.id}/users/${target!.id}/reset-password`, { newPassword: pw }),
    onSuccess: () => {
      setDone(`Password for ${target?.username} reset. They must choose a new one at next login.`)
      setTarget(null)
      setPw('')
      setError('')
    },
    onError: (e) => setError(errorMessage(e)),
  })

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Users of {shop.name} (code: {shop.code})</DialogTitle>
      <DialogContent>
        {done && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setDone('')}>{done}</Alert>}
        {users.error && <Alert severity="error">{errorMessage(users.error)}</Alert>}
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell><TableCell>Name</TableCell><TableCell>Role</TableCell>
              <TableCell>Status</TableCell><TableCell>Last login</TableCell><TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {(users.data ?? []).map((u) => (
              <TableRow key={u.id}>
                <TableCell>{u.username}</TableCell>
                <TableCell>{u.fullName}</TableCell>
                <TableCell>{u.role}</TableCell>
                <TableCell>
                  <Chip size="small" label={u.active ? 'Active' : 'Inactive'} color={u.active ? 'success' : 'default'} />
                </TableCell>
                <TableCell>{when(u.lastLoginAt)}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => { setTarget(u); setDone(''); setError('') }}>Reset password</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {target && (
          <Box sx={{ mt: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}>
            {error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}
            <Typography variant="subtitle2" sx={{ mb: 1 }}>New temporary password for {target.username}</Typography>
            <Stack direction="row" spacing={1}>
              <TextField size="small" value={pw} onChange={(e) => setPw(e.target.value)} fullWidth
                helperText="At least 8 characters" />
              <Button variant="contained" disabled={pw.length < 8 || reset.isPending}
                onClick={() => reset.mutate()}>Reset</Button>
              <Button onClick={() => { setTarget(null); setPw('') }}>Cancel</Button>
            </Stack>
          </Box>
        )}
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Close</Button></DialogActions>
    </Dialog>
  )
}
