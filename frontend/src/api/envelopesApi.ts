import { apiRequest } from './client';

const BASE = '/api/envelopes';

export type RecipientInput = {
  recipientId: number;
  routingOrder: number;
  name: string;
  email: string;
  clientUserId?: string;
};

export type SendEnvelopeInput = {
  documentId?: string;
  subject: string;
  recipients: RecipientInput[];
  reminderDays?: number;
  expirationDays?: number;
};

export type EnvelopeDto = {
  id: string;
  subject: string;
  status: string;
  docuSignEnvelopeId?: string;
  createdAt: string;
  updatedAt: string;
};

export type EnvelopeEventDto = {
  id: string;
  eventType: string;
  occurredAt: string;
};

export type EnvelopeDetail = EnvelopeDto & { events: EnvelopeEventDto[] };

export function sendEnvelope(input: SendEnvelopeInput, token: string): Promise<EnvelopeDto> {
  return apiRequest(BASE, { method: 'POST', body: JSON.stringify(input) }, token);
}

export function listEnvelopes(token: string, page = 0, size = 20) {
  return apiRequest<{ content: EnvelopeDto[]; totalElements: number; totalPages: number; page: number }>(
    `${BASE}?page=${page}&size=${size}`,
    {},
    token,
  );
}

export function getEnvelopeDetail(id: string, token: string): Promise<EnvelopeDetail> {
  return apiRequest(`${BASE}/${id}`, {}, token);
}

export async function getSigningUrl(
  envelopeId: string,
  payload: { recipientEmail: string; recipientName: string; clientUserId: string; returnUrl: string },
  token: string,
): Promise<{ url: string }> {
  return apiRequest(`${BASE}/${envelopeId}/signing-url`, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, token);
}
