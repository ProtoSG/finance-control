import React, { useCallback, useEffect, useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TouchableOpacity,
  RefreshControl, ActivityIndicator, Alert,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { getSummary, syncGmail } from '../api/client';
import { Category, Summary } from '../types';
import { categoryColor, categoryLabel } from '../components/CategoryIcon';

const MONTHS = [
  'Enero','Febrero','Marzo','Abril','Mayo','Junio',
  'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre',
];

export default function DashboardScreen() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1); // 1-based
  const [summary, setSummary] = useState<Summary | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const fetchSummary = useCallback(async () => {
    try {
      const monthStr = `${year}-${String(month).padStart(2, '0')}`;
      const res = await getSummary(monthStr);
      setSummary(res.data);
    } catch {
      Alert.alert('Error', 'No se pudo cargar el resumen. ¿Está el backend corriendo?');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [year, month]);

  useEffect(() => { setLoading(true); fetchSummary(); }, [fetchSummary]);

  const onRefresh = () => { setRefreshing(true); fetchSummary(); };

  const handleSync = async () => {
    setSyncing(true);
    try {
      await syncGmail();
      Alert.alert('Sincronización', 'Emails de BCP sincronizados correctamente');
      fetchSummary();
    } catch {
      Alert.alert('Error', 'No se pudo sincronizar con Gmail');
    } finally {
      setSyncing(false);
    }
  };

  const prevMonth = () => {
    if (month === 1) { setMonth(12); setYear(y => y - 1); }
    else setMonth(m => m - 1);
  };

  const nextMonth = () => {
    if (month === 12) { setMonth(1); setYear(y => y + 1); }
    else setMonth(m => m + 1);
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#fff" />}
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Finance Control</Text>
        <TouchableOpacity onPress={handleSync} disabled={syncing} style={styles.syncBtn}>
          {syncing
            ? <ActivityIndicator color="#fff" size="small" />
            : <MaterialCommunityIcons name="sync" size={22} color="#fff" />
          }
        </TouchableOpacity>
      </View>

      {/* Selector de mes */}
      <View style={styles.monthSelector}>
        <TouchableOpacity onPress={prevMonth}>
          <MaterialCommunityIcons name="chevron-left" size={28} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.monthText}>{MONTHS[month - 1]} {year}</Text>
        <TouchableOpacity onPress={nextMonth}>
          <MaterialCommunityIcons name="chevron-right" size={28} color="#fff" />
        </TouchableOpacity>
      </View>

      {/* Tarjetas gastos / ingresos */}
      <View style={styles.cardsRow}>
        <View style={[styles.totalCard, styles.cardHalf]}>
          <Text style={styles.totalLabel}>Total gastado</Text>
          {loading
            ? <ActivityIndicator color="#FF6B6B" size="small" />
            : <Text style={[styles.totalAmount, { color: '#FF6B6B' }]}>
                S/ {summary?.totalSpent?.toFixed(2) ?? '0.00'}
              </Text>
          }
          <Text style={styles.totalSub}>{summary?.transactionCount ?? 0} gastos</Text>
        </View>
        <View style={[styles.totalCard, styles.cardHalf]}>
          <Text style={styles.totalLabel}>Total ingresado</Text>
          {loading
            ? <ActivityIndicator color="#00B894" size="small" />
            : <Text style={[styles.totalAmount, { color: '#00B894' }]}>
                S/ {summary?.totalIncome?.toFixed(2) ?? '0.00'}
              </Text>
          }
          <Text style={styles.totalSub}>{summary?.incomeCount ?? 0} ingresos</Text>
        </View>
      </View>

      {/* Por categoría */}
      {!loading && summary && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Por categoría</Text>
          {Object.entries(summary.byCategory)
            .sort(([, a], [, b]) => b - a)
            .map(([cat, amount]) => {
              const pct = summary.totalSpent > 0
                ? ((amount / summary.totalSpent) * 100).toFixed(0)
                : '0';
              const color = categoryColor(cat as Category);
              return (
                <View key={cat} style={styles.categoryRow}>
                  <View style={[styles.categoryDot, { backgroundColor: color }]} />
                  <Text style={styles.categoryName}>{categoryLabel(cat as Category)}</Text>
                  <View style={styles.categoryBar}>
                    <View style={[styles.categoryBarFill, { width: `${pct}%`, backgroundColor: color }]} />
                  </View>
                  <Text style={styles.categoryAmount}>S/ {amount.toFixed(2)}</Text>
                </View>
              );
            })}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: '#1A1A2E' },
  header:          { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, paddingTop: 50 },
  headerTitle:     { fontSize: 22, fontWeight: '700', color: '#fff' },
  syncBtn:         { backgroundColor: '#16213E', borderRadius: 20, padding: 8 },
  monthSelector:   { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 20, marginBottom: 10 },
  monthText:       { color: '#fff', fontSize: 16, fontWeight: '600', width: 160, textAlign: 'center' },
  cardsRow:        { flexDirection: 'row', marginHorizontal: 20, marginBottom: 0, gap: 12 },
  cardHalf:        { flex: 1, margin: 0 },
  totalCard:       { backgroundColor: '#16213E', marginVertical: 20, borderRadius: 20, padding: 18, alignItems: 'center' },
  totalLabel:      { color: '#B2BEC3', fontSize: 12, marginBottom: 8 },
  totalAmount:     { fontSize: 26, fontWeight: '800' },
  totalSub:        { color: '#636E72', fontSize: 12, marginTop: 4 },
  section:         { backgroundColor: '#16213E', margin: 20, marginTop: 0, borderRadius: 20, padding: 20 },
  sectionTitle:    { color: '#fff', fontSize: 16, fontWeight: '700', marginBottom: 16 },
  categoryRow:     { flexDirection: 'row', alignItems: 'center', marginBottom: 12, gap: 8 },
  categoryDot:     { width: 10, height: 10, borderRadius: 5 },
  categoryName:    { color: '#DFE6E9', fontSize: 13, width: 110 },
  categoryBar:     { flex: 1, height: 6, backgroundColor: '#2D3436', borderRadius: 3, overflow: 'hidden' },
  categoryBarFill: { height: 6, borderRadius: 3 },
  categoryAmount:  { color: '#fff', fontSize: 13, fontWeight: '600', width: 75, textAlign: 'right' },
});
