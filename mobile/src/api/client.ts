import axios from 'axios';

// En emulador Android usa 10.0.2.2:8080
// En dispositivo físico usa la IP local de tu PC (ej: 192.168.1.X)
const DEV_URL = 'http://192.168.18.58:8080/api';
const BASE_URL = __DEV__ ? DEV_URL : 'https://tu-servidor.com/api';

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

export const getTransactions = () => api.get('/transactions');

export const getTransactionsByPeriod = (from: string, to: string) =>
  api.get('/transactions', { params: { from, to } });

export const getSummary = (month?: string) =>
  api.get('/summary', { params: { month } });

export const createTransaction = (data: object) =>
  api.post('/transactions', data);

export const updateTransaction = (id: number, data: object) =>
  api.put(`/transactions/${id}`, data);

export const deleteTransaction = (id: number) =>
  api.delete(`/transactions/${id}`);

export const syncGmail = () => api.post('/sync', {}, { timeout: 120000 });
