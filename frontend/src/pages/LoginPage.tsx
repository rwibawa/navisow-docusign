import { useAuth } from 'react-oidc-context';

export function LoginPage() {
  const auth = useAuth();

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Navisow</h1>
        <p>DocuSign Workflow Console</p>
        <p className="login-hint">Sign in to manage your documents and envelopes.</p>
        <button
          className="btn-primary"
          onClick={() => auth.signinRedirect()}
        >
          Sign in
        </button>
      </div>
    </div>
  );
}
