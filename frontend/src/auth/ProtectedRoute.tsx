import { useAuth } from 'react-oidc-context';
import { LoginPage } from '../pages/LoginPage';

interface Props {
  children: React.ReactNode;
}

export function ProtectedRoute({ children }: Props) {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <div className="loading-screen">
        <p>Checking authentication…</p>
      </div>
    );
  }

  if (auth.error) {
    return (
      <div className="error-screen">
        <p className="error">Auth error: {auth.error.message}</p>
        <button onClick={() => auth.signinRedirect()}>Retry login</button>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return <LoginPage />;
  }

  return <>{children}</>;
}
