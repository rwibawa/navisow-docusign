import { apiRequest } from '../api/client';

type SystemStatus = {
  service: string;
  status: string;
  timestamp: string;
};

export function getSystemStatus(token?: string) {
  return apiRequest<SystemStatus>('/api/system/status', {}, token);
}
