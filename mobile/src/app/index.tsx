import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button, Card, colors, Empty } from '@/components/ui';
import { api, ApiError, Group } from '@/lib/api';
import { formatVND } from '@/lib/format';

export default function GroupsScreen() {
  const insets = useSafeAreaInsets();
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setGroups(await api.listGroups());
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={groups}
        keyExtractor={(g) => String(g.id)}
        contentContainerStyle={{ padding: 16, gap: 12, paddingBottom: 100 }}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
        ListHeaderComponent={
          error ? (
            <Card style={{ backgroundColor: colors.dangerSoft, borderColor: colors.danger }}>
              <Text style={{ color: colors.danger }}>{error}</Text>
            </Card>
          ) : undefined
        }
        ListEmptyComponent={
          !loading && !error ? <Empty>Chưa có nhóm nào. Tạo nhóm đầu tiên để bắt đầu chia tiền.</Empty> : undefined
        }
        renderItem={({ item }) => (
          <Pressable onPress={() => router.push(`/group/${item.id}`)}>
            {({ pressed }) => (
              <Card style={{ opacity: pressed ? 0.7 : 1 }}>
                <Text style={s.name}>{item.name}</Text>
                <View style={s.row}>
                  <Text style={s.meta}>{item.members.length} người</Text>
                  <Text style={s.total}>{formatVND(item.totalSpent)}</Text>
                </View>
                <Text style={s.meta} numberOfLines={1}>
                  {item.members.map((m) => m.name).join(', ')}
                </Text>
              </Card>
            )}
          </Pressable>
        )}
      />
      <View style={[s.footer, { paddingBottom: insets.bottom + 12 }]}>
        <Button title="+ Tạo nhóm mới" onPress={() => router.push('/new-group')} />
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  name: { fontSize: 18, fontWeight: '700', color: colors.text },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 6 },
  meta: { color: colors.muted, fontSize: 14, marginTop: 2 },
  total: { fontSize: 16, fontWeight: '600', color: colors.text },
  footer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    paddingHorizontal: 16,
    paddingTop: 12,
    backgroundColor: colors.bg,
  },
});
