const BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...init,
  });
  if (res.status === 401 && typeof window !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    window.location.href = '/login';
    throw new Error('401 Unauthorized');
  }
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.status === 204 ? (undefined as T) : res.json();
}

// ── Types ──────────────────────────────────────────────────────────────────

export type Client = {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  createdAt: string;
};

export type ClientInput = { name: string; email: string; phone?: string };

export type Task = {
  id: number;
  title: string;
  description: string | null;
  status: 'NEW' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  deadline: string | null;
  clientId: number;
};

export type TaskInput = {
  title: string;
  description?: string;
  status?: Task['status'];
  priority?: Task['priority'];
  deadline?: string;
  clientId: number;
};

export type DashboardStats = {
  totalClients: number;
  tasksByStatus: Record<'NEW' | 'IN_PROGRESS' | 'DONE', number>;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Role = 'ADMIN' | 'MANAGER' | 'VIEWER';

export type LoginInput = { username: string; password: string };

export type AuthResponse = { token: string; username: string; role: Role };

// ── API functions ──────────────────────────────────────────────────────────

export const clientsApi = {
  getAll: () => request<PageResponse<Client>>('/api/clients').then(p => p.content),
  create: (data: ClientInput) =>
    request<Client>('/api/clients', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: ClientInput) =>
    request<Client>(`/api/clients/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/api/clients/${id}`, { method: 'DELETE' }),
};

export const tasksApi = {
  getAll: (params?: { status?: string; clientId?: number }) => {
    const qs = new URLSearchParams();
    if (params?.status) qs.set('status', params.status);
    if (params?.clientId) qs.set('clientId', String(params.clientId));
    const q = qs.toString();
    return request<PageResponse<Task>>(`/api/tasks${q ? `?${q}` : ''}`).then(p => p.content);
  },
  create: (data: TaskInput) =>
    request<Task>('/api/tasks', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: TaskInput) =>
    request<Task>(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/api/tasks/${id}`, { method: 'DELETE' }),
};

export const dashboardApi = {
  getStats: () => request<DashboardStats>('/api/dashboard'),
};

export const authApi = {
  login: (data: LoginInput) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),
};
