import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { listMembers, getMemberDetail, changeMemberRole } from '../../api/admin'
import { ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import SelectField from '../../components/SelectField'

const PAGE_SIZE = 20

const roleLabel = { MEMBER: '참가자', ORGANIZER: '주최자', ADMIN: '관리자' }
const roleOptions = Object.entries(roleLabel).map(([value, label]) => ({ value, label }))

function formatDate(value) {
    return value ? new Date(value).toLocaleDateString('ko-KR') : '-'
}

// 유저 상세 + 권한 변경 모달
function MemberDetailModal({ memberId, onClose, onChanged }) {
    const [detail, setDetail] = useState(null)
    const [role, setRole] = useState('')
    const [error, setError] = useState('')
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        let cancelled = false
        getMemberDetail(memberId)
            .then((res) => {
                if (cancelled) return
                setDetail(res.data)
                setRole(res.data.role)
            })
            .catch((err) => {
                if (!cancelled) setError(err instanceof ApiError ? err.message : '상세 정보를 불러오지 못했습니다.')
            })
        return () => {
            cancelled = true
        }
    }, [memberId])

    const handleSave = async () => {
        if (!detail || role === detail.role) return
        if (!window.confirm(`${detail.email}의 권한을 '${roleLabel[role]}'(으)로 변경할까요?`)) return
        setSaving(true)
        setError('')
        try {
            const res = await changeMemberRole(memberId, role)
            setDetail(res.data)
            onChanged(res.data)
        } catch (err) {
            setError(err instanceof ApiError ? err.message : '권한 변경에 실패했습니다.')
        } finally {
            setSaving(false)
        }
    }

    const rows = detail
        ? [
            ['이메일', detail.email],
            ['이름', detail.name],
            ['현재 권한', roleLabel[detail.role] ?? detail.role],
            ['기관명', detail.organizationName],
            ['사업자등록번호', detail.businessNo],
            ['연령대', detail.ageGroup],
            ['직무', detail.job],
            ['가입일', formatDate(detail.createdAt)],
        ]
        : []

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={onClose}>
            <div
                className="bg-surface border border-border rounded-xl p-6 max-w-lg w-full max-h-[85vh] overflow-y-auto"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex items-start justify-between gap-3 mb-4">
                    <h2 className="text-lg font-semibold text-text">유저 상세</h2>
                    <button onClick={onClose} aria-label="닫기" className="text-text-muted hover:text-text">
                        <X size={18} />
                    </button>
                </div>

                {error && <p className="mb-4 text-sm text-danger">{error}</p>}
                {!detail && !error && <p className="text-sm text-text-muted">불러오는 중...</p>}

                {detail && (
                    <>
                        <dl className="grid grid-cols-[120px_1fr] gap-y-2 text-sm mb-6">
                            {rows.map(([label, value]) => (
                                <div key={label} className="contents">
                                    <dt className="text-text-muted">{label}</dt>
                                    <dd className="text-text break-all">{value || '-'}</dd>
                                </div>
                            ))}
                        </dl>

                        <div className="flex items-end gap-2">
                            <SelectField
                                label="권한 변경"
                                options={roleOptions}
                                value={role}
                                onChange={(e) => setRole(e.target.value)}
                                className="flex-1"
                            />
                            <Button onClick={handleSave} loading={saving} disabled={role === detail.role}>
                                저장
                            </Button>
                        </div>
                    </>
                )}
            </div>
        </div>
    )
}

export default function AdminUsers() {
    const [members, setMembers] = useState([])
    const [keywordInput, setKeywordInput] = useState('')
    const [keyword, setKeyword] = useState('')
    const [page, setPage] = useState(0)
    const [pagination, setPagination] = useState(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [selectedId, setSelectedId] = useState(null)

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        setError('')
        listMembers({ keyword, page, size: PAGE_SIZE })
            .then((res) => {
                if (cancelled) return
                setMembers(res.data ?? [])
                setPagination(res.meta?.pagination ?? null)
            })
            .catch((err) => {
                if (!cancelled) setError(err instanceof ApiError ? err.message : '유저 목록을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [keyword, page])

    const handleSearch = (e) => {
        e.preventDefault()
        setPage(0)
        setKeyword(keywordInput.trim())
    }

    // 권한 변경 결과를 목록에도 즉시 반영
    const handleChanged = (updated) => {
        setMembers((prev) => prev.map((m) => (m.id === updated.id ? { ...m, role: updated.role } : m)))
    }

    const totalPages = pagination?.totalPages
    const hasNext = totalPages != null ? page + 1 < totalPages : members.length === PAGE_SIZE

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
            <h1 className="text-xl font-semibold text-text mb-6">유저 정보 관리</h1>

            <form onSubmit={handleSearch} className="flex items-end gap-2 mb-6">
                <TextField
                    placeholder="이메일 또는 이름으로 검색"
                    value={keywordInput}
                    onChange={(e) => setKeywordInput(e.target.value)}
                    className="flex-1"
                />
                <Button type="submit">검색</Button>
            </form>

            {error && <p className="mb-4 text-sm text-danger">{error}</p>}

            <div className="overflow-x-auto border border-border rounded-xl">
                <table className="w-full text-sm">
                    <thead className="bg-surface2 text-text-muted">
                    <tr>
                        <th className="text-left px-4 py-3 font-medium">이메일</th>
                        <th className="text-left px-4 py-3 font-medium">이름</th>
                        <th className="text-left px-4 py-3 font-medium">권한</th>
                        <th className="text-left px-4 py-3 font-medium">가입일</th>
                        <th className="px-4 py-3" />
                    </tr>
                    </thead>
                    <tbody>
                    {loading && (
                        <tr>
                            <td colSpan={5} className="px-4 py-6 text-center text-text-muted">불러오는 중...</td>
                        </tr>
                    )}
                    {!loading && members.length === 0 && (
                        <tr>
                            <td colSpan={5} className="px-4 py-6 text-center text-text-muted">유저가 없습니다.</td>
                        </tr>
                    )}
                    {!loading &&
                        members.map((m) => (
                            <tr key={m.id} className="border-t border-border">
                                <td className="px-4 py-3 text-text break-all">{m.email}</td>
                                <td className="px-4 py-3 text-text">{m.name}</td>
                                <td className="px-4 py-3 text-text">{roleLabel[m.role] ?? m.role}</td>
                                <td className="px-4 py-3 text-text-muted">{formatDate(m.createdAt)}</td>
                                <td className="px-4 py-3 text-right">
                                    <Button variant="secondary" className="!px-3 !py-1.5" onClick={() => setSelectedId(m.id)}>
                                        상세
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="flex items-center justify-center gap-3 mt-6">
                <Button variant="secondary" disabled={page === 0 || loading} onClick={() => setPage((p) => p - 1)}>
                    이전
                </Button>
                <span className="text-sm text-text-muted">
          {page + 1}
                    {totalPages != null && ` / ${Math.max(totalPages, 1)}`}
        </span>
                <Button variant="secondary" disabled={!hasNext || loading} onClick={() => setPage((p) => p + 1)}>
                    다음
                </Button>
            </div>

            {selectedId && (
                <MemberDetailModal memberId={selectedId} onClose={() => setSelectedId(null)} onChanged={handleChanged} />
            )}
        </div>
    )
}