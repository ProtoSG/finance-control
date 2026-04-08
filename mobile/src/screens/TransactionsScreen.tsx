import React, { useCallback, useEffect, useState } from 'react';
import {
  View, Text, FlatList, StyleSheet, TouchableOpacity,
  RefreshControl, Alert,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { deleteTransaction, getTransactions } from '../api/client';
import { Transaction } from '../types';
import { CategoryIcon, categoryLabel } from '../components/CategoryIcon';

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

interface Props {
  navigation: any;
}

export default function TransactionsScreen({ navigation }: Props) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchTransactions = useCallback(async () => {
    try {
      const res = await getTransactions();
      setTransactions(res.data);
    } catch {
      Alert.alert('Error', 'No se pudieron cargar las transacciones');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { fetchTransactions(); }, [fetchTransactions]);

  const onRefresh = () => { setRefreshing(true); fetchTransactions(); };

  const handleDelete = (id: number) => {
    Alert.alert('Eliminar', '¿Eliminar esta transacción?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Eliminar', style: 'destructive',
        onPress: async () => {
          await deleteTransaction(id);
          setTransactions(prev => prev.filter(t => t.id !== id));
        },
      },
    ]);
  };

  const renderItem = ({ item }: { item: Transaction }) => (
    <TouchableOpacity
      style={styles.card}
      onLongPress={() => handleDelete(item.id)}
    >
      <View style={styles.cardLeft}>
        <CategoryIcon category={item.category} size={28} />
      </View>
      <View style={styles.cardMid}>
        <Text style={styles.merchant} numberOfLines={1}>{item.merchant}</Text>
        <Text style={styles.meta}>
          {categoryLabel(item.category)} · {formatDate(item.transactionDate)}
        </Text>
        {item.source === 'BCP_EMAIL' && (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>BCP</Text>
          </View>
        )}
      </View>
      <View style={styles.cardRight}>
        <Text style={[styles.amount, item.type === 'INCOME' && { color: '#00B894' }]}>
          {item.type === 'INCOME' ? '+' : '-'}S/ {item.amount.toFixed(2)}
        </Text>
        <Text style={styles.time}>{formatTime(item.transactionDate)}</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Transacciones</Text>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => navigation.navigate('AddTransaction', { onSaved: fetchTransactions })}
        >
          <MaterialCommunityIcons name="plus" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      <FlatList
        data={transactions}
        keyExtractor={item => item.id.toString()}
        renderItem={renderItem}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#fff" />}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.empty}>
              <MaterialCommunityIcons name="inbox-outline" size={48} color="#636E72" />
              <Text style={styles.emptyText}>Sin transacciones</Text>
              <Text style={styles.emptySubText}>Sincroniza tus emails de BCP o agrega una manualmente</Text>
            </View>
          ) : null
        }
        contentContainerStyle={{ paddingBottom: 20 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#1A1A2E' },
  header:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, paddingTop: 50 },
  headerTitle:  { fontSize: 22, fontWeight: '700', color: '#fff' },
  addBtn:       { backgroundColor: '#4ECDC4', borderRadius: 20, padding: 8 },
  card:         { flexDirection: 'row', backgroundColor: '#16213E', marginHorizontal: 16, marginVertical: 5, borderRadius: 14, padding: 14, alignItems: 'center', gap: 12 },
  cardLeft:     { width: 44, height: 44, backgroundColor: '#0F3460', borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
  cardMid:      { flex: 1 },
  cardRight:    { alignItems: 'flex-end' },
  merchant:     { color: '#fff', fontWeight: '600', fontSize: 14 },
  meta:         { color: '#636E72', fontSize: 12, marginTop: 2 },
  badge:        { backgroundColor: '#0F3460', borderRadius: 6, paddingHorizontal: 6, paddingVertical: 2, alignSelf: 'flex-start', marginTop: 4 },
  badgeText:    { color: '#4ECDC4', fontSize: 10, fontWeight: '700' },
  amount:       { color: '#FF6B6B', fontWeight: '700', fontSize: 15 },
  time:         { color: '#636E72', fontSize: 11, marginTop: 2 },
  empty:        { alignItems: 'center', marginTop: 80, gap: 8, paddingHorizontal: 40 },
  emptyText:    { color: '#DFE6E9', fontSize: 16, fontWeight: '600' },
  emptySubText: { color: '#636E72', fontSize: 13, textAlign: 'center' },
});
