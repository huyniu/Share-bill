import { ReactNode, useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Image, KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';

import { Button, colors, Input } from '@/components/ui';
import { api, UnauthorizedError } from '@/lib/api';
import { clearPassword, loadPassword, onUnauthorized, savePassword } from '@/lib/auth';

type State = 'checking' | 'locked' | 'offline' | 'ok';

/**
 * Hiện màn hình nhập mật khẩu nếu server yêu cầu, rồi mới vào app.
 * Mật khẩu đúng được lưu trên máy nên lần sau không phải nhập lại.
 */
export function AuthGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<State>('checking');
  const [slow, setSlow] = useState(false);

  const check = useCallback(async () => {
    setState('checking');
    setSlow(false);
    // Server miễn phí ngủ sau 15 phút; báo cho người dùng nếu phải chờ lâu
    const timer = setTimeout(() => setSlow(true), 4000);
    try {
      await api.checkPassword(await loadPassword());
      setState('ok');
    } catch (e) {
      setState(e instanceof UnauthorizedError ? 'locked' : 'offline');
    } finally {
      clearTimeout(timer);
    }
  }, []);

  useEffect(() => {
    check();
    return onUnauthorized(() => {
      clearPassword();
      setState('locked');
    });
  }, [check]);

  if (state === 'ok') return <>{children}</>;

  if (state === 'checking') {
    return (
      <View style={s.center}>
        <ActivityIndicator size="large" color={colors.primary} />
        <Text style={s.muted}>
          {slow ? 'Server đang khởi động, có thể mất khoảng 1 phút...' : 'Đang kết nối...'}
        </Text>
      </View>
    );
  }

  if (state === 'offline') {
    return (
      <View style={s.center}>
        <Text style={s.title}>Không kết nối được</Text>
        <Text style={s.muted}>Kiểm tra mạng của điện thoại rồi thử lại.</Text>
        <Button title="Thử lại" onPress={check} style={{ alignSelf: 'stretch' }} />
      </View>
    );
  }

  return <LoginScreen onSuccess={() => setState('ok')} />;
}

function LoginScreen({ onSuccess }: { onSuccess: () => void }) {
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!password) return;
    setLoading(true);
    setError(null);
    try {
      await api.checkPassword(password);
      await savePassword(password);
      onSuccess();
    } catch (e) {
      setError(e instanceof UnauthorizedError ? 'Sai mật khẩu, thử lại nhé.' : (e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={s.center}>
        <Image source={require('../../assets/icon.png')} style={s.logo} />
        <Text style={s.title}>Share Bill</Text>
        <Text style={s.muted}>Nhập mật khẩu của nhóm để tiếp tục</Text>
        <View style={{ alignSelf: 'stretch', gap: 12 }}>
          <Input
            placeholder="Mật khẩu"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoFocus
            autoCapitalize="none"
            autoCorrect={false}
            onSubmitEditing={submit}
            returnKeyType="go"
          />
          {error ? <Text style={{ color: colors.danger }}>{error}</Text> : null}
          <Button title="Vào app" onPress={submit} disabled={!password} loading={loading} />
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 14,
    backgroundColor: colors.bg,
  },
  logo: { width: 84, height: 84, borderRadius: 20 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text },
  muted: { color: colors.muted, fontSize: 15, textAlign: 'center' },
});
