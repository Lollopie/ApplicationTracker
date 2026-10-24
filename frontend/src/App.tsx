import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { ApplicationDetailPage } from './pages/ApplicationDetailPage'
import { ApplicationsPage } from './pages/ApplicationsPage'
import { EditApplicationPage } from './pages/EditApplicationPage'
import { LoginPage } from './pages/LoginPage'
import { NewApplicationPage } from './pages/NewApplicationPage'
import { RegisterPage } from './pages/RegisterPage'

export function App() {
  return (
    // Render serves these paths through rewrites listed one by one in render.yaml: add new top-level routes there too.
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route path="/applications/new" element={<NewApplicationPage />} />
          <Route path="/applications/:id" element={<ApplicationDetailPage />} />
          <Route path="/applications/:id/edit" element={<EditApplicationPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/applications" replace />} />
    </Routes>
  )
}
