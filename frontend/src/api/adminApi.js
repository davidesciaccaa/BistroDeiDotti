import { normalizeMenuSections } from '../utils/menu.js';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '');

// sessionStorage, not localStorage: the admin session dies with the browser session.
const TOKEN_KEY = 'adv.admin.token';

export class AdminApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.name = 'AdminApiError';
    this.status = status;
    this.code = code;
  }
}

export function getStoredToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function clearStoredToken() {
  sessionStorage.removeItem(TOKEN_KEY);
}

async function readErrorPayload(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function adminRequest(path, { method = 'GET', body } = {}) {
  const token = getStoredToken();

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: body ? JSON.stringify(body) : undefined
    });
  } catch {
    throw new AdminApiError('Server non raggiungibile. Controlla che il backend sia attivo.', 0, 'network_error');
  }

  if (response.status === 401) {
    clearStoredToken();
    const payload = await readErrorPayload(response);
    throw new AdminApiError(payload?.message ?? 'Sessione scaduta.', 401, payload?.error ?? 'unauthorized');
  }

  if (!response.ok) {
    const payload = await readErrorPayload(response);
    throw new AdminApiError(
      payload?.message ?? `Richiesta fallita (${response.status}).`,
      response.status,
      payload?.error
    );
  }

  return response.status === 204 ? null : response.json();
}

export async function adminLogin(password) {
  const session = await adminRequest('/admin/login', { method: 'POST', body: { password } });
  sessionStorage.setItem(TOKEN_KEY, session.token);
  return session;
}

export async function adminLogout() {
  try {
    await adminRequest('/admin/logout', { method: 'POST' });
  } finally {
    clearStoredToken();
  }
}

export function fetchAdminSession() {
  return adminRequest('/admin/session');
}

export async function fetchAdminMenuSections() {
  return normalizeMenuSections(await adminRequest('/admin/menu/sections'));
}

export async function saveAdminPrices(prices) {
  return normalizeMenuSections(await adminRequest('/admin/menu/prices', { method: 'PATCH', body: { prices } }));
}

export async function createAdminMenuItem(item) {
  return normalizeMenuSections(await adminRequest('/admin/menu/items', { method: 'POST', body: item }));
}

export async function updateAdminMenuItem(id, item) {
  return normalizeMenuSections(await adminRequest(`/admin/menu/items/${encodeURIComponent(id)}`, { method: 'PATCH', body: item }));
}

export async function deleteAdminMenuItem(id) {
  return normalizeMenuSections(await adminRequest(`/admin/menu/items/${encodeURIComponent(id)}`, { method: 'DELETE' }));
}
