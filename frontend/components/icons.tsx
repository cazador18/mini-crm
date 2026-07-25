type IconProps = { className?: string };

const base = 'w-5 h-5';

export function DashboardIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <rect x="3.75" y="3.75" width="7" height="7" rx="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="13.25" y="3.75" width="7" height="4.5" rx="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="13.25" y="10.75" width="7" height="9.5" rx="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="3.75" y="13.25" width="7" height="7" rx="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function UsersIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M15.75 8.25a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.5 20.25a7.5 7.5 0 0 1 15 0"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M17.25 9.75a2.75 2.75 0 1 0 0-5.5M20.25 20.25a6.5 6.5 0 0 0-3.75-5.9"
      />
    </svg>
  );
}

export function ClipboardIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M9 4.5h6a1.5 1.5 0 0 1 1.5 1.5v0h1.5A1.5 1.5 0 0 1 19.5 7.5v11.25A1.5 1.5 0 0 1 18 20.25H6a1.5 1.5 0 0 1-1.5-1.5V7.5A1.5 1.5 0 0 1 6 6h1.5v0A1.5 1.5 0 0 1 9 4.5Z"
      />
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6M9 15.5h4" />
    </svg>
  );
}

export function InboxIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3.75 13.5h4.15a2.1 2.1 0 0 1 1.87 1.14l.32.63a2.1 2.1 0 0 0 1.87 1.14h.98a2.1 2.1 0 0 0 1.87-1.14l.32-.63a2.1 2.1 0 0 1 1.87-1.14h4.15"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M5.34 6.24 3.75 13.5v4.5a1.5 1.5 0 0 0 1.5 1.5h13.5a1.5 1.5 0 0 0 1.5-1.5v-4.5l-1.59-7.26A1.5 1.5 0 0 0 17.19 5H6.81a1.5 1.5 0 0 0-1.47 1.24Z"
      />
    </svg>
  );
}

export function ClockIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <circle cx="12" cy="12" r="8.25" strokeLinecap="round" strokeLinejoin="round" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 7.5V12l3 1.75" />
    </svg>
  );
}

export function CheckIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m4.5 12.75 5.25 5.25 10.5-10.5" />
    </svg>
  );
}

export function PlusIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
    </svg>
  );
}

export function XIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m6 6 12 12M18 6 6 18" />
    </svg>
  );
}

export function PencilIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="m16.86 4.49 2.65 2.65a1 1 0 0 1 0 1.41L8.9 19.16l-4.15.99.99-4.15L16.45 5.19a1.5 1.5 0 0 1 .41-.7Z"
      />
      <path strokeLinecap="round" strokeLinejoin="round" d="m14.75 6.6 2.65 2.65" />
    </svg>
  );
}

export function LogoutIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M9 6.75V5.25A1.5 1.5 0 0 1 10.5 3.75h6a1.5 1.5 0 0 1 1.5 1.5v13.5a1.5 1.5 0 0 1-1.5 1.5h-6a1.5 1.5 0 0 1-1.5-1.5v-1.5"
      />
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 12h10.5m0 0-3-3m3 3-3 3" />
    </svg>
  );
}

export function TrashIcon({ className = base }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M4.5 7.25h15M9.75 7.25V5.5a1.5 1.5 0 0 1 1.5-1.5h1.5a1.5 1.5 0 0 1 1.5 1.5v1.75M18.25 7.25 17.6 19a1.5 1.5 0 0 1-1.5 1.4H7.9A1.5 1.5 0 0 1 6.4 19L5.75 7.25M10.25 11v6M13.75 11v6"
      />
    </svg>
  );
}
