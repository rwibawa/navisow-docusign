import { NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { HomePage } from './pages/HomePage';
import { HealthPage } from './pages/HealthPage';
import { DocuSignConnectPage } from './pages/DocuSignConnectPage';
import { DocuSignCallbackPage } from './pages/DocuSignCallbackPage';
import { DocumentsPage } from './pages/DocumentsPage';
import { EnvelopesPage } from './pages/EnvelopesPage';
import { EnvelopeComposePage } from './pages/EnvelopeComposePage';
import { EnvelopeDetailPage } from './pages/EnvelopeDetailPage';

export function App() {
  const auth = useAuth();

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>Navisow</h1>
        <p>DocuSign Workflow Console</p>
        <nav>
          <NavLink to="/" end>Overview</NavLink>
          <NavLink to="/documents">Documents</NavLink>
          <NavLink to="/envelopes">Envelopes</NavLink>
          <NavLink to="/docusign/connect">DocuSign Account</NavLink>
          <NavLink to="/health">Backend Status</NavLink>
        </nav>
        {auth.isAuthenticated && (
          <div className="user-bar">
            <span className="user-name">
              {auth.user?.profile.name ?? auth.user?.profile.email}
            </span>
            <button
              className="btn-ghost"
              onClick={() => auth.signoutRedirect()}
            >
              Sign out
            </button>
          </div>
        )}
      </aside>
      <main className="content">
        <Routes>
          {/* Public callback routes */}
          <Route path="/auth/docusign/callback" element={<DocuSignCallbackPage />} />

          {/* Protected routes */}
          <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
          <Route path="/health" element={<ProtectedRoute><HealthPage /></ProtectedRoute>} />
          <Route path="/docusign/connect" element={<ProtectedRoute><DocuSignConnectPage /></ProtectedRoute>} />
          <Route path="/documents" element={<ProtectedRoute><DocumentsPage /></ProtectedRoute>} />
          <Route path="/envelopes" element={<ProtectedRoute><EnvelopesPage /></ProtectedRoute>} />
          <Route path="/envelopes/new" element={<ProtectedRoute><EnvelopeComposePage /></ProtectedRoute>} />
          <Route path="/envelopes/:id" element={<ProtectedRoute><EnvelopeDetailPage /></ProtectedRoute>} />
        </Routes>
      </main>
    </div>
  );
}
