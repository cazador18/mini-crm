'use client';

import { useEffect, useState } from 'react';
import { clientsApi, type Client, type ClientInput } from '@/lib/api';
import ClientForm from '@/components/ClientForm';

type FormMode = { mode: 'create' } | { mode: 'edit'; client: Client } | null;

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
        <h1 className="text-xl font-semibold text-gray-900">Клиенты</h1>
        {!formMode && (
          <button
            onClick={() => setFormMode({ mode: 'create' })}
            className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700"
          >
            + Новый клиент
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
        <div className="bg-red-50 text-red-700 border border-red-200 rounded p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Загрузка…</p>
      ) : clients.length === 0 ? (
        <p className="text-sm text-gray-500">Клиентов пока нет.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
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
            <tbody className="bg-white divide-y divide-gray-100">
              {clients.map(client => (
                <tr key={client.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{client.name}</td>
                  <td className="px-4 py-3 text-gray-600">{client.email}</td>
                  <td className="px-4 py-3 text-gray-600">{client.phone ?? '—'}</td>
                  <td className="px-4 py-3 text-gray-400">
                    {new Date(client.createdAt).toLocaleDateString('ru-RU')}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <button
                      onClick={() => setFormMode({ mode: 'edit', client })}
                      className="text-blue-600 hover:underline text-sm"
                    >
                      Редактировать
                    </button>
                    <button
                      onClick={() => handleDelete(client.id, client.name)}
                      className="text-red-600 hover:underline text-sm ml-4"
                    >
                      Удалить
                    </button>
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
