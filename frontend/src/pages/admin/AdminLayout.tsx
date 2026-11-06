import { Outlet } from 'react-router-dom'
import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material'
import { useAuth } from '../../auth/AuthContext'
import PasswordGate from '../../components/PasswordGate'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50' }}>
      <AppBar position="sticky" color="inherit" elevation={0}
        sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 800, color: 'primary.main', flexGrow: 1 }}>
            Platform admin
          </Typography>
          <Typography variant="body2" color="text.secondary">{user?.username}</Typography>
          <Button onClick={logout} sx={{ textTransform: 'none' }}>Log out</Button>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ p: { xs: 2, md: 3 }, maxWidth: 1400, mx: 'auto' }}>
        <Outlet />
      </Box>
      <PasswordGate />
    </Box>
  )
}
