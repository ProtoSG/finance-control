import React from 'react';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Category } from '../types';

const CATEGORY_CONFIG: Record<Category, { icon: string; color: string; label: string }> = {
  ALIMENTACION:    { icon: 'food',               color: '#FF6B6B', label: 'Alimentación' },
  TRANSPORTE:      { icon: 'car',                color: '#4ECDC4', label: 'Transporte' },
  ENTRETENIMIENTO: { icon: 'gamepad-variant',    color: '#45B7D1', label: 'Entretenimiento' },
  SALUD:           { icon: 'heart-pulse',        color: '#96CEB4', label: 'Salud' },
  EDUCACION:       { icon: 'school',             color: '#FFEAA7', label: 'Educación' },
  SERVICIOS:       { icon: 'lightning-bolt',     color: '#DDA0DD', label: 'Servicios' },
  COMPRAS:         { icon: 'shopping',           color: '#F0A500', label: 'Compras' },
  TRANSFERENCIA:   { icon: 'bank-transfer',      color: '#74B9FF', label: 'Transferencia' },
  OTROS:           { icon: 'dots-horizontal',    color: '#B2BEC3', label: 'Otros' },
  SALARIO:         { icon: 'cash',               color: '#00B894', label: 'Salario' },
  FREELANCE:       { icon: 'laptop',             color: '#6C5CE7', label: 'Freelance' },
};

interface Props {
  category: Category;
  size?: number;
}

export function CategoryIcon({ category, size = 24 }: Props) {
  const { icon, color } = CATEGORY_CONFIG[category] ?? CATEGORY_CONFIG.OTROS;
  return <MaterialCommunityIcons name={icon as any} size={size} color={color} />;
}

export function categoryLabel(category: Category): string {
  return CATEGORY_CONFIG[category]?.label ?? 'Otros';
}

export function categoryColor(category: Category): string {
  return CATEGORY_CONFIG[category]?.color ?? '#B2BEC3';
}

export { CATEGORY_CONFIG };
