import { useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { getSystemStatus } from '../services/systemApi';

type Status = {
  service: string;
  status: string;
  timestamp: string;
};

export function HealthPage() {
  const auth = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<Status | null>(null);

  useEffect(() => {
    getSystemStatus(auth.user?.access_token)
      .then((data) => setStatus(data))
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [auth.user?.access_token]);

  if (loading) {
    return <p>Checking backend status…</p>;
  }

  if (error) {
    return <p className="error">Failed to load status: {error}</p>;
  }

  return (
    <section>
      <h2>Backend Status</h2>
      <pre>{JSON.stringify(status, null, 2)}</pre>
    </section>
  );
}
