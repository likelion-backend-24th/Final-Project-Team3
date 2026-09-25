import { Link, useLocation } from 'react-router-dom'

export const adminNav = [
    { to: '/admin', label: '컨퍼런스 승인', end: true },
    { to: '/admin/settlements', label: '정산 대시보드' },
    { to: '/admin/settings', label: '시스템 설정' },
    { to: '/admin/users', label: '유저 정보 관리' },
]

function isNavActive(item, pathname) {
    if (item.end) return pathname === item.to
    return pathname === item.to || pathname.startsWith(`${item.to}/`)
}

export default function AdminSidebar() {
    const location = useLocation()

    return (
        <aside className="hidden md:block w-56 shrink-0 border-r border-border min-h-[calc(100vh-3.5rem)] sm:min-h-[calc(100vh-4rem)]">
            <nav className="flex flex-col gap-1 p-4">
                {adminNav.map((item) => (
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