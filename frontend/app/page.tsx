'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { dashboardApi, type DashboardStats } from '@/lib/api';
import { STATUS_THEME, STATUS_ORDER } from '@/lib/theme';
import { CheckIcon, ClockIcon, InboxIcon, UsersIcon } from '@/components/icons';
import type { ComponentType } from 'react';

const STATUS_ICON: Record<(typeof STATUS_ORDER)[number], ComponentType<{ className?: string }>> = {
  NEW: InboxIcon,
  IN_PROGRESS: ClockIcon,
  DONE: CheckIcon,
};

type Card = {
  label: string;
  value: number;
  href: string;
  icon: ComponentType<{ className?: string }>;
  chipBg: string;
  chipText: string;
  gradient: string;
};

function StatCard({ label, value, href, icon: Icon, chipBg, chipText, gradient }: Card) {
  return (
    <Link
      href={href}
      className={`group block rounded-2xl border border-gray-100 bg-white bg-gradient-to-br ${gradient} p-5 shadow-sm transition-shadow hover:shadow-md`}
    >
      <div className={`inline-flex h-11 w-11 items-center justify-center rounded-xl ${chipBg} ${chipText}`}>
        <Icon className="h-5 w-5" />
      </div>
      <div className="mt-4 text-3xl font-bold text-gray-900">{value}</div>
      <div className="mt-1 text-xs font-medium uppercase tracking-wider text-gray-500">{label}</div>
      <div className="mt-3 text-xs text-gray-400 transition-colors group-hover:text-gray-600">→ перейти</div>
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
      <h1 className="text-2xl font-semibold tracking-tight text-gray-900 mb-6">Дашборд</h1>

      {error && (
        <div className="bg-red-50 text-red-700 border border-red-200 rounded-xl p-3 text-sm mb-4">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="animate-pulse bg-white border border-gray-100 rounded-2xl h-36" />
          ))}
        </div>
      ) : stats ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Клиенты"
            value={stats.totalClients}
            href="/clients"
            icon={UsersIcon}
            chipBg="bg-indigo-50"
            chipText="text-indigo-600"
            gradient="from-indigo-50/60 to-white"
          />
          {STATUS_ORDER.map(status => (
            <StatCard
              key={status}
              label={STATUS_THEME[status].label}
              value={stats.tasksByStatus[status] ?? 0}
              href="/tasks"
              icon={STATUS_ICON[status]}
              chipBg={STATUS_THEME[status].chipBg}
              chipText={STATUS_THEME[status].chipText}
              gradient={STATUS_THEME[status].gradient}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
