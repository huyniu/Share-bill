import { Alert, Platform } from 'react-native';

/** 1200000 -> "1.200.000đ" */
export function formatVND(amount: number): string {
  const sign = amount < 0 ? '-' : '';
  const digits = Math.abs(Math.round(amount)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${digits}đ`;
}

/**
 * Đọc số tiền người dùng gõ. Hỗ trợ "1.200.000", "1200000", "150k", "1,5tr", "2tr".
 * Trả về null nếu không hợp lệ.
 */
export function parseAmount(input: string): number | null {
  const s = input.trim().toLowerCase().replace(/\s|đ|vnd/g, '');
  if (!s) return null;
  const m = s.match(/^([\d.,]+)(k|tr|m)?$/);
  if (!m) return null;
  const [, num, unit] = m;
  let value: number;
  if (unit) {
    // Có đơn vị: dấu phẩy hoặc chấm là phần thập phân (1,5tr = 1.500.000)
    value = Number(num.replace(',', '.'));
    value *= unit === 'k' ? 1_000 : 1_000_000;
  } else {
    // Không đơn vị: dấu chấm/phẩy là phân cách hàng nghìn
    value = Number(num.replace(/[.,]/g, ''));
  }
  if (!Number.isFinite(value) || value <= 0) return null;
  return Math.round(value);
}

export function formatDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function showError(e: unknown) {
  const message = e instanceof Error ? e.message : String(e);
  if (Platform.OS === 'web') {
    window.alert(message);
  } else {
    Alert.alert('Có lỗi', message);
  }
}

/** Hộp thoại xác nhận chạy được cả trên iPhone lẫn web. */
export function confirm(title: string, message: string, okText = 'Đồng ý'): Promise<boolean> {
  if (Platform.OS === 'web') {
    return Promise.resolve(window.confirm(`${title}\n\n${message}`));
  }
  return new Promise((resolve) => {
    Alert.alert(title, message, [
      { text: 'Hủy', style: 'cancel', onPress: () => resolve(false) },
      { text: okText, style: 'destructive', onPress: () => resolve(true) },
    ]);
  });
}
