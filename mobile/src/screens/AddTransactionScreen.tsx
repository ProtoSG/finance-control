import React, { useState } from 'react';
import {
  View, Text, TextInput, StyleSheet, TouchableOpacity,
  ScrollView, Alert, ActivityIndicator,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { createTransaction } from '../api/client';
import { Category, TransactionType } from '../types';
import { CATEGORY_CONFIG } from '../components/CategoryIcon';

const EXPENSE_CATEGORIES: Category[] = [
  'ALIMENTACION', 'TRANSPORTE', 'ENTRETENIMIENTO', 'SALUD',
  'EDUCACION', 'SERVICIOS', 'COMPRAS', 'TRANSFERENCIA', 'OTROS',
];
const INCOME_CATEGORIES: Category[] = ['SALARIO', 'FREELANCE', 'TRANSFERENCIA', 'OTROS'];

interface Props {
  navigation: any;
  route: any;
}

export default function AddTransactionScreen({ navigation, route }: Props) {
  const [type, setType] = useState<TransactionType>('EXPENSE');
  const [amount, setAmount] = useState('');
  const [merchant, setMerchant] = useState('');
  const [category, setCategory] = useState<Category>('OTROS');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);

  const categories = type === 'INCOME' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

  const handleTypeChange = (newType: TransactionType) => {
    setType(newType);
    setCategory(newType === 'INCOME' ? 'SALARIO' : 'OTROS');
  };

  const handleSave = async () => {
    if (!amount || !merchant) {
      Alert.alert('Campos requeridos', 'Ingresa el monto y el comercio');
      return;
    }
    const parsed = parseFloat(amount.replace(',', '.'));
    if (isNaN(parsed) || parsed <= 0) {
      Alert.alert('Monto inválido', 'Ingresa un monto válido');
      return;
    }

    setLoading(true);
    try {
      await createTransaction({
        amount: parsed,
        merchant,
        category,
        notes,
        type,
        transactionDate: new Date().toISOString(),
        operationType: 'Manual',
      });
      route.params?.onSaved?.();
      navigation.goBack();
    } catch {
      Alert.alert('Error', 'No se pudo guardar la transacción');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container} keyboardShouldPersistTaps="handled">
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <MaterialCommunityIcons name="arrow-left" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.title}>Nueva transacción</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.form}>
        {/* Toggle Gasto / Ingreso */}
        <View style={styles.toggle}>
          <TouchableOpacity
            style={[styles.toggleBtn, type === 'EXPENSE' && styles.toggleExpenseActive]}
            onPress={() => handleTypeChange('EXPENSE')}
          >
            <MaterialCommunityIcons
              name="arrow-up-circle"
              size={18}
              color={type === 'EXPENSE' ? '#fff' : '#636E72'}
            />
            <Text style={[styles.toggleText, type === 'EXPENSE' && styles.toggleTextActive]}>
              Gasto
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.toggleBtn, type === 'INCOME' && styles.toggleIncomeActive]}
            onPress={() => handleTypeChange('INCOME')}
          >
            <MaterialCommunityIcons
              name="arrow-down-circle"
              size={18}
              color={type === 'INCOME' ? '#fff' : '#636E72'}
            />
            <Text style={[styles.toggleText, type === 'INCOME' && styles.toggleTextActive]}>
              Ingreso
            </Text>
          </TouchableOpacity>
        </View>

        <Text style={styles.label}>Monto (S/)</Text>
        <TextInput
          style={[styles.input, { borderColor: type === 'INCOME' ? '#00B894' : '#FF6B6B' }]}
          value={amount}
          onChangeText={setAmount}
          keyboardType="decimal-pad"
          placeholder="0.00"
          placeholderTextColor="#636E72"
        />

        <Text style={styles.label}>{type === 'INCOME' ? 'Fuente / Descripción' : 'Comercio / Descripción'}</Text>
        <TextInput
          style={styles.input}
          value={merchant}
          onChangeText={setMerchant}
          placeholder={type === 'INCOME' ? 'Ej: Empresa SAC' : 'Ej: Starbucks'}
          placeholderTextColor="#636E72"
        />

        <Text style={styles.label}>Categoría</Text>
        <View style={styles.categoriesGrid}>
          {categories.map(cat => {
            const cfg = CATEGORY_CONFIG[cat];
            const selected = category === cat;
            return (
              <TouchableOpacity
                key={cat}
                style={[styles.catChip, selected && { backgroundColor: cfg.color + '33', borderColor: cfg.color }]}
                onPress={() => setCategory(cat)}
              >
                <MaterialCommunityIcons name={cfg.icon as any} size={18} color={cfg.color} />
                <Text style={[styles.catLabel, selected && { color: cfg.color }]}>{cfg.label}</Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <Text style={styles.label}>Notas (opcional)</Text>
        <TextInput
          style={[styles.input, { height: 80 }]}
          value={notes}
          onChangeText={setNotes}
          placeholder="Notas..."
          placeholderTextColor="#636E72"
          multiline
        />

        <TouchableOpacity
          style={[styles.saveBtn, { backgroundColor: type === 'INCOME' ? '#00B894' : '#4ECDC4' }]}
          onPress={handleSave}
          disabled={loading}
        >
          {loading
            ? <ActivityIndicator color="#fff" />
            : <Text style={styles.saveBtnText}>Guardar {type === 'INCOME' ? 'ingreso' : 'gasto'}</Text>
          }
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:         { flex: 1, backgroundColor: '#1A1A2E' },
  header:            { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, paddingTop: 50 },
  title:             { color: '#fff', fontSize: 18, fontWeight: '700' },
  form:              { padding: 20, gap: 8 },
  toggle:            { flexDirection: 'row', backgroundColor: '#16213E', borderRadius: 14, padding: 4, marginBottom: 8 },
  toggleBtn:         { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, paddingVertical: 10, borderRadius: 10 },
  toggleExpenseActive: { backgroundColor: '#FF6B6B' },
  toggleIncomeActive:  { backgroundColor: '#00B894' },
  toggleText:        { color: '#636E72', fontSize: 14, fontWeight: '600' },
  toggleTextActive:  { color: '#fff' },
  label:             { color: '#B2BEC3', fontSize: 13, marginBottom: 4, marginTop: 12 },
  input:             { backgroundColor: '#16213E', color: '#fff', borderRadius: 12, padding: 14, fontSize: 16, borderWidth: 1.5, borderColor: 'transparent' },
  categoriesGrid:    { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
  catChip:           { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#16213E', borderRadius: 20, paddingHorizontal: 12, paddingVertical: 8, borderWidth: 1.5, borderColor: '#2D3436' },
  catLabel:          { color: '#B2BEC3', fontSize: 12 },
  saveBtn:           { borderRadius: 14, padding: 16, alignItems: 'center', marginTop: 28 },
  saveBtnText:       { color: '#fff', fontWeight: '800', fontSize: 16 },
});
