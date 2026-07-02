'use client';

import { useState, useEffect } from 'react';
import type { ClientInput } from '@/lib/api';
import { CheckIcon, XIcon } from '@/components/icons';

type Props = {
  initial?: ClientInput;
  onSubmit: (data: ClientInput) => Promise<void>;
  onCancel: () => void;
  saving: boolean;
};

export default function ClientForm({ initial, onSubmit, onCancel, saving }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [email, setEmail] = useState(initial?.email ?? '');
  const [phone, setPhone] = useState(initial?.phone ?? '');

  useEffect(() => {
    setName(initial?.name ?? '');
    setEmail(initial?.email ?? '');
    setPhone(initial?.phone ?? '');
  }, [initial]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ name, email, phone: phone || undefined });
  };

  const inputCls =
    'w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500';
  const labelCls = 'mb-1.5 block text-sm font-medium text-gray-700';

  return (
    <form
      onSubmit={handleSubmit}
      className="mb-6 grid grid-cols-1 gap-4 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm sm:grid-cols-3 sm:items-end"
    >
      <div>
        <label className={labelCls}>Имя *</label>
        <input
          className={inputCls}
          value={name}
          onChange={e => setName(e.target.value)}
          required
          placeholder="Иван Иванов"
        />
      </div>
      <div>
        <label className={labelCls}>Email *</label>
        <input
          className={inputCls}
          type="email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
          placeholder="ivan@example.com"
        />
      </div>
      <div>
        <label className={labelCls}>Телефон</label>
        <input
          className={inputCls}
          value={phone}
          onChange={e => setPhone(e.target.value)}
          placeholder="+7 999 000-00-00"
        />
      </div>
      <div className="flex gap-2 pt-1 sm:col-span-3">
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
