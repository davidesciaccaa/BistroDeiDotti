import { normalizeMenuSections } from '../utils/menu.js';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '');

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`API request failed with status ${response.status}`);
  }

  return response.json();
}

export function fetchApiStatus() {
  return request('/status');
}

export async function fetchMenuSections() {
  return normalizeMenuSections(await request('/menu/sections'));
}
