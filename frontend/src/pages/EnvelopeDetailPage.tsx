import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import {
  type EnvelopeDetail,
  getEnvelopeDetail,
  getSigningUrl,
} from '../api/envelopesApi';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const STATUS_COLOR: Record<string, string> = {
  SENT: 'badge-sent',
  DELIVERED: 'badge-delivered',
  COMPLETED: 'badge-completed',
  DECLINED: 'badge-declined',
  VOIDED: 'badge-voided',
  DRAFT: 'badge-draft',
};

export function EnvelopeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const auth = useAuth();
  const token = auth.user?.access_token ?? '';

  const [envelope, setEnvelope] = useState<EnvelopeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [signingEmail, setSigningEmail] = useState('');
  const [signingName, setSigningName] = useState('');
  const [clientUserId, setClientUserId] = useState('1');
  const [loadingSignUrl, setLoadingSignUrl] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getEnvelopeDetail(id, token);
      setEnvelope(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load envelope');
    } finally {
      setLoading(false);
    }
  }, [id, token]);

  useEffect(() => { load(); }, [load]);

  const handleGetSigningUrl = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setLoadingSignUrl(true);
    setError(null);
    try {
      const { url } = await getSigningUrl(id, {
        recipientEmail: signingEmail,
        recipientName: signingName,
        clientUserId,
        returnUrl: `${window.location.origin}/envelopes/${id}`,
      }, token);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to get signing URL');
    } finally {
      setLoadingSignUrl(false);
    }
  };

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="error">{error}</p>;
  if (!envelope) return <p className="error">Envelope not found.</p>;

  return (
    <section>
      <button className="btn-ghost back-btn" onClick={() => navigate('/envelopes')}>
        ← Back to Envelopes
      </button>

      <h2>{envelope.subject}</h2>

      <div className="detail-meta">
        <span className={`badge ${STATUS_COLOR[envelope.status] ?? ''}`}>
          {envelope.status}
        </span>
        <span className="hint">Created {new Date(envelope.createdAt).toLocaleString()}</span>
        {envelope.docuSignEnvelopeId && (
          <span className="mono hint">ID: {envelope.docuSignEnvelopeId}</span>
        )}
      </div>

      {/* Certificate download */}
      {envelope.status === 'COMPLETED' && envelope.docuSignEnvelopeId && (
        <div className="action-section">
          <a
            href={`${API_BASE}/api/envelopes/${id}/certificate`}
            download={`certificate-${id}.pdf`}
            className="btn-primary"
          >
            Download Certificate of Completion
          </a>
        </div>
      )}

      {/* Embedded signing form */}
      {(envelope.status === 'SENT' || envelope.status === 'DELIVERED') && (
        <div className="action-section">
          <h3>Open Signing Session</h3>
          <form className="compose-form compact" onSubmit={handleGetSigningUrl}>
            <div className="form-row">
              <div className="form-group">
                <label>Recipient Email</label>
                <input
                  type="email"
                  required
                  title="Recipient email address"
                  value={signingEmail}
                  onChange={(e) => setSigningEmail(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>Recipient Name</label>
                <input
                  type="text"
                  required
                  title="Recipient full name"
                  value={signingName}
                  onChange={(e) => setSigningName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>Client User ID</label>
                <input
                  type="text"
                  required
                  title="Client user ID for embedded signing"
                  value={clientUserId}
                  onChange={(e) => setClientUserId(e.target.value)}
                />
              </div>
            </div>
            <button type="submit" className="btn-primary" disabled={loadingSignUrl}>
              {loadingSignUrl ? 'Getting URL…' : 'Open Signing Session'}
            </button>
          </form>
        </div>
      )}

      {/* Event timeline */}
      <div className="timeline-section">
        <h3>Event Timeline</h3>
        {envelope.events.length === 0 ? (
          <p className="hint">No events recorded yet.</p>
        ) : (
          <ol className="timeline">
            {envelope.events.map((ev) => (
              <li key={ev.id} className="timeline-item">
                <span className="timeline-event">{ev.eventType}</span>
                <span className="timeline-time">
                  {new Date(ev.occurredAt).toLocaleString()}
                </span>
              </li>
            ))}
          </ol>
        )}
      </div>
    </section>
  );
}
