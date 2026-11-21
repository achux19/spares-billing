import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Alert, Box, Button, Paper, Slider, TextField, Typography } from '@mui/material'
import { api, errorMessage } from '../../api/client'
import type { Branding } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useBranding, useLogo } from '../../hooks/useBranding'

const MAX = 300 * 1024

export default function LogoCard() {
  const qc = useQueryClient()
  const { isOwner } = useAuth()
  const { data } = useBranding()
  const logo = useLogo()
  const fileRef = useRef<HTMLInputElement>(null)
  const [height, setHeight] = useState(18)
  const [width, setWidth] = useState(60)
  useEffect(() => {
    if (data) { setHeight(data.logoHeightMm); setWidth(data.logoWidthMm) }
  }, [data?.logoHeightMm, data?.logoWidthMm]) // eslint-disable-line react-hooks/exhaustive-deps
  const [msg, setMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const done = (b: Branding, text: string) => { qc.setQueryData(['branding'], b); setMsg({ type: 'success', text }) }
  const fail = (e: unknown) => setMsg({ type: 'error', text: errorMessage(e) })

  const upload = useMutation({
    mutationFn: async (file: File) => {
      const body = new FormData()
      body.append('file', file)
      return (await api.post<Branding>('/branding/logo', body, { headers: { 'Content-Type': 'multipart/form-data' } })).data
    },
    onSuccess: (b) => done(b, 'Logo saved. It now prints on every invoice, quotation, credit note and enquiry.'),
    onError: fail,
  })
  const size = useMutation({
    mutationFn: async () => (await api.put<Branding>('/branding/logo-size', { heightMm: height, widthMm: width })).data,
    onSuccess: (b) => done(b, 'Logo size saved'),
    onError: fail,
  })
  const clear = useMutation({
    mutationFn: async () => (await api.delete<Branding>('/branding/logo')).data,
    onSuccess: (b) => done(b, 'Logo removed'),
    onError: fail,
  })

  const pick = (file: File | undefined) => {
    if (!file) return
    if (file.size > MAX) { setMsg({ type: 'error', text: 'Logo must be 300 KB or smaller' }); return }
    setMsg(null)
    upload.mutate(file)
    if (fileRef.current) fileRef.current.value = ''
  }

  return (
    <Paper sx={{ p: 2, mb: 2, display: 'grid', gap: 2, gridColumn: '1 / -1' }}>
      <Typography variant="h6">Print logo</Typography>
      <Typography variant="body2" color="text.secondary">
        Printed at the top of every invoice, bill, quotation, credit note and supplier enquiry, including the PDF.
        Use a PNG, JPG or WEBP picture, up to 300 KB. A wide picture about 600 x 200 pixels on a white or transparent
        background looks best.
      </Typography>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <Box sx={{ minWidth: 160, height: 80, border: 1, borderColor: 'divider', borderRadius: 1, display: 'grid', placeItems: 'center', p: 1 }}>
          {logo
            ? <img src={logo} alt="logo" style={{ maxWidth: 200, maxHeight: 70 }} />
            : <Typography variant="caption" color="text.secondary">No logo</Typography>}
        </Box>
        {isOwner && (
          <>
            <input ref={fileRef} type="file" hidden accept="image/png,image/jpeg,image/webp"
              onChange={(e) => pick(e.target.files?.[0])} />
            <Button variant="outlined" disabled={upload.isPending} onClick={() => fileRef.current?.click()}>
              {data?.hasLogo ? 'Change logo' : 'Upload logo'}
            </Button>
            {data?.hasLogo && <Button color="inherit" disabled={clear.isPending} onClick={() => clear.mutate()}>Remove logo</Button>}
          </>
        )}
      </Box>
      {logo && isOwner && (
        <Box sx={{ display: 'grid', gap: 1 }}>
          <Typography variant="subtitle2">Largest size on printed pages: height {height} mm, width {width} mm</Typography>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
            <Typography variant="body2" sx={{ width: 60 }}>Height</Typography>
            <Slider min={8} max={50} step={1} value={height} onChange={(_, v) => { setHeight(v as number); setMsg(null) }}
              sx={{ maxWidth: 260 }} />
            <TextField size="small" type="number" value={height} sx={{ width: 100 }}
              onChange={(e) => setHeight(Math.min(50, Math.max(8, Math.round(Number(e.target.value) || 8))))}
              slotProps={{ htmlInput: { min: 8, max: 50 }, input: { endAdornment: <Typography variant="caption">mm</Typography> } }} />
          </Box>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
            <Typography variant="body2" sx={{ width: 60 }}>Width</Typography>
            <Slider min={15} max={120} step={1} value={width} onChange={(_, v) => { setWidth(v as number); setMsg(null) }}
              sx={{ maxWidth: 260 }} />
            <TextField size="small" type="number" value={width} sx={{ width: 100 }}
              onChange={(e) => setWidth(Math.min(120, Math.max(15, Math.round(Number(e.target.value) || 15))))}
              slotProps={{ htmlInput: { min: 15, max: 120 }, input: { endAdornment: <Typography variant="caption">mm</Typography> } }} />
            <Button variant="contained" disabled={(height === data?.logoHeightMm && width === data?.logoWidthMm) || size.isPending}
              onClick={() => size.mutate()}>Save size</Button>
          </Box>
          <Typography variant="caption" color="text.secondary">
            The picture keeps its proportions and is fitted inside this box (made bigger or smaller as needed), so it is never stretched. Preview is at print size.
            Bills on the small receipt printer keep a fixed small logo.
          </Typography>
          <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1, display: 'inline-block', alignSelf: 'start' }}>
            <Box sx={{ width: `${width}mm`, height: `${height}mm`, maxWidth: '100%' }}>
              <img src={logo} alt="preview" style={{ width: '100%', height: '100%', objectFit: 'contain', objectPosition: 'left top', display: 'block' }} />
            </Box>
          </Box>
        </Box>
      )}
      {msg && <Alert severity={msg.type}>{msg.text}</Alert>}
    </Paper>
  )
}
