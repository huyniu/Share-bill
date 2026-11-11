import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { Button, Card, Chip, colors, Input, SectionTitle, Segmented, styles as ui } from '@/components/ui';
import { api, CreateExpense, Member, SplitType } from '@/lib/api';
import { formatVND, parseAmount, showError } from '@/lib/format';

type PayMode = 'single' | 'multi';

export default function AddExpenseScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const groupId = Number(id);

  const [members, setMembers] = useState<Member[]>([]);
  const [description, setDescription] = useState('');
  const [amountText, setAmountText] = useState('');

  const [payMode, setPayMode] = useState<PayMode>('single');
  const [singlePayer, setSinglePayer] = useState<number | null>(null);
  const [payerAmounts, setPayerAmounts] = useState<Record<number, string>>({});

  const [splitType, setSplitType] = useState<SplitType>('EQUAL');
  const [equalSet, setEqualSet] = useState<Set<number>>(new Set());
  const [exactAmounts, setExactAmounts] = useState<Record<number, string>>({});
  const [shareCounts, setShareCounts] = useState<Record<number, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .getGroup(groupId)
      .then((g) => {
        setMembers(g.members);
        setEqualSet(new Set(g.members.map((m) => m.id)));
        setShareCounts(Object.fromEntries(g.members.map((m) => [m.id, '1'])));
        if (g.members.length > 0) setSinglePayer(g.members[0].id);
      })
      .catch(showError);
  }, [groupId]);

  const total = parseAmount(amountText);

  // ----- Tính trước để hiển thị và kiểm tra -----
  const payersSum = useMemo(
    () => members.reduce((sum, m) => sum + (parseAmount(payerAmounts[m.id] ?? '') ?? 0), 0),
    [members, payerAmounts],
  );
  const exactSum = useMemo(
    () => members.reduce((sum, m) => sum + (parseAmount(exactAmounts[m.id] ?? '') ?? 0), 0),
    [members, exactAmounts],
  );
  const shareTotal = useMemo(
    () => members.reduce((sum, m) => sum + (Number(shareCounts[m.id]) || 0), 0),
    [members, shareCounts],
  );

  const problem = ((): string | null => {
    if (!description.trim()) return 'Nhập nội dung khoản chi';
    if (!total) return 'Nhập số tiền hợp lệ (VD: 350000, 350k, 1,2tr)';
    if (payMode === 'single' && singlePayer == null) return 'Chọn người trả';
    if (payMode === 'multi') {
      if (payersSum === 0) return 'Nhập số tiền từng người đã trả';
      if (payersSum !== total) return `Người trả: ${formatVND(payersSum)} / ${formatVND(total)}`;
    }
    if (splitType === 'EQUAL' && equalSet.size === 0) return 'Chọn ít nhất một người để chia';
    if (splitType === 'EXACT' && exactSum !== total) return `Đã chia: ${formatVND(exactSum)} / ${formatVND(total)}`;
    if (splitType === 'SHARES' && shareTotal <= 0) return 'Nhập số phần cho ít nhất một người';
    return null;
  })();

  const save = async () => {
    if (problem || !total) return;
    const body: CreateExpense = {
      description: description.trim(),
      amount: total,
      splitType,
      paidBy:
        payMode === 'single'
          ? [{ memberId: singlePayer! }]
          : members
              .map((m) => ({ memberId: m.id, amount: parseAmount(payerAmounts[m.id] ?? '') ?? 0 }))
              .filter((p) => p.amount > 0),
      splitAmong:
        splitType === 'EQUAL'
          ? members.filter((m) => equalSet.has(m.id)).map((m) => ({ memberId: m.id }))
          : splitType === 'EXACT'
            ? members
                .map((m) => ({ memberId: m.id, value: parseAmount(exactAmounts[m.id] ?? '') ?? 0 }))
                .filter((p) => p.value > 0)
            : members
                .map((m) => ({ memberId: m.id, value: Math.floor(Number(shareCounts[m.id]) || 0) }))
                .filter((p) => p.value > 0),
    };
    setSaving(true);
    try {
      await api.addExpense(groupId, body);
      if (router.canGoBack()) router.back();
      else router.replace(`/group/${groupId}`);
    } catch (e) {
      showError(e);
      setSaving(false);
    }
  };

  const toggleEqual = (memberId: number) =>
    setEqualSet((prev) => {
      const next = new Set(prev);
      if (next.has(memberId)) next.delete(memberId);
      else next.add(memberId);
      return next;
    });

  const equalPreview = total && equalSet.size > 0 ? Math.floor(total / equalSet.size) : null;

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 14, paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
        <Input label="Nội dung" placeholder="VD: Lẩu, taxi, khách sạn" value={description} onChangeText={setDescription} />
        <Input
          label="Số tiền"
          placeholder="VD: 1200000 hoặc 1,2tr"
          value={amountText}
          onChangeText={setAmountText}
          keyboardType="numbers-and-punctuation"
        />
        {total ? <Text style={s.hint}>= {formatVND(total)}</Text> : null}

        {/* ----- Ai trả ----- */}
        <SectionTitle>Ai trả?</SectionTitle>
        <Segmented<PayMode>
          value={payMode}
          onChange={setPayMode}
          options={[
            { value: 'single', label: 'Một người' },
            { value: 'multi', label: 'Nhiều người' },
          ]}
        />
        {payMode === 'single' ? (
          <View style={s.wrap}>
            {members.map((m) => (
              <Chip key={m.id} label={m.name} selected={singlePayer === m.id} onPress={() => setSinglePayer(m.id)} />
            ))}
          </View>
        ) : (
          <Card style={{ gap: 10 }}>
            {members.map((m) => (
              <AmountRow
                key={m.id}
                name={m.name}
                value={payerAmounts[m.id] ?? ''}
                onChange={(t) => setPayerAmounts((p) => ({ ...p, [m.id]: t }))}
                placeholder="0"
              />
            ))}
            <Remaining current={payersSum} total={total} />
          </Card>
        )}

        {/* ----- Chia cho ai ----- */}
        <SectionTitle>Chia thế nào?</SectionTitle>
        <Segmented<SplitType>
          value={splitType}
          onChange={setSplitType}
          options={[
            { value: 'EQUAL', label: 'Chia đều' },
            { value: 'EXACT', label: 'Số tiền' },
            { value: 'SHARES', label: 'Theo phần' },
          ]}
        />

        {splitType === 'EQUAL' && (
          <>
            <View style={s.wrap}>
              {members.map((m) => (
                <Chip key={m.id} label={m.name} selected={equalSet.has(m.id)} onPress={() => toggleEqual(m.id)} />
              ))}
            </View>
            {equalPreview != null && (
              <Text style={s.hint}>
                Mỗi người khoảng {formatVND(equalPreview)} ({equalSet.size} người)
              </Text>
            )}
          </>
        )}

        {splitType === 'EXACT' && (
          <Card style={{ gap: 10 }}>
            {members.map((m) => (
              <AmountRow
                key={m.id}
                name={m.name}
                value={exactAmounts[m.id] ?? ''}
                onChange={(t) => setExactAmounts((p) => ({ ...p, [m.id]: t }))}
                placeholder="0"
              />
            ))}
            <Remaining current={exactSum} total={total} />
          </Card>
        )}

        {splitType === 'SHARES' && (
          <Card style={{ gap: 10 }}>
            {members.map((m) => {
              const count = Number(shareCounts[m.id]) || 0;
              return (
                <View key={m.id} style={s.amountRow}>
                  <Text style={s.rowName}>{m.name}</Text>
                  <Text style={s.hint}>
                    {total && shareTotal > 0 && count > 0 ? formatVND(Math.round((total * count) / shareTotal)) : ''}
                  </Text>
                  <TextInput
                    style={[ui.input, { width: 64, textAlign: 'center' }]}
                    value={shareCounts[m.id] ?? ''}
                    onChangeText={(t) => setShareCounts((p) => ({ ...p, [m.id]: t.replace(/\D/g, '') }))}
                    keyboardType="number-pad"
                    placeholder="0"
                  />
                </View>
              );
            })}
            <Text style={s.hint}>Ghi 0 cho người không tham gia. VD: người uống bia ghi 2, người không uống ghi 1.</Text>
          </Card>
        )}

        {problem ? <Text style={[s.hint, { color: colors.danger }]}>{problem}</Text> : null}
        <Button title="Lưu khoản chi" onPress={save} disabled={!!problem} loading={saving} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function AmountRow({
  name,
  value,
  onChange,
  placeholder,
}: {
  name: string;
  value: string;
  onChange: (t: string) => void;
  placeholder?: string;
}) {
  const parsed = parseAmount(value);
  return (
    <View style={s.amountRow}>
      <Text style={s.rowName}>{name}</Text>
      <Text style={s.hint}>{parsed ? formatVND(parsed) : ''}</Text>
      <TextInput
        style={[ui.input, { width: 130, textAlign: 'right' }]}
        value={value}
        onChangeText={onChange}
        keyboardType="numbers-and-punctuation"
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
      />
    </View>
  );
}

function Remaining({ current, total }: { current: number; total: number | null }) {
  if (!total) return null;
  const diff = total - current;
  const ok = diff === 0;
  return (
    <Text style={[s.hint, { color: ok ? colors.primary : colors.danger, textAlign: 'right' }]}>
      {ok ? '✓ Khớp tổng tiền' : diff > 0 ? `Còn thiếu ${formatVND(diff)}` : `Dư ${formatVND(-diff)}`}
    </Text>
  );
}

const s = StyleSheet.create({
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  hint: { color: colors.muted, fontSize: 14 },
  amountRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  rowName: { flex: 1, fontSize: 16, color: colors.text },
});
