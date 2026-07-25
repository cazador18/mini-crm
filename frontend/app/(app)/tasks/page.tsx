'use client';

import { useCallback, useEffect, useState } from 'react';
import { clientsApi, tasksApi, type Client, type Task, type TaskInput } from '@/lib/api';
import TaskForm from '@/components/TaskForm';
import { PencilIcon, PlusIcon, TrashIcon } from '@/components/icons';
import { PRIORITY_ORDER, PRIORITY_THEME, STATUS_ORDER, STATUS_THEME } from '@/lib/theme';

type FormMode = { mode: 'create' } | { mode: 'edit'; task: Task } | null;

function StatusBadge({ status }: { status: Task['status'] }) {
  const theme = STATUS_THEME[status];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${theme.badge}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${theme.dot}`} />
      {theme.label}
    </span>
  );
}

function PriorityBadge({ priority }: { priority: Task['priority'] }) {
  const theme = PRIORITY_THEME[priority];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${theme.badge}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${theme.dot}`} />
      {theme.label}
    </span>
  );
}

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
    'rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold tracking-tight text-gray-900">Задачи</h1>
        {!formMode && (
          <button
            onClick={() => setFormMode({ mode: 'create' })}
            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-indigo-700"
          >
            <PlusIcon className="h-4 w-4" />
            Новая задача
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
            {STATUS_ORDER.map(status => (
              <option key={status} value={status}>{STATUS_THEME[status].label}</option>
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
        <div className="bg-red-50 text-red-700 border border-red-200 rounded-xl p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-gray-500">Загрузка…</p>
      ) : tasks.length === 0 ? (
        <p className="text-sm text-gray-500">Задач не найдено.</p>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-100 text-sm">
            <thead className="bg-gray-50/60">
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
            <tbody className="divide-y divide-gray-50">
              {tasks.map(task => (
                <tr key={task.id} className="transition-colors hover:bg-gray-50">
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
                    <StatusBadge status={task.status} />
                  </td>
                  <td className="px-4 py-3">
                    <PriorityBadge priority={task.priority} />
                  </td>
                  <td className="px-4 py-3 text-gray-400">
                    {task.deadline
                      ? new Date(task.deadline).toLocaleDateString('ru-RU')
                      : '—'}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setFormMode({ mode: 'edit', task })}
                        aria-label={`Редактировать «${task.title}»`}
                        title="Редактировать"
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-gray-500 transition-colors hover:bg-indigo-50 hover:text-indigo-600"
                      >
                        <PencilIcon className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(task.id, task.title)}
                        aria-label={`Удалить «${task.title}»`}
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
