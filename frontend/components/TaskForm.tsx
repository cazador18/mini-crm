'use client';

import { useState, useEffect } from 'react';
import type { Client, TaskInput } from '@/lib/api';

const STATUS_LABELS: Record<string, string> = {
  NEW: 'Новая',
  IN_PROGRESS: 'В работе',
  DONE: 'Готово',
};

const PRIORITY_LABELS: Record<string, string> = {
  LOW: 'Низкий',
  MEDIUM: 'Средний',
  HIGH: 'Высокий',
};

type Props = {
  initial?: TaskInput;
  clients: Client[];
  onSubmit: (data: TaskInput) => Promise<void>;
  onCancel: () => void;
  saving: boolean;
};

export default function TaskForm({ initial, clients, onSubmit, onCancel, saving }: Props) {
  const [title, setTitle]           = useState(initial?.title ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [status, setStatus]         = useState<string>(initial?.status ?? 'NEW');
  const [priority, setPriority]     = useState<string>(initial?.priority ?? 'MEDIUM');
  const [deadline, setDeadline]     = useState(initial?.deadline ?? '');
  const [clientId, setClientId]     = useState<string>(initial?.clientId ? String(initial.clientId) : '');

  useEffect(() => {
    setTitle(initial?.title ?? '');
    setDescription(initial?.description ?? '');
    setStatus(initial?.status ?? 'NEW');
    setPriority(initial?.priority ?? 'MEDIUM');
    setDeadline(initial?.deadline ?? '');
    setClientId(initial?.clientId ? String(initial.clientId) : '');
  }, [initial]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      title,
      description: description || undefined,
      status: status as TaskInput['status'],
      priority: priority as TaskInput['priority'],
      deadline: deadline || undefined,
      clientId: Number(clientId),
    });
  };

  const inputCls =
    'border border-gray-300 rounded px-3 py-2 w-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white';

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3 sm:items-end"
    >
      <div className="sm:col-span-2">
        <label className="block text-xs font-medium text-gray-600 mb-1">Заголовок *</label>
        <input
          className={inputCls}
          value={title}
          onChange={e => setTitle(e.target.value)}
          required
          placeholder="Описание задачи"
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Клиент *</label>
        <select
          className={inputCls}
          value={clientId}
          onChange={e => setClientId(e.target.value)}
          required
        >
          <option value="">— выберите —</option>
          {clients.map(c => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Статус</label>
        <select className={inputCls} value={status} onChange={e => setStatus(e.target.value)}>
          {Object.entries(STATUS_LABELS).map(([val, label]) => (
            <option key={val} value={val}>{label}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Приоритет</label>
        <select className={inputCls} value={priority} onChange={e => setPriority(e.target.value)}>
          {Object.entries(PRIORITY_LABELS).map(([val, label]) => (
            <option key={val} value={val}>{label}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Дедлайн</label>
        <input
          className={inputCls}
          type="date"
          value={deadline}
          onChange={e => setDeadline(e.target.value)}
        />
      </div>

      <div className="sm:col-span-3">
        <label className="block text-xs font-medium text-gray-600 mb-1">Описание</label>
        <textarea
          className={inputCls + ' resize-none'}
          rows={2}
          value={description}
          onChange={e => setDescription(e.target.value)}
          placeholder="Дополнительные детали…"
        />
      </div>

      <div className="sm:col-span-3 flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'Сохранение…' : initial ? 'Сохранить' : 'Создать'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 rounded text-sm border border-gray-300 hover:bg-gray-100"
        >
          Отмена
        </button>
      </div>
    </form>
  );
}
