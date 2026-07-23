import "./globals.css";
import { Inter } from "next/font/google";

const inter = Inter({ subsets: ["latin", "cyrillic"] });

export const metadata = {
  title: "Mini-CRM",
  description: "Мини-CRM / трекер задач (учебный MVP)",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body className={`${inter.className} antialiased text-gray-900`}>
        {children}
      </body>
    </html>
  );
}
