import Constants from 'expo-constants';
import { Platform } from 'react-native';

import { getPassword, notifyUnauthorized } from './auth';

/** Cổng backend, phải trùng với server.port trong application.properties. */
const API_PORT = 8686;

/**
 * Địa chỉ backend Spring Boot.
 * - Nếu đặt EXPO_PUBLIC_API_URL (trong file .env) thì dùng giá trị đó.
 * - Bản web đã build (do chính backend phục vụ, mở bằng Safari): gọi cùng địa chỉ đang mở.
 * - Khi đang phát triển: lấy IP máy tính đang chạy Expo + cổng API_PORT.
 */
function resolveBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv.replace(/\/$/, '');

  if (Platform.OS === 'web' && !__DEV__) return '';

  const hostUri = Constants.expoConfig?.hostUri; // ví dụ "192.168.0.101:8081"
  const host = hostUri?.split(':')[0];
  if (host) return `http://${host}:${API_PORT}`;

  return Platform.OS === 'web' ? `http://localhost:${API_PORT}` : `http://127.0.0.1:${API_PORT}`;
}

export const API_URL = resolveBaseUrl();

export type SplitType = 'EQUAL' | 'EXACT' | 'SHARES';

export interface Member {
  id: number;
  name: string;
}

export interface Group {
  id: number;
  name: string;
  members: Member[];
  totalSpent: number;
  createdAt: string;
}

export interface PortionView {
  memberId: number;
  name: string;
  amount: number;
}

export interface Expense {
  id: number;
  description: string;
  amount: number;
  splitType: SplitType;
  paidBy: PortionView[];
  splitAmong: PortionView[];
  createdAt: string;
}

export interface Balance {
  memberId: number;
  name: string;
  paid: number;
  owed: number;
  sent: number;
  received: number;
  balance: number;
}

export interface Transfer {
  fromMemberId: number;
  fromName: string;
  toMemberId: number;
  toName: string;
  amount: number;
}

export interface Payment {
  id: number;
  fromMemberId: number;
  fromName: string;
  toMemberId: number;
  toName: string;
  amount: number;
  createdAt: string;
}

export interface CreateExpense {
  description: string;
  amount: number;
  paidBy: { memberId: number; amount?: number }[];
  splitType: SplitType;
  splitAmong?: { memberId: number; value?: number }[];
}

export class ApiError extends Error {}

/** Server từ chối vì sai hoặc thiếu mật khẩu. */
export class UnauthorizedError extends ApiError {}

/** Không gọi được server (mất mạng, server tắt). */
export class NetworkError extends ApiError {}

async function request<T>(path: string, init?: RequestInit, password = getPassword()): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(password ? { 'X-App-Password': password } : {}),
        ...init?.headers,
      },
    });
  } catch {
    throw new NetworkError(
      `Không kết nối được server${API_URL ? ` (${API_URL})` : ''}. Kiểm tra kết nối mạng rồi thử lại.`,
    );
  }
  if (res.status === 401) {
    notifyUnauthorized();
    throw new UnauthorizedError('Sai mật khẩu');
  }
  if (!res.ok) {
    let message = `Lỗi ${res.status}`;
    try {
      const body = await res.json();
      if (body?.detail) message = body.detail;
    } catch {
      // body không phải JSON
    }
    throw new ApiError(message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

const json = (body: unknown) => JSON.stringify(body);

export const api = {
  /** Thử mật khẩu; ném UnauthorizedError nếu sai. Không phát sự kiện đăng xuất. */
  checkPassword: async (password: string | null) => {
    const res = await fetch(`${API_URL}/api/auth/check`, {
      headers: password ? { 'X-App-Password': password } : {},
    }).catch(() => {
      throw new NetworkError('Không kết nối được server. Kiểm tra kết nối mạng rồi thử lại.');
    });
    if (res.status === 401) throw new UnauthorizedError('Sai mật khẩu');
    if (!res.ok) throw new ApiError(`Lỗi ${res.status}`);
  },

  listGroups: () => request<Group[]>('/api/groups'),
  getGroup: (id: number) => request<Group>(`/api/groups/${id}`),
  createGroup: (name: string, members: string[]) =>
    request<Group>('/api/groups', { method: 'POST', body: json({ name, members }) }),
  deleteGroup: (id: number) => request<void>(`/api/groups/${id}`, { method: 'DELETE' }),

  addMember: (groupId: number, name: string) =>
    request<Member>(`/api/groups/${groupId}/members`, { method: 'POST', body: json({ name }) }),
  removeMember: (groupId: number, memberId: number) =>
    request<void>(`/api/groups/${groupId}/members/${memberId}`, { method: 'DELETE' }),

  listExpenses: (groupId: number) => request<Expense[]>(`/api/groups/${groupId}/expenses`),
  addExpense: (groupId: number, body: CreateExpense) =>
    request<Expense>(`/api/groups/${groupId}/expenses`, { method: 'POST', body: json(body) }),
  deleteExpense: (groupId: number, expenseId: number) =>
    request<void>(`/api/groups/${groupId}/expenses/${expenseId}`, { method: 'DELETE' }),

  balances: (groupId: number) => request<Balance[]>(`/api/groups/${groupId}/balances`),
  settlements: (groupId: number) => request<Transfer[]>(`/api/groups/${groupId}/settlements`),

  listPayments: (groupId: number) => request<Payment[]>(`/api/groups/${groupId}/payments`),
  recordPayment: (groupId: number, fromMemberId: number, toMemberId: number, amount: number) =>
    request<Payment>(`/api/groups/${groupId}/payments`, {
      method: 'POST',
      body: json({ fromMemberId, toMemberId, amount }),
    }),
  deletePayment: (groupId: number, paymentId: number) =>
    request<void>(`/api/groups/${groupId}/payments/${paymentId}`, { method: 'DELETE' }),
};
