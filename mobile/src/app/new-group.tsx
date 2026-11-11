import { router } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, Text, View } from 'react-native';

import { Button, colors, Input } from '@/components/ui';
import { api } from '@/lib/api';
import { showError } from '@/lib/format';

export default function NewGroupScreen() {
  const [name, setName] = useState('');
  const [members, setMembers] = useState<string[]>(['', '']);
  const [saving, setSaving] = useState(false);

  const names = members.map((m) => m.trim()).filter(Boolean);
  const canSave = name.trim().length > 0 && names.length >= 2;

  const save = async () => {
    setSaving(true);
    try {
      const g = await api.createGroup(name.trim(), names);
      router.replace(`/group/${g.id}`);
    } catch (e) {
      showError(e);
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 16 }} keyboardShouldPersistTaps="handled">
        <Input label="Tên nhóm" placeholder="VD: Đi Đà Lạt" value={name} onChangeText={setName} autoFocus />

        <View style={{ gap: 8 }}>
          <Text style={{ fontWeight: '600', color: colors.text }}>Thành viên (ít nhất 2 người)</Text>
          {members.map((m, i) => (
            <View key={i} style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
              <View style={{ flex: 1 }}>
                <Input
                  placeholder={`Người ${i + 1}`}
                  value={m}
                  onChangeText={(t) => setMembers((prev) => prev.map((x, j) => (j === i ? t : x)))}
                  returnKeyType="next"
                  onSubmitEditing={() => i === members.length - 1 && setMembers((p) => [...p, ''])}
                />
              </View>
              {members.length > 2 && (
                <Pressable onPress={() => setMembers((prev) => prev.filter((_, j) => j !== i))} hitSlop={8}>
                  <Text style={{ color: colors.danger, fontSize: 22 }}>×</Text>
                </Pressable>
              )}
            </View>
          ))}
          <Button title="+ Thêm người" variant="secondary" onPress={() => setMembers((p) => [...p, ''])} />
        </View>

        <Button title="Tạo nhóm" onPress={save} disabled={!canSave} loading={saving} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
