'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { dashboardApi, type DashboardStats } from '@/lib/api';

type Card = {
  label: string;
  value: number;
  colorCls: string;
  href: string;
};

function StatCard({ label, value, colorCls, href }: Card) {
  return (
    <Link
      href={href}
      className="block rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow"
    >
      <div className={`text-xs font-medium uppercase tracking-wider mb-1 ${colorCls}`}>
        {label}
      </div>
      <div className="text-3xl font-bold text-gray-900">{value}</div>
      <div className="text-xs text-gray-400 mt-2">→ перейти</div>
    </Link>
  );
}

export default function HomePage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dashboardApi
      .getStats()
      .then(setStats)
      .catch(e => setError(e instanceof Error ? e.message : 'Ошибка загрузки'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 className="text-xl font-semibold text-gray-900 mb-6">Дашборд</h1>

      {error && (
        <div className="bg-red-50 text-red-700 border border-red-200 rounded p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="animate-pulse bg-gray-100 rounded-lg h-28" />
          ))}
        </div>
      ) : stats ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Клиенты"
            value={stats.totalClients}
            colorCls="text-blue-600"
            href="/clients"
          />
          <StatCard
            label="Новые задачи"
            value={stats.tasksByStatus.NEW ?? 0}
            colorCls="text-gray-500"
            href="/tasks"
          />
          <StatCard
            label="В работе"
            value={stats.tasksByStatus.IN_PROGRESS ?? 0}
            colorCls="text-yellow-600"
            href="/tasks"
          />
          <StatCard
            label="Завершённые"
            value={stats.tasksByStatus.DONE ?? 0}
            colorCls="text-green-600"
            href="/tasks"
          />
        </div>
      ) : null}
    </div>
  );
}
