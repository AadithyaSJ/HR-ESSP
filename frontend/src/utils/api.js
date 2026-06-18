import { API_BASE_URL } from '../config';

export async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('jwt_token');
  
  const headers = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    'X-Tunnel-Skip-AntiPhishing-Page': 'true',
    ...options.headers
  };

  const config = {
    ...options,
    headers
  };

  // Build full URL safely without duplicate slashes
  let url;
  if (endpoint.startsWith('http')) {
    url = endpoint;
  } else {
    const baseUrl = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
    const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    url = `${baseUrl}${path}`;
  }

  const response = await fetch(url, config);
  
  if (!response.ok) {
    let errorData = null;
    try {
      errorData = await response.json();
    } catch (e) {
      // ignore
    }
    const message = errorData?.message || `HTTP error! status: ${response.status}`;
    throw new Error(message);
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    const json = await response.json();
    if (json.success === false) {
      throw new Error(json.message || 'Operation failed');
    }
    return json.data !== undefined ? json.data : json;
  }
  
  return null;
}
