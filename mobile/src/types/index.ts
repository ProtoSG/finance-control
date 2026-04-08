export type Category =
  | 'ALIMENTACION'
  | 'TRANSPORTE'
  | 'ENTRETENIMIENTO'
  | 'SALUD'
  | 'EDUCACION'
  | 'SERVICIOS'
  | 'COMPRAS'
  | 'TRANSFERENCIA'
  | 'OTROS'
  | 'SALARIO'
  | 'FREELANCE';

export type TransactionSource = 'BCP_EMAIL' | 'MANUAL';

export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: number;
  amount: number;
  merchant: string;
  operationType: string;
  transactionDate: string; // ISO string
  cardNumber?: string;
  operationNumber?: string;
  source: TransactionSource;
  type: TransactionType;
  category: Category;
  notes?: string;
  createdAt: string;
}

export interface Summary {
  totalSpent: number;
  transactionCount: number;
  totalIncome: number;
  incomeCount: number;
  byCategory: Partial<Record<Category, number>>;
  period: string;
}
