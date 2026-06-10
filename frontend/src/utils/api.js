import { API_BASE_URL } from '../config';

export async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('jwt_token');
  
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers
  };

  const config = {
    ...options,
    headers
  };

  // Build full URL
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

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
