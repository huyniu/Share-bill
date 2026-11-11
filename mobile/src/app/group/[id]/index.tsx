import { router, Stack, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button, Card, Chip, colors, Empty, Input, SectionTitle, Segmented } from '@/components/ui';
import { api, Balance, Expense, Group, Payment, Transfer } from '@/lib/api';
import { confirm, formatDate, formatVND, showError } from '@/lib/format';

type Tab = 'expenses' | 'balances' | 'settle';

export default function GroupScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const groupId = Number(id);
  const insets = useSafeAreaInsets();

  const [tab, setTab] = useState<Tab>('settle');
  const [group, setGroup] = useState<Group | null>(null);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [balances, setBalances] = useState<Balance[]>([]);
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newMember, setNewMember] = useState('');
  const [busyKey, setBusyKey] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [g, e, b, t, p] = await Promise.all([
        api.getGroup(groupId),
        api.listExpenses(groupId),
        api.balances(groupId),
        api.settlements(groupId),
        api.listPayments(groupId),
      ]);
      setGroup(g);
      setExpenses(e);
      setBalances(b);
      setTransfers(t);
      setPayments(p);
    } catch (err) {
      showError(err);
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const run = async (key: string, action: () => Promise<unknown>) => {
    setBusyKey(key);
    try {
      await action();
      await load();
    } catch (e) {
      showError(e);
    } finally {
      setBusyKey(null);
    }
  };

  const addMember = () => {
    const name = newMember.trim();
    if (!name) return;
    run('member', async () => {
      await api.addMember(groupId, name);
      setNewMember('');
    });
  };

  const removeMember = async (memberId: number, name: string) => {
    if (await confirm('Xóa thành viên', `Xóa ${name} khỏi nhóm?`, 'Xóa')) {
      run(`m${memberId}`, () => api.removeMember(groupId, memberId));
    }
  };

  const deleteExpense = async (e: Expense) => {
    if (await confirm('Xóa khoản chi', `Xóa "${e.description}" (${formatVND(e.amount)})?`, 'Xóa')) {
      run(`e${e.id}`, () => api.deleteExpense(groupId, e.id));
    }
  };

  const markPaid = (t: Transfer, index: number) =>
    run(`t${index}`, () => api.recordPayment(groupId, t.fromMemberId, t.toMemberId, t.amount));

  const undoPayment = async (p: Payment) => {
    if (await confirm('Hủy thanh toán', `Hủy lần ${p.fromName} trả ${p.toName} ${formatVND(p.amount)}?`, 'Hủy thanh toán')) {
      run(`p${p.id}`, () => api.deletePayment(groupId, p.id));
    }
  };

  const deleteGroup = async () => {
    if (await confirm('Xóa nhóm', `Xóa nhóm "${group?.name}" và toàn bộ dữ liệu?`, 'Xóa nhóm')) {
      try {
        await api.deleteGroup(groupId);
        if (router.canGoBack()) router.back();
        else router.replace('/');
      } catch (e) {
        showError(e);
      }
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <Stack.Screen options={{ title: group?.name ?? '' }} />
      <ScrollView
        contentContainerStyle={{ padding: 16, gap: 12, paddingBottom: 110 }}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
        keyboardShouldPersistTaps="handled"
      >
        {group && (
          <Card style={{ gap: 10 }}>
            <View style={s.rowBetween}>
              <Text style={s.muted}>Tổng chi tiêu</Text>
              <Text style={s.bigAmount}>{formatVND(group.totalSpent)}</Text>
            </View>
            <View style={s.wrap}>
              {group.members.map((m) => (
                <Chip key={m.id} label={m.name} onPress={() => removeMember(m.id, m.name)} />
              ))}
            </View>
            <View style={{ flexDirection: 'row', gap: 8 }}>
              <View style={{ flex: 1 }}>
                <Input
                  placeholder="Thêm người..."
                  value={newMember}
                  onChangeText={setNewMember}
                  onSubmitEditing={addMember}
                  returnKeyType="done"
                />
              </View>
              <Button title="Thêm" variant="secondary" onPress={addMember} loading={busyKey === 'member'} />
            </View>
          </Card>
        )}

        <Segmented<Tab>
          value={tab}
          onChange={setTab}
          options={[
            { value: 'settle', label: 'Ai trả ai' },
            { value: 'balances', label: 'Số dư' },
            { value: 'expenses', label: `Khoản chi (${expenses.length})` },
          ]}
        />

        {tab === 'settle' && (
          <>
            {transfers.length === 0 ? (
              <Empty>{expenses.length === 0 ? 'Chưa có khoản chi nào.' : 'Cả nhóm đã hết nợ 🎉'}</Empty>
            ) : (
              transfers.map((t, i) => (
                <Card key={`${t.fromMemberId}-${t.toMemberId}`} style={{ gap: 12 }}>
                  <View style={s.rowBetween}>
                    <Text style={s.transferText}>
                      <Text style={{ fontWeight: '700' }}>{t.fromName}</Text>
                      {'  →  '}
                      <Text style={{ fontWeight: '700' }}>{t.toName}</Text>
                    </Text>
                    <Text style={[s.amount, { color: colors.primary }]}>{formatVND(t.amount)}</Text>
                  </View>
                  <Button
                    title="Đánh dấu đã trả"
                    variant="secondary"
                    onPress={() => markPaid(t, i)}
                    loading={busyKey === `t${i}`}
                  />
                </Card>
              ))
            )}
            {payments.length > 0 && (
              <>
                <SectionTitle>Đã trả</SectionTitle>
                {payments.map((p) => (
                  <Card key={p.id} style={s.rowBetween}>
                    <View style={{ flex: 1 }}>
                      <Text style={s.transferText}>
                        {p.fromName} → {p.toName}
                      </Text>
                      <Text style={s.muted}>{formatDate(p.createdAt)}</Text>
                    </View>
                    <View style={{ alignItems: 'flex-end', gap: 4 }}>
                      <Text style={s.amount}>{formatVND(p.amount)}</Text>
                      <Pressable onPress={() => undoPayment(p)} hitSlop={8}>
                        <Text style={{ color: colors.danger }}>Hủy</Text>
                      </Pressable>
                    </View>
                  </Card>
                ))}
              </>
            )}
          </>
        )}

        {tab === 'balances' &&
          balances.map((b) => (
            <Card key={b.memberId} style={s.rowBetween}>
              <View style={{ flex: 1 }}>
                <Text style={s.name}>{b.name}</Text>
                <Text style={s.muted}>
                  Đã trả {formatVND(b.paid)} · Phải chịu {formatVND(b.owed)}
                </Text>
                {(b.sent > 0 || b.received > 0) && (
                  <Text style={s.muted}>
                    {[
                      b.sent > 0 ? `Đã chuyển ${formatVND(b.sent)}` : null,
                      b.received > 0 ? `Đã nhận ${formatVND(b.received)}` : null,
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                  </Text>
                )}
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text
                  style={[
                    s.amount,
                    { color: b.balance > 0 ? colors.primary : b.balance < 0 ? colors.danger : colors.muted },
                  ]}
                >
                  {b.balance > 0 ? '+' : ''}
                  {formatVND(b.balance)}
                </Text>
                <Text style={s.muted}>{b.balance > 0 ? 'được nhận' : b.balance < 0 ? 'phải trả' : 'xong'}</Text>
              </View>
            </Card>
          ))}

        {tab === 'expenses' &&
          (expenses.length === 0 ? (
            <Empty>Chưa có khoản chi nào.</Empty>
          ) : (
            expenses.map((e) => (
              <Pressable key={e.id} onLongPress={() => deleteExpense(e)}>
                <Card style={{ gap: 4 }}>
                  <View style={s.rowBetween}>
                    <Text style={[s.name, { flex: 1 }]}>{e.description}</Text>
                    <Text style={s.amount}>{formatVND(e.amount)}</Text>
                  </View>
                  <Text style={s.muted}>
                    {e.paidBy.length === 1
                      ? `${e.paidBy[0].name} trả`
                      : e.paidBy.map((p) => `${p.name} ${formatVND(p.amount)}`).join(', ') + ' cùng trả'}
                  </Text>
                  <Text style={s.muted}>
                    Chia: {e.splitAmong.map((p) => `${p.name} ${formatVND(p.amount)}`).join(', ')}
                  </Text>
                  <View style={s.rowBetween}>
                    <Text style={s.muted}>{formatDate(e.createdAt)}</Text>
                    <Pressable onPress={() => deleteExpense(e)} hitSlop={8}>
                      <Text style={{ color: colors.danger }}>Xóa</Text>
                    </Pressable>
                  </View>
                </Card>
              </Pressable>
            ))
          ))}

        {group && (
          <Button title="Xóa nhóm" variant="danger" onPress={deleteGroup} style={{ marginTop: 24 }} />
        )}
      </ScrollView>

      <View style={[s.footer, { paddingBottom: insets.bottom + 12 }]}>
        <Button
          title="+ Thêm khoản chi"
          onPress={() => router.push(`/group/${groupId}/add-expense`)}
          disabled={!group || group.members.length === 0}
        />
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  muted: { color: colors.muted, fontSize: 14 },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  amount: { fontSize: 16, fontWeight: '700', color: colors.text },
  bigAmount: { fontSize: 22, fontWeight: '800', color: colors.text },
  transferText: { fontSize: 16, color: colors.text },
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
