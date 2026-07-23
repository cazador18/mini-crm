'use client';

import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ClipboardIcon, DashboardIcon, LogoutIcon, UsersIcon } from '@/components/icons';

const NAV_ITEMS = [
  { href: '/', label: 'Дашборд', icon: DashboardIcon },
  { href: '/clients', label: 'Клиенты', icon: UsersIcon },
  { href: '/tasks', label: 'Задачи', icon: ClipboardIcon },
];

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);

  useEffect(() => {
    setUsername(localStorage.getItem('username'));
    setRole(localStorage.getItem('role'));
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    router.push('/login');
  };

  return (
    <aside className="flex w-60 shrink-0 flex-col border-r border-gray-100 bg-white">
      <div className="px-5 py-5">
        <Link href="/" className="flex items-center gap-2 text-lg font-semibold tracking-tight text-gray-900">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-sm font-bold text-white">
            M
          </span>
          Mini-CRM
        </Link>
      </div>

      <nav className="flex flex-1 flex-col gap-1 px-3">
        {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
          const active = href === '/' ? pathname === '/' : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors ${
                active
                  ? 'bg-indigo-50 font-medium text-indigo-700'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              }`}
            >
              <Icon className={`h-5 w-5 ${active ? 'text-indigo-600' : 'text-gray-400'}`} />
              {label}
            </Link>
          );
        })}
      </nav>

      {username && (
        <div className="border-t border-gray-100 px-3 py-4">
          <div className="mb-2 px-3 text-xs text-gray-500">
            <div className="font-medium text-gray-700">{username}</div>
            <div>{role}</div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
          >
            <LogoutIcon className="h-5 w-5 text-gray-400" />
            Выйти
          </button>
        </div>
      )}
    </aside>
  );
}
