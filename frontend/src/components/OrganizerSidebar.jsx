import { Link, useLocation } from 'react-router-dom'

export const organizerNav = [
    { to: '/organizer', label: '대시보드', end: true },
    { to: '/organizer/conferences/new', label: '컨퍼런스 등록' },
    { to: '/organizer/applications', label: '신청 현황' },
    { to: '/organizer/checkin', label: 'QR 체크인' },
    { to: '/organizer/settlements', label: '정산 내역' },
]

function isNavActive(item, pathname) {
    if (item.end) return pathname === item.to
    return pathname === item.to || pathname.startsWith(`${item.to}/`)
}

export default function OrganizerSidebar() {
    const location = useLocation()

    return (
        <aside className="hidden md:block w-56 shrink-0 border-r border-border min-h-[calc(100vh-3.5rem)] sm:min-h-[calc(100vh-4rem)]">
            <nav className="flex flex-col gap-1 p-4">
                {organizerNav.map((item) => (
                    <Link
                        key={item.to}
                        to={item.to}
                        className={`px-3 py-2 rounded-md text-sm transition-colors ${
                            isNavActive(item, location.pathname)
                                ? 'bg-surface2 text-text font-medium'
                                : 'text-text-muted hover:text-text hover:bg-surface2/50'
                        }`}
                    >
                        {item.label}
                    </Link>
                ))}
            </nav>
        </aside>
    )
}