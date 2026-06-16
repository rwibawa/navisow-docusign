import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { type RecipientInput, sendEnvelope } from '../api/envelopesApi';

type RecipientRow = RecipientInput & { key: string };

export function EnvelopeComposePage() {
  const [params] = useSearchParams();
  const documentId = params.get('documentId') ?? undefined;
  const fileName = params.get('fileName') ?? 'document';
  const navigate = useNavigate();
  const auth = useAuth();
  const token = auth.user?.access_token ?? '';

  const [subject, setSubject] = useState(`Please sign: ${fileName}`);
  const [reminderDays, setReminderDays] = useState<string>('3');
  const [expirationDays, setExpirationDays] = useState<string>('30');
  const [recipients, setRecipients] = useState<RecipientRow[]>([
    { key: crypto.randomUUID(), recipientId: 1, routingOrder: 1, name: '', email: '', clientUserId: undefined },
  ]);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const addRecipient = () => {
    setRecipients((prev) => [
      ...prev,
      {
        key: crypto.randomUUID(),
        recipientId: prev.length + 1,
        routingOrder: prev.length + 1,
        name: '',
        email: '',
        clientUserId: undefined,
      },
    ]);
  };

  const updateRecipient = (key: string, field: keyof RecipientInput, value: string) => {
    setRecipients((prev) =>
      prev.map((r) => (r.key === key ? { ...r, [field]: value } : r)),
    );
  };

  const removeRecipient = (key: string) => {
    setRecipients((prev) => prev.filter((r) => r.key !== key));
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    setSending(true);
    setError(null);
    try {
      const envelope = await sendEnvelope(
        {
          documentId,
          subject,
          recipients: recipients.map(({ key: _k, ...r }) => r),
          reminderDays: reminderDays ? Number(reminderDays) : undefined,
          expirationDays: expirationDays ? Number(expirationDays) : undefined,
        },
        token,
      );
      navigate(`/envelopes/${envelope.id}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to send envelope');
      setSending(false);
    }
  };

  return (
    <section>
      <h2>Send Envelope for Signature</h2>
      {documentId && (
        <p className="hint">
          Document: <strong>{fileName}</strong>
        </p>
      )}

      {error && <p className="error">{error}</p>}

      <form className="compose-form" onSubmit={handleSend}>
        <div className="form-group">
          <label>Email Subject</label>
          <input
            type="text"
            required
            title="Email subject"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
          />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Reminder (days)</label>
            <input
              type="number"
              min="1"
              max="30"
              value={reminderDays}
              title="Reminder delay in days"
              onChange={(e) => setReminderDays(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>Expiration (days)</label>
            <input
              type="number"
              min="1"
              max="180"
              value={expirationDays}
              title="Expiration in days"
              onChange={(e) => setExpirationDays(e.target.value)}
            />
          </div>
        </div>

        <h3>Recipients</h3>
        {recipients.map((r, i) => (
          <div key={r.key} className="recipient-row">
            <span className="recipient-num">#{i + 1}</span>
            <input
              type="text"
              placeholder="Name"
              required
              value={r.name}
              onChange={(e) => updateRecipient(r.key, 'name', e.target.value)}
            />
            <input
              type="email"
              placeholder="Email"
              required
              value={r.email}
              onChange={(e) => updateRecipient(r.key, 'email', e.target.value)}
            />
            {recipients.length > 1 && (
              <button
                type="button"
                className="btn-ghost"
                onClick={() => removeRecipient(r.key)}
              >
                Remove
              </button>
            )}
          </div>
        ))}
        <button type="button" className="btn-ghost" onClick={addRecipient}>
          + Add Recipient
        </button>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={sending}>
            {sending ? 'Sending…' : 'Send Envelope'}
          </button>
          <button type="button" className="btn-ghost" onClick={() => navigate('/documents')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
