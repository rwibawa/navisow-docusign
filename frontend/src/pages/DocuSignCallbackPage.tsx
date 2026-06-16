import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { apiRequest } from '../api/client';

type CallbackResult = {
  connected: boolean;
  accountId: string;
  baseUri: string;
};

/**
 * Handles the OAuth callback redirect from DocuSign.
 * DocuSign calls: /auth/docusign/callback?code=...&state=...
 */
export function DocuSignCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [error, setError] = useState<string | null>(null);
  const exchanged = useRef(false);

  useEffect(() => {
    if (exchanged.current || !token) return;
    exchanged.current = true;

    const code = params.get('code');
    const state = params.get('state');
    const savedState = sessionStorage.getItem('docusign_oauth_state');

    if (!code) {
      setError('Missing authorization code from DocuSign.');
      return;
    }

    if (state !== savedState) {
      setError('State mismatch — possible CSRF attempt. Please try connecting again.');
      return;
    }

    sessionStorage.removeItem('docusign_oauth_state');

    apiRequest<CallbackResult>(
      '/api/docusign/auth/callback',
      {
        method: 'POST',
        body: JSON.stringify({ code, state }),
      },
      token,
    )
      .then(() => navigate('/docusign/connect', { replace: true }))
      .catch((err: Error) => setError(err.message));
  }, [token, params, navigate]);

  if (error) {
    return (
      <section>
        <h2>DocuSign Connection Failed</h2>
        <p className="error">{error}</p>
        <button onClick={() => navigate('/docusign/connect')}>Back</button>
      </section>
    );
  }

  return (
    <section>
      <p>Completing DocuSign connection…</p>
    </section>
  );
}
