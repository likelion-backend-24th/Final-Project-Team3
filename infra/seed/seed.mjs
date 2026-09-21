// 배포 서버 더미데이터 시드 (컨퍼런스 등록 → 관리자 승인 → 세션 등록 → 관리자 승인)
// 실행: BASE_URL=http://16.184.35.224 ORGANIZER_EMAIL=.. ORGANIZER_PASSWORD=.. ADMIN_EMAIL=.. ADMIN_PASSWORD=.. node infra/seed/seed.mjs
// 일시는 타임존 없는 LocalDateTime이라 화면에 그대로 찍힌다 -> 컨퍼런스/세션은 KST 벽시계 시각(09:00~)으로 넣는다.
// 단 서버(UTC)의 @Future 검증을 받는 "신청 시작"만 UTC 기준 now+5분으로 만든다.

const BASE = (process.env.BASE_URL ?? "http://16.184.35.224").replace(/\/$/, "");
const { ORGANIZER_EMAIL, ORGANIZER_PASSWORD, ADMIN_EMAIL, ADMIN_PASSWORD } = process.env;
if (!ORGANIZER_EMAIL || !ORGANIZER_PASSWORD || !ADMIN_EMAIL || !ADMIN_PASSWORD) {
  console.error("ORGANIZER_EMAIL/PASSWORD, ADMIN_EMAIL/PASSWORD 환경변수를 설정하세요.");
  process.exit(1);
}

const pad = (n) => String(n).padStart(2, "0");
const iso = (d) =>
  `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:00`;
const inMinutes = (m) => new Date(Date.now() + m * 60_000);
// 오늘로부터 days일 뒤 UTC hour시 정각
const at = (days, hour) => {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + days);
  d.setUTCHours(hour, 0, 0, 0);
  return d;
};

const conferences = [
  {
    title: "2026 백엔드 엔지니어링 컨퍼런스",
    capacity: 300,
    start: at(14, 9),
    end: at(14, 18),
    location: "서울 강남구 코엑스 컨퍼런스룸 A",
    description: "MSA, 대용량 트래픽, 데이터베이스 설계까지 백엔드 개발자를 위한 실무 중심 컨퍼런스입니다.",
    tags: ["Backend", "Cloud", "Data"],
    sessions: [
      { title: "MSA 전환기: 모놀리스에서 서비스 분리까지", speaker: "김민수", price: 0, capacity: 100, max: 2, sh: 10, eh: 12 },
      { title: "대용량 트래픽 처리와 캐시 전략", speaker: "이서연", price: 20000, capacity: 80, max: 2, sh: 13, eh: 15 },
      { title: "JPA 성능 최적화 실전", speaker: "박지훈", price: 15000, capacity: 60, max: 1, sh: 15, eh: 17 },
    ],
  },
  {
    title: "AI & 데이터 서밋 2026",
    capacity: 200,
    start: at(21, 9),
    end: at(21, 18),
    location: "서울 성동구 서울숲 컨벤션홀",
    description: "생성형 AI 활용 사례와 데이터 파이프라인 구축 노하우를 공유합니다.",
    tags: ["AI", "ML", "Data"],
    sessions: [
      { title: "LLM을 서비스에 붙이는 법", speaker: "최유진", price: 10000, capacity: 100, max: 2, sh: 10, eh: 12 },
      { title: "실시간 데이터 파이프라인 구축", speaker: "정하늘", price: 0, capacity: 70, max: 2, sh: 13, eh: 15 },
    ],
  },
  {
    title: "프론트엔드 데브데이",
    capacity: 150,
    start: at(30, 9),
    end: at(30, 17),
    location: "판교 테크노밸리 세미나실",
    description: "React, 성능 최적화, 디자인 시스템까지 프론트엔드 개발자들의 하루.",
    tags: ["Frontend", "Software"],
    sessions: [
      { title: "React 렌더링 최적화 깊게 파보기", speaker: "한소희", price: 5000, capacity: 90, max: 2, sh: 10, eh: 12 },
      { title: "디자인 시스템 구축기", speaker: "오세훈", price: 0, capacity: 60, max: 2, sh: 13, eh: 15 },
    ],
  },
];

async function call(method, path, { token, body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const res = await fetch(BASE + path, { method, headers, body: payload });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { json = { raw: text }; }
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${text.slice(0, 300)}`);
  return json.data;
}

const login = async (email, password) =>
  (await call("POST", "/api/auth/login", { body: { email, password } })).accessToken;

async function main() {
  const organizer = await login(ORGANIZER_EMAIL, ORGANIZER_PASSWORD);
  const admin = await login(ADMIN_EMAIL, ADMIN_PASSWORD);
  console.log("로그인 완료 (주최자/관리자)");

  for (const c of conferences) {
    const request = {
      title: c.title,
      capacity: c.capacity,
      startAt: iso(c.start),
      endAt: iso(c.end),
      location: c.location,
      description: c.description,
      tags: c.tags,
    };
    const form = new FormData();
    form.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
    const conf = await call("POST", "/api/conferences", { token: organizer, form });
    await call("PATCH", `/api/admin/conferences/${conf.id}/approve`, { token: admin });
    console.log(`컨퍼런스 승인: ${c.title}`);

    for (const s of c.sessions) {
      const day = new Date(c.start);
      const sessionStart = new Date(day); sessionStart.setUTCHours(s.sh, 0, 0, 0);
      const sessionEnd = new Date(day); sessionEnd.setUTCHours(s.eh, 0, 0, 0);
      const session = await call("POST", `/api/conferences/${conf.id}/sessions`, {
        token: organizer,
        body: {
          title: s.title,
          capacity: s.capacity,
          startAt: iso(inMinutes(5)), // 신청 시작: 5분 뒤 (@Future)
          endAt: iso(new Date(c.start.getTime() - 24 * 3600_000)), // 신청 종료: 컨퍼런스 하루 전
          sessionStartAt: iso(sessionStart),
          sessionEndAt: iso(sessionEnd),
          location: c.location,
          speaker: s.speaker,
          price: s.price,
          maxHeadcountPerApplication: s.max,
        },
      });
      await call("PATCH", `/api/admin/sessions/${session.id}/approve`, { token: admin });
      console.log(`  세션 승인: ${s.title}`);
    }
  }
  console.log("완료");
}

main().catch((e) => { console.error("실패:", e.message); process.exit(1); });
