'use client';

import { useEffect, useState } from 'react';
import { clientsApi, type Client, type ClientInput } from '@/lib/api';
import ClientForm from '@/components/ClientForm';
import { PencilIcon, PlusIcon, TrashIcon } from '@/components/icons';

type FormMode = { mode: 'create' } | { mode: 'edit'; client: Client } | null;

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formMode, setFormMode] = useState<FormMode>(null);
  const [saving, setSaving] = useState(false);

  const loadClients = async () => {
    try {
      setError(null);
      const data = await clientsApi.getAll();
      setClients(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка загрузки');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadClients(); }, []);

  const handleSubmit = async (data: ClientInput) => {
    setSaving(true);
    try {
      if (formMode?.mode === 'edit') {
        await clientsApi.update(formMode.client.id, data);
      } else {
        await clientsApi.create(data);
      }
      setFormMode(null);
      await loadClients();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка сохранения');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Удалить клиента «${name}»?`)) return;
    try {
      await clientsApi.delete(id);
      await loadClients();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка удаления');
    }
  };

  const formInitial =
    formMode?.mode === 'edit'
      ? { name: formMode.client.name, email: formMode.client.email, phone: formMode.client.phone ?? '' }
      : undefined;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold tracking-tight text-gray-900">Клиенты</h1>
        {!formMode && (
          <button
            onClick={() => setFormMode({ mode: 'create' })}
            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-indigo-700"
          >
            <PlusIcon className="h-4 w-4" />
            Новый клиент
          </button>
        )}
      </div>

      {formMode && (
        <ClientForm
          initial={formInitial}
          onSubmit={handleSubmit}
          onCancel={() => setFormMode(null)}
          saving={saving}
        />
      )}

      {error && (
        <div className="bg-red-50 text-red-700 border border-red-200 rounded-xl p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Загрузка…</p>
      ) : clients.length === 0 ? (
        <p className="text-sm text-gray-500">Клиентов пока нет.</p>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-100 text-sm">
            <thead className="bg-gray-50/60">
              <tr>
                {['Имя', 'Email', 'Телефон', 'Дата добавления', ''].map(h => (
                  <th
                    key={h}
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {clients.map(client => (
                <tr key={client.id} className="transition-colors hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700">
                        {getInitials(client.name)}
                      </span>
                      <span className="font-medium text-gray-900">{client.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{client.email}</td>
                  <td className="px-4 py-3 text-gray-600">{client.phone ?? '—'}</td>
                  <td className="px-4 py-3 text-gray-400">
                    {new Date(client.createdAt).toLocaleDateString('ru-RU')}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setFormMode({ mode: 'edit', client })}
                        aria-label={`Редактировать ${client.name}`}
                        title="Редактировать"
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 transition-colors hover:bg-indigo-50 hover:text-indigo-600"
                      >
                        <PencilIcon className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(client.id, client.name)}
                        aria-label={`Удалить ${client.name}`}
                        title="Удалить"
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 transition-colors hover:bg-red-50 hover:text-red-600"
                      >
                        <TrashIcon className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
