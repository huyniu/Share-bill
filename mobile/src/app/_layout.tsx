import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { AuthGate } from '@/components/AuthGate';
import { colors } from '@/components/ui';

export default function RootLayout() {
  return (
    <AuthGate>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerTintColor: colors.primary,
          headerTitleStyle: { color: colors.text },
          contentStyle: { backgroundColor: colors.bg },
          headerBackButtonDisplayMode: 'minimal',
        }}
      >
        <Stack.Screen name="index" options={{ title: 'Share Bill' }} />
        <Stack.Screen name="new-group" options={{ title: 'Nhóm mới', presentation: 'modal' }} />
        <Stack.Screen name="group/[id]/index" options={{ title: '' }} />
        <Stack.Screen name="group/[id]/add-expense" options={{ title: 'Thêm khoản chi', presentation: 'modal' }} />
      </Stack>
    </AuthGate>
  );
}
