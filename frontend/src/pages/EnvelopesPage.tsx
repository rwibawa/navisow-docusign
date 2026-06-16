import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { type EnvelopeDto, listEnvelopes } from '../api/envelopesApi';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Draft',
  SENT: 'Sent',
  DELIVERED: 'Delivered',
  COMPLETED: 'Completed',
  DECLINED: 'Declined',
  VOIDED: 'Voided',
};

export function EnvelopesPage() {
  const auth = useAuth();
  const token = auth.user?.access_token ?? '';
  const [envelopes, setEnvelopes] = useState<EnvelopeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);

  const load = useCallback(async (page: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await listEnvelopes(token, page);
      setEnvelopes(result.content);
      setTotalPages(result.totalPages);
      setCurrentPage(page);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load envelopes');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => { load(0); }, [load]);

  return (
    <section>
      <div className="page-header">
        <h2>Envelopes</h2>
        <Link to="/envelopes/new" className="btn-primary">+ New Envelope</Link>
      </div>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p>Loading envelopes…</p>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Status</th>
                <th>Created</th>
                <th>DocuSign ID</th>
              </tr>
            </thead>
            <tbody>
              {envelopes.length === 0 && (
                <tr>
                  <td colSpan={4} className="empty-row">No envelopes yet.</td>
                </tr>
              )}
              {envelopes.map((env) => (
                <tr key={env.id}>
                  <td>
                    <Link to={`/envelopes/${env.id}`}>{env.subject}</Link>
                  </td>
                  <td>
                    <span className={`badge badge-${env.status.toLowerCase()}`}>
                      {STATUS_LABELS[env.status] ?? env.status}
                    </span>
                  </td>
                  <td>{new Date(env.createdAt).toLocaleDateString()}</td>
                  <td className="mono">{env.docuSignEnvelopeId ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="btn-ghost"
                disabled={currentPage === 0}
                onClick={() => load(currentPage - 1)}
              >
                ← Prev
              </button>
              <span>Page {currentPage + 1} / {totalPages}</span>
              <button
                className="btn-ghost"
                disabled={currentPage >= totalPages - 1}
                onClick={() => load(currentPage + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
