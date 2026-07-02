import "./globals.css";
import { Inter } from "next/font/google";
import Sidebar from "@/components/Sidebar";

const inter = Inter({ subsets: ["latin", "cyrillic"] });

export const metadata = {
  title: "Mini-CRM",
  description: "Мини-CRM / трекер задач (учебный MVP)",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body className={`${inter.className} antialiased text-gray-900`}>
        <div className="flex min-h-screen bg-slate-50">
          <Sidebar />
          <main className="flex-1 px-8 py-8">
            <div className="mx-auto max-w-6xl">{children}</div>
          </main>
        </div>
      </body>
    </html>
  );
}
