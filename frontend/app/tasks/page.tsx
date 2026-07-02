'use client';

import { useCallback, useEffect, useState } from 'react';
import { clientsApi, tasksApi, type Client, type Task, type TaskInput } from '@/lib/api';
import TaskForm from '@/components/TaskForm';

type FormMode = { mode: 'create' } | { mode: 'edit'; task: Task } | null;

const STATUS_LABELS: Record<Task['status'], string> = {
  NEW: 'Новая',
  IN_PROGRESS: 'В работе',
  DONE: 'Готово',
};

const STATUS_BADGE: Record<Task['status'], string> = {
  NEW: 'bg-gray-100 text-gray-700',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  DONE: 'bg-green-100 text-green-700',
};

const PRIORITY_LABELS: Record<Task['priority'], string> = {
  LOW: 'Низкий',
  MEDIUM: 'Средний',
  HIGH: 'Высокий',
};

const PRIORITY_BADGE: Record<Task['priority'], string> = {
  LOW: 'bg-gray-100 text-gray-600',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-red-100 text-red-700',
};

export default function TasksPage() {
  const [tasks, setTasks]       = useState<Task[]>([]);
  const [clients, setClients]   = useState<Client[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);
  const [formMode, setFormMode] = useState<FormMode>(null);
  const [saving, setSaving]     = useState(false);
  const [statusFilter, setStatusFilter] = useState<Task['status'] | ''>('');
  const [clientFilter, setClientFilter] = useState<number | ''>('');

  const loadTasks = useCallback(async () => {
    try {
      setError(null);
      const data = await tasksApi.getAll({
        status: statusFilter || undefined,
        clientId: clientFilter || undefined,
      });
      setTasks(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка загрузки');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, clientFilter]);

  useEffect(() => {
    clientsApi.getAll().then(setClients).catch(() => {});
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const handleSubmit = async (data: TaskInput) => {
    setSaving(true);
    try {
      if (formMode?.mode === 'edit') {
        await tasksApi.update(formMode.task.id, data);
      } else {
        await tasksApi.create(data);
      }
      setFormMode(null);
      await loadTasks();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка сохранения');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, title: string) => {
    if (!confirm(`Удалить задачу «${title}»?`)) return;
    try {
      await tasksApi.delete(id);
      await loadTasks();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка удаления');
    }
  };

  const clientMap = new Map(clients.map(c => [c.id, c.name]));

  const formInitial: TaskInput | undefined =
    formMode?.mode === 'edit'
      ? {
          title: formMode.task.title,
          description: formMode.task.description ?? '',
          status: formMode.task.status,
          priority: formMode.task.priority,
          deadline: formMode.task.deadline ?? '',
          clientId: formMode.task.clientId,
        }
      : undefined;

  const selectCls =
    'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-900">Задачи</h1>
        {!formMode && (
          <button
            onClick={() => setFormMode({ mode: 'create' })}
            className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700"
          >
            + Новая задача
          </button>
        )}
      </div>

      {formMode && (
        <TaskForm
          initial={formInitial}
          clients={clients}
          onSubmit={handleSubmit}
          onCancel={() => setFormMode(null)}
          saving={saving}
        />
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600">Статус:</label>
          <select
            className={selectCls}
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value as Task['status'] | '')}
          >
            <option value="">Все</option>
            {(Object.entries(STATUS_LABELS) as [Task['status'], string][]).map(([val, label]) => (
              <option key={val} value={val}>{label}</option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600">Клиент:</label>
          <select
            className={selectCls}
            value={clientFilter}
            onChange={e => setClientFilter(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">Все</option>
            {clients.map(c => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>

        {(statusFilter || clientFilter) && (
          <button
            onClick={() => { setStatusFilter(''); setClientFilter(''); }}
            className="text-sm text-gray-500 hover:text-gray-800 underline"
          >
            Сбросить
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-50 text-red-700 border border-red-200 rounded p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Загрузка…</p>
      ) : tasks.length === 0 ? (
        <p className="text-sm text-gray-500">Задач не найдено.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                {['Заголовок', 'Клиент', 'Статус', 'Приоритет', 'Дедлайн', ''].map(h => (
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
              {tasks.map(task => (
                <tr key={task.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {task.title}
                    {task.description && (
                      <p className="text-xs text-gray-400 font-normal truncate max-w-xs">{task.description}</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {clientMap.get(task.clientId) ?? '—'}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${STATUS_BADGE[task.status]}`}>
                      {STATUS_LABELS[task.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${PRIORITY_BADGE[task.priority]}`}>
                      {PRIORITY_LABELS[task.priority]}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-400">
                    {task.deadline
                      ? new Date(task.deadline).toLocaleDateString('ru-RU')
                      : '—'}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <button
                      onClick={() => setFormMode({ mode: 'edit', task })}
                      className="text-blue-600 hover:underline text-sm"
                    >
                      Редактировать
                    </button>
                    <button
                      onClick={() => handleDelete(task.id, task.title)}
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
