import { apiRequest } from './client';

const BASE = '/api/documents';

export type DocumentDto = {
  id: string;
  fileName: string;
  status: string;
  createdAt: string;
};

export type PageResult<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
};

export async function uploadDocument(file: File, token: string): Promise<DocumentDto> {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(
    `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}${BASE}`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: form,
    },
  );
  if (!res.ok) throw new Error(`Upload failed: HTTP ${res.status}`);
  return res.json();
}

export function listDocuments(
  token: string,
  page = 0,
  size = 20,
): Promise<PageResult<DocumentDto>> {
  return apiRequest(`${BASE}?page=${page}&size=${size}`, {}, token);
}
