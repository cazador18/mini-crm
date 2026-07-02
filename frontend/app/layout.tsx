import "./globals.css";
import Link from "next/link";

export const metadata = {
  title: "Mini-CRM",
  description: "Мини-CRM / трекер задач (учебный MVP)",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body className="min-h-screen bg-white text-gray-900">
        <nav className="border-b border-gray-200 px-6 py-3 flex items-center gap-6 text-sm">
          <Link href="/" className="font-semibold text-gray-900 hover:text-blue-600">
            Mini-CRM
          </Link>
          <Link href="/clients" className="text-gray-600 hover:text-blue-600">
            Клиенты
          </Link>
          <Link href="/tasks" className="text-gray-600 hover:text-blue-600">
            Задачи
          </Link>
        </nav>
        <main className="px-6 py-6">{children}</main>
      </body>
    </html>
  );
}
