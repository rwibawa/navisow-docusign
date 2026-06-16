import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { type DocumentDto, type PageResult, listDocuments, uploadDocument } from '../api/documentsApi';

export function DocumentsPage() {
  const auth = useAuth();
  const token = auth.user?.access_token ?? '';
  const [page, setPage] = useState<PageResult<DocumentDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listDocuments(token);
      setPage(result);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => { load(); }, [load]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      await uploadDocument(file, token);
      await load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <section>
      <div className="page-header">
        <h2>Documents</h2>
        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.docx,.doc"
            title="Upload document file"
            className="visually-hidden"
            onChange={handleFileChange}
          />
          <button
            className="btn-primary"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            {uploading ? 'Uploading…' : '+ Upload Document'}
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p>Loading documents…</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>File Name</th>
              <th>Status</th>
              <th>Uploaded</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {page?.content.length === 0 && (
              <tr>
                <td colSpan={4} className="empty-row">No documents yet. Upload one above.</td>
              </tr>
            )}
            {page?.content.map((doc) => (
              <tr key={doc.id}>
                <td>{doc.fileName}</td>
                <td><span className={`badge badge-${doc.status.toLowerCase()}`}>{doc.status}</span></td>
                <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                <td>
                  <Link to={`/envelopes/new?documentId=${doc.id}&fileName=${encodeURIComponent(doc.fileName)}`}
                    className="link-action">
                    Send for Signature
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
