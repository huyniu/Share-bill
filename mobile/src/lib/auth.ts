import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'sharebill.password';

let current: string | null = null;
const listeners = new Set<() => void>();

/** Mật khẩu đang dùng (đã nạp từ bộ nhớ máy). */
export function getPassword(): string | null {
  return current;
}

export async function loadPassword(): Promise<string | null> {
  try {
    current = await AsyncStorage.getItem(KEY);
  } catch {
    current = null;
  }
  return current;
}

export async function savePassword(password: string): Promise<void> {
  current = password;
  try {
    await AsyncStorage.setItem(KEY, password);
  } catch {
    // Không lưu được (ví dụ chế độ ẩn danh): vẫn dùng được trong phiên này
  }
}

export async function clearPassword(): Promise<void> {
  current = null;
  try {
    await AsyncStorage.removeItem(KEY);
  } catch {
    // bỏ qua
  }
}

/** Đăng ký nhận thông báo khi server từ chối mật khẩu (401). */
export function onUnauthorized(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function notifyUnauthorized() {
  listeners.forEach((l) => l());
}
