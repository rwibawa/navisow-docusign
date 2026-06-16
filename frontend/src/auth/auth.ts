import type { UserManagerSettings } from 'oidc-client-ts';

export const oidcConfig: UserManagerSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8090/realms/navisow',
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'navisow-frontend',
  redirect_uri: `${window.location.origin}/auth/oidc/callback`,
  post_logout_redirect_uri: window.location.origin,
  scope: 'openid profile email',
  response_type: 'code',
  automaticSilentRenew: true,
};
