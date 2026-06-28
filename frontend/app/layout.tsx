import "./globals.css";

export const metadata = {
  title: "Mini-CRM",
  description: "Мини-CRM / трекер задач (учебный MVP)",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body>{children}</body>
    </html>
  );
}
