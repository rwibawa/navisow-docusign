import { useCallback, useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { apiRequest } from '../api/client';

type ConnectionStatus = {
  connected: boolean;
  accountId?: string;
  expiresAt?: string;
};

type AuthorizeUrlResponse = {
  url: string;
  state: string;
};

export function DocuSignConnectPage() {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const [status, setStatus] = useState<ConnectionStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    if (!token) return;
    try {
      const data = await apiRequest<ConnectionStatus>(
        '/api/docusign/auth/status',
        {},
        token,
      );
      setStatus(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  const handleConnect = async () => {
    if (!token) return;
    try {
      const { url, state } = await apiRequest<AuthorizeUrlResponse>(
        '/api/docusign/auth/authorize-url',
        {},
        token,
      );
      // Persist state for CSRF validation in callback
      sessionStorage.setItem('docusign_oauth_state', state);
      window.location.href = url;
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to start OAuth flow');
    }
  };

  const handleDisconnect = async () => {
    if (!token) return;
    try {
      await apiRequest<void>('/api/docusign/auth/connection', { method: 'DELETE' }, token);
      await fetchStatus();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to disconnect');
    }
  };

  if (loading) return <p>Loading DocuSign status…</p>;

  return (
    <section>
      <h2>DocuSign Account</h2>

      {error && <p className="error">{error}</p>}

      {status?.connected ? (
        <div className="status-card connected">
          <p>
            <strong>Connected</strong> — account <code>{status.accountId}</code>
          </p>
          {status.expiresAt && (
            <p className="hint">Token expires: {new Date(status.expiresAt).toLocaleString()}</p>
          )}
          <button className="btn-danger" onClick={handleDisconnect}>
            Disconnect
          </button>
        </div>
      ) : (
        <div className="status-card">
          <p>No DocuSign account connected.</p>
          <button className="btn-primary" onClick={handleConnect}>
            Connect DocuSign Account
          </button>
        </div>
      )}
    </section>
  );
}
