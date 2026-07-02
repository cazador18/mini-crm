import type { Task } from './api';

type StatusThemeEntry = {
  label: string;
  badge: string;
  dot: string;
  chipBg: string;
  chipText: string;
  gradient: string;
};

export const STATUS_THEME: Record<Task['status'], StatusThemeEntry> = {
  NEW: {
    label: 'Новая',
    badge: 'bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20',
    dot: 'bg-blue-500',
    chipBg: 'bg-blue-50',
    chipText: 'text-blue-600',
    gradient: 'from-blue-50/60 to-white',
  },
  IN_PROGRESS: {
    label: 'В работе',
    badge: 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20',
    dot: 'bg-amber-500',
    chipBg: 'bg-amber-50',
    chipText: 'text-amber-600',
    gradient: 'from-amber-50/60 to-white',
  },
  DONE: {
    label: 'Готово',
    badge: 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20',
    dot: 'bg-emerald-500',
    chipBg: 'bg-emerald-50',
    chipText: 'text-emerald-600',
    gradient: 'from-emerald-50/60 to-white',
  },
};

type PriorityThemeEntry = {
  label: string;
  badge: string;
  dot: string;
};

export const PRIORITY_THEME: Record<Task['priority'], PriorityThemeEntry> = {
  LOW: {
    label: 'Низкий',
    badge: 'bg-slate-100 text-slate-600 ring-1 ring-inset ring-slate-500/20',
    dot: 'bg-slate-400',
  },
  MEDIUM: {
    label: 'Средний',
    badge: 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20',
    dot: 'bg-amber-500',
  },
  HIGH: {
    label: 'Высокий',
    badge: 'bg-rose-50 text-rose-700 ring-1 ring-inset ring-rose-600/20',
    dot: 'bg-rose-500',
  },
};

export const STATUS_ORDER: Task['status'][] = ['NEW', 'IN_PROGRESS', 'DONE'];
export const PRIORITY_ORDER: Task['priority'][] = ['LOW', 'MEDIUM', 'HIGH'];
