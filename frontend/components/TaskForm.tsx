'use client';

import { useState, useEffect } from 'react';
import type { Client, TaskInput } from '@/lib/api';
import { PRIORITY_ORDER, PRIORITY_THEME, STATUS_ORDER, STATUS_THEME } from '@/lib/theme';
import { CheckIcon, XIcon } from '@/components/icons';

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
    'w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500';
  const labelCls = 'mb-1.5 block text-sm font-medium text-gray-700';

  return (
    <form
      onSubmit={handleSubmit}
      className="mb-6 grid grid-cols-1 gap-4 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm sm:grid-cols-3 sm:items-end"
    >
      <div className="sm:col-span-2">
        <label className={labelCls}>Заголовок *</label>
        <input
          className={inputCls}
          value={title}
          onChange={e => setTitle(e.target.value)}
          required
          placeholder="Описание задачи"
        />
      </div>

      <div>
        <label className={labelCls}>Клиент *</label>
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
        <label className={labelCls}>Статус</label>
        <select className={inputCls} value={status} onChange={e => setStatus(e.target.value)}>
          {STATUS_ORDER.map(s => (
            <option key={s} value={s}>{STATUS_THEME[s].label}</option>
          ))}
        </select>
      </div>

      <div>
        <label className={labelCls}>Приоритет</label>
        <select className={inputCls} value={priority} onChange={e => setPriority(e.target.value)}>
          {PRIORITY_ORDER.map(p => (
            <option key={p} value={p}>{PRIORITY_THEME[p].label}</option>
          ))}
        </select>
      </div>

      <div>
        <label className={labelCls}>Дедлайн</label>
        <input
          className={inputCls}
          type="date"
          value={deadline}
          onChange={e => setDeadline(e.target.value)}
        />
      </div>

      <div className="sm:col-span-3">
        <label className={labelCls}>Описание</label>
        <textarea
          className={inputCls + ' resize-none'}
          rows={2}
          value={description}
          onChange={e => setDescription(e.target.value)}
          placeholder="Дополнительные детали…"
        />
      </div>

      <div className="flex gap-2 sm:col-span-3">
        <button
          type="submit"
          disabled={saving}
          className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-indigo-700 disabled:opacity-50"
        >
          <CheckIcon className="h-4 w-4" />
          {saving ? 'Сохранение…' : initial ? 'Сохранить' : 'Создать'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
        >
          <XIcon className="h-4 w-4" />
          Отмена
        </button>
      </div>
    </form>
  );
}
