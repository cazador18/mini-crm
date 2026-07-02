'use client';

import { useState, useEffect } from 'react';
import type { ClientInput } from '@/lib/api';

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
    'border border-gray-300 rounded px-3 py-2 w-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3 sm:items-end"
    >
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Имя *</label>
        <input
          className={inputCls}
          value={name}
          onChange={e => setName(e.target.value)}
          required
          placeholder="Иван Иванов"
        />
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Email *</label>
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
        <label className="block text-xs font-medium text-gray-600 mb-1">Телефон</label>
        <input
          className={inputCls}
          value={phone}
          onChange={e => setPhone(e.target.value)}
          placeholder="+7 999 000-00-00"
        />
      </div>
      <div className="sm:col-span-3 flex gap-2 pt-1">
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
