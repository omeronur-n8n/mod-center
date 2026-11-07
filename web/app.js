// Gelisio PC — web sürümü. Android uygulamasıyla aynı Firestore verisini (users/{uid}/...) kullanır.
// Alışkanlık, proje ve haftalık hesapları Android'deki data/Logic.kt ile birebir aynı kuralları izler.
import { initializeApp } from "https://cdn.jsdelivr.net/npm/firebase@10.14.1/app/+esm";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "https://cdn.jsdelivr.net/npm/firebase@10.14.1/auth/+esm";
import {
  initializeFirestore, persistentLocalCache, persistentMultipleTabManager,
  collection, doc, onSnapshot, setDoc, deleteDoc, serverTimestamp,
} from "https://cdn.jsdelivr.net/npm/firebase@10.14.1/firestore/+esm";

const app = initializeApp({
  apiKey: "AIzaSyBG2Uovxbp8xxTHPFJX2Tnxv8_1UjMcUEo",
  authDomain: "gelisio-1.firebaseapp.com",
  projectId: "gelisio-1",
  storageBucket: "gelisio-1.firebasestorage.app",
  messagingSenderId: "83444051862",
  appId: "1:83444051862:android:9f59a560b4d9dcbab837b7",
});
const auth = getAuth(app);
const db = initializeFirestore(app, { localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }) });

const RULES_MSG = "Firestore kuralları erişimi engelliyor. Kuralları Firebase konsolunda Firestore → Kurallar bölümüne yapıştırıp yayınla.";
const DIM = { physical: "Fiziksel", mental: "Zihinsel", spiritual: "Ruhsal", social: "Sosyal" };
const PHIL = [["monastic", "Keşiş"], ["bimodal", "Çift modlu"], ["rhythmic", "Ritmik"], ["journalistic", "Gazeteci"]];
const DAYS = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"];
const TABS = [
  { href: "#/today", label: "Bugün", id: "today", icon: "M3 12l9-8 9 8M5 10v10h14V10" },
  { href: "#/habits", label: "Alışkanlık", id: "habits", icon: "M5 12l4 4 10-10" },
  { href: "#/projects/project", label: "Proje", id: "projects", icon: "M4 6h16M4 12h10M4 18h7" },
  { href: "#/week", label: "Hafta", id: "week", icon: "M4 5h16v14H4zM4 10h16M9 5v14" },
  { href: "#/me", label: "Ben", id: "me", icon: "M12 12a4 4 0 100-8 4 4 0 000 8zM4 21c1-4 4-6 8-6s7 2 8 6" },
];

/* ---------- Durum ---------- */
const S = {
  user: null, profile: {}, identities: [], habits: [], logs: {}, projects: [], sessions: [], outputs: [], weeks: {}, error: null,
};
let unsubs = [];
let renderQueued = false;
let deferred = false;

/* ---------- Yardımcılar ---------- */
const $ = (s) => document.querySelector(s);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const pad = (n) => String(n).padStart(2, "0");
const key = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const parse = (s) => { const [y, m, d] = s.split("-").map(Number); return new Date(y, m - 1, d); };
const todayKey = () => key(new Date());
const addDays = (s, n) => { const d = parse(s); d.setDate(d.getDate() + n); return key(d); };
const diffDays = (a, b) => Math.round((parse(b) - parse(a)) / 86400000);
const dow = (s) => ((parse(s).getDay() + 6) % 7) + 1; // ISO: 1=Pzt … 7=Paz
const weekStartKey = (s) => addDays(s, -(dow(s) - 1));
const weekDays = (s) => Array.from({ length: 7 }, (_, i) => addDays(weekStartKey(s), i));
const sessDate = (s) => key(new Date(s.start));
const fmtLong = (s) => parse(s).toLocaleDateString("tr-TR", { weekday: "long", day: "numeric", month: "long" });
const fmtShort = (s) => parse(s).toLocaleDateString("tr-TR", { day: "numeric", month: "short" });
const pct = (x) => (x == null ? "—" : `${Math.round(x * 100)}%`);
const bar = (x) => `<div class="bar"><span style="width:${Math.max(0, Math.min(100, Math.round(x * 100)))}%"></span></div>`;
const isoWeek = (s) => {
  const d = parse(s);
  const t = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
  const day = t.getUTCDay() || 7;
  t.setUTCDate(t.getUTCDate() + 4 - day);
  const y = t.getUTCFullYear();
  const w = Math.ceil(((t - Date.UTC(y, 0, 1)) / 86400000 + 1) / 7);
  return `${y}-W${pad(w)}`;
};
const fmtClock = (ms) => {
  const s = Math.floor(ms / 1000);
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  return h ? `${h}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`;
};

/* ---------- Alışkanlık motoru (Logic.kt ile aynı) ---------- */
const isDone = (s) => s === "full" || s === "min";
const st = (hid, date) => S.logs[hid]?.[date];
const scheduled = (h, date) => h.days.includes(dow(date)) && date >= h.createdAt;
const activeHabits = () => S.habits.filter((h) => !h.archived);
const reps = (h) => Object.values(S.logs[h.id] || {}).filter(isDone).length;
const stageOf = (n) => (n <= 20 ? "Kuruluyor" : n <= 60 ? "Pekişiyor" : "Otomatik");

function prevScheduled(h, date) {
  let d = addDays(date, -1);
  for (let i = 0; i < 7; i++) {
    if (d < h.createdAt) return null;
    if (scheduled(h, d)) return d;
    d = addDays(d, -1);
  }
  return null;
}

/** Zincir hücresi: tek kaçırma sarı (miss), art arda ikinci kaçırma kırmızı (miss2). */
function cellOf(h, date, today) {
  if (date > today) return "future";
  if (!scheduled(h, date)) return "off";
  const s = st(h.id, date);
  if (s === "full") return "full";
  if (s === "min") return "min";
  if (date === today && !s) return "open";
  const prev = prevScheduled(h, date);
  if (!prev) return "miss";
  return isDone(st(h.id, prev)) ? "miss" : "miss2";
}

/** Ana metrik: son N günün tutarlılığı. Bugün yapılmadıysa henüz kaçırma sayılmaz. */
function consistency(h, today, win = 30) {
  let sched = 0, done = 0;
  for (let i = 0; i < win; i++) {
    const d = addDays(today, -i);
    if (!scheduled(h, d)) continue;
    const s = st(h.id, d);
    if (d === today && !isDone(s)) continue;
    sched++;
    if (isDone(s)) done++;
  }
  return sched ? done / sched : null;
}

/** Kırılgan seri (ikincil): art arda yapılan planlı gün sayısı. */
function streak(h, today) {
  let d = isDone(st(h.id, today)) ? today : addDays(today, -1);
  let n = 0;
  while (d >= h.createdAt) {
    if (scheduled(h, d)) {
      if (isDone(st(h.id, d))) n++; else break;
    }
    d = addDays(d, -1);
  }
  return n;
}

/** 14 günde bir: %95+ zorluğu artır, %60 altı 2 dakikalık sürüme in. */
function goldilocks(h, today) {
  if (diffDays(h.createdAt, today) < 14) return null;
  if (h.goldilocksAt && diffDays(h.goldilocksAt, today) < 14) return null;
  const c = consistency(h, addDays(today, -1), 14);
  if (c == null) return null;
  return c >= 0.95 ? "up" : c < 0.6 ? "down" : null;
}

const missedYesterday = (h, t) => scheduled(h, addDays(t, -1)) && !isDone(st(h.id, addDays(t, -1)));
const votes = (iid) => S.habits.filter((h) => h.identityId === iid).reduce((n, h) => n + reps(h), 0);

/** Hobi pratiği: bir günde seanslar + bağlı alışkanlık tamamlaması (×15 dk). */
function practiceMinutes(p, date) {
  const fromSessions = S.sessions.filter((s) => s.projectId === p.id && sessDate(s) === date).reduce((n, s) => n + s.minutes, 0);
  const fromHabits = S.habits.filter((h) => h.projectId === p.id && isDone(st(h.id, date))).length;
  return fromSessions + fromHabits * 15;
}
const weeklyPractice = (p, anyDay) => weekDays(anyDay).reduce((n, d) => n + practiceMinutes(p, d), 0);
function levelUpReady(p, t) {
  if (p.kind !== "hobby" || p.skillLevel >= 10) return false;
  return [1, 2, 3, 4].every((w) => weeklyPractice(p, addDays(t, -7 * w)) >= p.weeklyPracticeMin);
}
const weekDeepMinutes = (anyDay, projectId) =>
  S.sessions.filter((s) => s.kind === "deep" && weekDays(anyDay).includes(sessDate(s)) && (!projectId || s.projectId === projectId))
    .reduce((n, s) => n + s.minutes, 0);
const milestonePct = (p) => (p.milestones.length ? p.milestones.filter((m) => m.done).length / p.milestones.length : 0);

/* ---------- Normalleştirme (Store.kt'nin eşlemesi) ---------- */
const normHabit = (id, d) => ({
  id,
  name: d.name || "",
  identityId: d.identityId || "",
  fullVersion: d.fullVersion || "",
  twoMinVersion: d.twoMinVersion || "",
  cue: { type: d.cue?.type || "time", time: d.cue?.time || "", place: d.cue?.place || "", afterHabitId: d.cue?.afterHabitId || "" },
  reward: d.reward || "",
  days: Array.isArray(d.days) ? d.days.map(Number) : [1, 2, 3, 4, 5, 6, 7],
  difficulty: Number(d.difficulty) || 1,
  projectId: d.projectId || "",
  archived: !!d.archived,
  createdAt: d.createdAt || todayKey(),
  goldilocksAt: d.goldilocksAt || null,
});

const normProject = (id, d) => ({
  id,
  kind: d.kind || "project",
  title: d.title || "",
  goalId: d.goalId || "",
  role: d.role || "",
  milestones: (d.milestones || []).map((m) => ({ title: m.title || "", done: !!m.done })),
  leadMeasure: { unit: d.leadMeasure?.unit || "derin saat", weeklyTarget: Number(d.leadMeasure?.weeklyTarget) || 10 },
  dueDate: d.dueDate || null,
  identityId: d.identityId || "",
  dimension: d.dimension || "physical",
  skillLevel: Number(d.skillLevel) || 1,
  weeklyPracticeMin: Number(d.weeklyPracticeMin ?? 120),
  outputType: d.outputType || "",
  archived: !!d.archived,
});

/* ---------- Firestore dinleyicileri ---------- */
const userRef = () => doc(db, "users", S.user.uid);
const colRef = (name) => collection(db, "users", S.user.uid, name);
const newId = (name) => doc(colRef(name)).id;

function stopListening() {
  unsubs.forEach((u) => u());
  unsubs = [];
  S.logs = {};
}

function startListening(uid) {
  stopListening();
  const on = (ref, fn) => unsubs.push(onSnapshot(
    ref,
    (snap) => { try { fn(snap); S.error = null; } catch (e) { console.error(e); } scheduleRender(); },
    (err) => { S.error = err.code === "permission-denied" ? RULES_MSG : err.message; scheduleRender(); },
  ));
  on(doc(db, "users", uid), (s) => {
    const d = s.data() || {};
    S.profile = {
      mission: d.mission || "",
      roles: d.roles || [],
      values: d.values || [],
      workPhilosophy: d.workPhilosophy || null,
      deepBlocks: (d.deepBlocks || []).map((b) => ({ day: Number(b.day) || 1, time: b.time || "09:00", minutes: Number(b.minutes) || 90, projectId: b.projectId || "" })),
    };
  });
  const col = (n) => collection(db, "users", uid, n);
  on(col("identities"), (s) => { S.identities = s.docs.map((d) => ({ id: d.id, statement: d.data().statement || "" })); });
  on(col("habits"), (s) => { S.habits = s.docs.map((d) => normHabit(d.id, d.data())).sort((a, b) => (a.cue.time || "99").localeCompare(b.cue.time || "99")); });
  on(col("habitLogs"), (s) => {
    const L = {};
    s.docs.forEach((d) => { const x = d.data(); if (x.habitId && x.date) (L[x.habitId] ||= {})[x.date] = x.status; });
    S.logs = L;
  });
  on(col("projects"), (s) => { S.projects = s.docs.map((d) => normProject(d.id, d.data())); });
  on(col("sessions"), (s) => {
    S.sessions = s.docs.map((d) => { const x = d.data(); return { id: d.id, projectId: x.projectId || "", start: Number(x.start) || 0, minutes: Number(x.minutes) || 0, ritual: x.ritualChecks || [], kind: x.kind || "deep" }; })
      .sort((a, b) => b.start - a.start);
  });
  on(col("weeks"), (s) => { S.weeks = {}; s.docs.forEach((d) => { S.weeks[d.id] = d.data(); }); });
  on(col("outputs"), (s) => { S.outputs = s.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.date || "").localeCompare(a.date || "")); });
}

/* ---------- Yazmalar ---------- */
async function setLog(hid, date, status) {
  const ref = doc(colRef("habitLogs"), `${hid}_${date}`);
  if (!status) return deleteDoc(ref);
  return setDoc(ref, { habitId: hid, date, status, at: serverTimestamp() });
}

function markHabit(hid, status, date = todayKey()) {
  const cur = st(hid, date);
  setLog(hid, date, cur === status ? null : status).catch(fail);
}

const fail = (e) => toast(e?.code === "permission-denied" ? RULES_MSG : (e?.message || String(e)));

/* ---------- Derin seans (tarayıcıda, sekme açık kaldığı sürece) ---------- */
const RUN_KEY = "gelisio_run";
function getRun() { try { return JSON.parse(localStorage.getItem(RUN_KEY) || "null"); } catch { return null; } }
function setRun(v) { try { if (v) localStorage.setItem(RUN_KEY, JSON.stringify(v)); else localStorage.removeItem(RUN_KEY); } catch { /* özel pencere */ } }

async function finishRun() {
  const r = getRun();
  if (!r) return;
  const p = S.projects.find((x) => x.id === r.projectId);
  const minutes = Math.max(1, Math.round((Date.now() - r.start) / 60000));
  await setDoc(doc(colRef("sessions"), newId("sessions")), {
    projectId: r.projectId, start: r.start, end: r.start + minutes * 60000, minutes, ritualChecks: r.ritual,
    kind: p?.kind === "hobby" ? "practice" : "deep",
  });
  setRun(null);
  toast(`${minutes} dk kaydedildi`);
  scheduleRender();
}

/* ---------- Bildirim ---------- */
let toastTimer;
function toast(msg) {
  const el = $("#toast");
  el.textContent = msg;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 2600);
}

/* ---------- Görünümler ---------- */
const chain = (h, t, n = 14) => {
  let html = "";
  for (let i = n - 1; i >= 0; i--) html += `<span class="c ${cellOf(h, addDays(t, -i), t)}"></span>`;
  return `<div class="chain" aria-label="Son ${n} gün">${html}</div>`;
};

const cueText = (h) => {
  if (h.cue.type === "stack") {
    const other = S.habits.find((x) => x.id === h.cue.afterHabitId);
    return other ? `Şundan sonra: ${esc(other.name)}` : "İstifleme işareti";
  }
  return [h.cue.time, h.cue.place].filter(Boolean).map(esc).join(" · ") || "İşaret yok";
};

const statusButtons = (h, date) => {
  const s = st(h.id, date);
  return `<div class="seg-group">
    <button class="seg ${s === "full" ? "on" : ""}" data-a="mark" data-id="${h.id}" data-s="full" data-d="${date}">Tam</button>
    <button class="seg ${s === "min" ? "on" : ""}" data-a="mark" data-id="${h.id}" data-s="min" data-d="${date}">Min</button>
    <button class="seg ${s === "skip" ? "on" : ""}" data-a="mark" data-id="${h.id}" data-s="skip" data-d="${date}">Atla</button>
  </div>`;
};

function viewToday() {
  const t = todayKey();
  const hs = activeHabits().filter((h) => scheduled(h, t));
  const doneN = hs.filter((h) => isDone(st(h.id, t))).length;
  const missed = activeHabits().filter((h) => missedYesterday(h, t) && st(h.id, t) == null);
  const nudges = activeHabits().map((h) => [h, goldilocks(h, t)]).filter((x) => x[1]);
  const blocks = S.profile.deepBlocks.filter((b) => b.day === dow(t));
  const projName = (id) => S.projects.find((p) => p.id === id)?.title || "";

  return `
  <header class="head"><div><p class="eyebrow">${esc(fmtLong(t))}</p><h1>Bugün</h1></div></header>
  ${S.profile.mission ? `<p class="mission">${esc(S.profile.mission)}</p>` : ""}

  <div class="glass card">
    <div class="hr">
      <div><span class="big">${doneN}/${hs.length}</span><span class="muted">bugünkü alışkanlık</span></div>
      <a class="ghost" href="#/habit/new">+ Alışkanlık</a>
    </div>
    ${blocks.length ? `<div><h3>Derin bloklar</h3><div class="checks">${blocks.map((b) =>
      `<span class="tag mint">${esc(b.time)} · ${b.minutes} dk${b.projectId ? " · " + esc(projName(b.projectId)) : ""}</span>`).join("")}</div></div>` : ""}
  </div>

  ${missed.length ? `<section class="section"><h3>Dün kaçırdın</h3>${missed.map((h) => `
    <div class="glass card nudge">
      <div><h2>${esc(h.name)}</h2><p class="muted">${h.twoMinVersion ? esc(h.twoMinVersion) : "Bugün en küçük sürümünü yap."}</p></div>
      <div class="seg-group" style="justify-self:start">
        <button class="seg" data-a="mark" data-id="${h.id}" data-s="min" data-d="${t}">2 dk sürümü yaptım</button>
      </div>
    </div>`).join("")}</section>` : ""}

  ${nudges.length ? `<section class="section"><h3>Ayar önerisi</h3>${nudges.map(([h, dir]) => `
    <div class="glass card nudge good">
      <div><h2>${esc(h.name)}</h2><p class="muted">${dir === "up"
        ? "Son 14 günde %95 üstü tutarlılık. Zorluğu bir kademe artırmak için kabul et."
        : "Son 14 günde %60 altı. Bu hafta 2 dakikalık sürüme in ve kabul et."}</p></div>
      <div class="hr"><button class="ghost" data-a="accept" data-id="${h.id}" data-dir="${dir}">Kabul et</button></div>
    </div>`).join("")}</section>` : ""}

  <section class="section">
    <h3>Alışkanlıklar</h3>
    ${hs.length ? hs.map((h) => `
      <div class="glass item" style="display:grid;gap:12px">
        <div class="hr">
          <a href="#/habit/${h.id}" class="grow"><h2 style="margin:0">${esc(h.name)}</h2>
            <span class="muted">${cueText(h)}</span></a>
          ${statusButtons(h, t)}
        </div>
        ${chain(h, t)}
      </div>`).join("")
      : `<div class="glass empty"><p class="muted">Bugün planlı alışkanlık yok.</p><a class="primary" href="#/habit/new">İlk alışkanlığını ekle</a></div>`}
  </section>`;
}

function viewHabits() {
  const t = todayKey();
  const hs = activeHabits();
  const groups = S.identities
    .map((i) => ({ i, list: hs.filter((h) => h.identityId === i.id) }))
    .filter((g) => g.list.length);
  const loose = hs.filter((h) => !S.identities.some((i) => i.id === h.identityId));
  const card = (h) => {
    const c = consistency(h, t);
    return `<a class="glass item" href="#/habit/${h.id}">
      <div class="grow"><h2 style="margin:0;font-size:16px">${esc(h.name)}</h2>
        <span class="muted">${cueText(h)} · ${stageOf(reps(h))}</span></div>
      <div style="text-align:right"><span class="big" style="font-size:20px">${pct(c)}</span><span class="faint">tutarlılık</span></div>
    </a>`;
  };
  return `
  <header class="head"><div><p class="eyebrow">${activeHabits().length} aktif</p><h1>Alışkanlık</h1></div>
    <a class="primary" href="#/habit/new">Yeni</a></header>
  ${!hs.length ? `<div class="glass empty"><p class="muted">Henüz alışkanlık yok. Kimlik seç, küçük bir sürüm tanımla, işaretini koy.</p><a class="primary" href="#/habit/new">Başla</a></div>` : ""}
  ${groups.map((g) => `<section class="section"><div><p class="eyebrow">Kimlik</p><h2>${esc(g.i.statement)}</h2>
    <span class="muted">${votes(g.i.id)} oy</span></div>${g.list.map(card).join("")}</section>`).join("")}
  ${loose.length ? `<section class="section"><h3>Kimliksiz</h3>${loose.map(card).join("")}</section>` : ""}`;
}

function viewHabitDetail(id) {
  const h = S.habits.find((x) => x.id === id);
  if (!h) return notFound("Alışkanlık bulunamadı");
  const t = todayKey();
  const id2 = S.identities.find((i) => i.id === h.identityId);
  const proj = S.projects.find((p) => p.id === h.projectId);
  const c = consistency(h, t);
  const r = reps(h);
  const chain30 = (() => {
    let html = "";
    for (let i = 29; i >= 0; i--) html += `<span class="c ${cellOf(h, addDays(t, -i), t)}"></span>`;
    return `<div class="chain" style="gap:5px">${html}</div>`;
  })();
  return `
  <a class="back" href="#/habits">‹ Alışkanlık</a>
  <header class="head" style="margin-bottom:8px"><div><h1 style="font-size:26px">${esc(h.name)}</h1>
    ${h.archived ? `<span class="tag warn">Arşivde</span>` : ""}</div></header>

  <div class="stats">
    <div class="stat"><span class="faint">Tutarlılık</span><span class="big">${pct(c)}</span></div>
    <div class="stat"><span class="faint">Seri</span><span class="big">${streak(h, t)}</span></div>
    <div class="stat"><span class="faint">${stageOf(r)}</span><span class="big">${r}</span></div>
  </div>

  <div class="glass card">
    <div class="hr"><h3 style="margin:0">Bugün</h3>${statusButtons(h, t)}</div>
    <div><h3>Son 30 gün</h3>${chain30}
      <p class="faint" style="margin-top:8px">Yeşil: tam · mor: min · sarı: tek kaçırma · kırmızı: art arda kaçırma</p></div>
  </div>

  <div class="glass card">
    ${id2 ? `<div><h3>Kimlik</h3><p>${esc(id2.statement)}</p><p class="faint">${votes(id2.id)} oy</p></div>` : ""}
    <div><h3>Tam sürüm</h3><p>${esc(h.fullVersion) || "—"}</p></div>
    <div><h3>2 dakikalık sürüm</h3><p>${esc(h.twoMinVersion) || "—"}</p></div>
    <div><h3>İşaret</h3><p>${cueText(h)}</p></div>
    <div><h3>Ödül</h3><p>${esc(h.reward) || "—"}</p></div>
    ${proj ? `<div><h3>Bağlı proje</h3><a class="tag mint" href="#/project/${proj.id}">${esc(proj.title)}</a></div>` : ""}
  </div>

  <div class="row2" style="margin-top:14px">
    <a class="ghost" href="#/habit/${h.id}/edit" style="text-align:center">Düzenle</a>
    <button class="ghost" data-a="archHabit" data-id="${h.id}">${h.archived ? "Arşivden çıkar" : "Arşivle"}</button>
  </div>
  <button class="danger block" style="margin-top:12px" data-a="delHabit" data-id="${h.id}">Alışkanlığı sil</button>`;
}

function viewHabitForm(id) {
  const h = id ? S.habits.find((x) => x.id === id) : null;
  if (id && !h) return notFound("Alışkanlık bulunamadı");
  const cur = h || { name: "", identityId: "", fullVersion: "", twoMinVersion: "", cue: { type: "time", time: "", place: "", afterHabitId: "" }, reward: "", days: [1, 2, 3, 4, 5, 6, 7], projectId: "" };
  const others = S.habits.filter((x) => x.id !== id && !x.archived);
  return `
  <a class="back" href="${h ? `#/habit/${h.id}` : "#/habits"}">‹ Geri</a>
  <h1 style="font-size:26px;margin-bottom:14px">${h ? "Düzenle" : "Yeni alışkanlık"}</h1>
  <form class="glass card" data-f="habit">
    <input type="hidden" name="id" value="${h?.id || ""}">
    <label class="field">Ad<input name="name" required value="${esc(cur.name)}" placeholder="Örn. Sabah 10 sayfa okuma"></label>
    <label class="field">Kimlik
      <select name="identityId">
        <option value="">— yok —</option>
        ${S.identities.map((i) => `<option value="${i.id}" ${i.id === cur.identityId ? "selected" : ""}>${esc(i.statement)}</option>`).join("")}
      </select>
    </label>
    <label class="field">Tam sürüm<input name="fullVersion" value="${esc(cur.fullVersion)}" placeholder="Ne yapacağını net yaz"></label>
    <label class="field">2 dakikalık sürüm<input name="twoMinVersion" value="${esc(cur.twoMinVersion)}" placeholder="Kötü günde bunu yap"></label>
    <label class="field">İşaret türü
      <select name="cueType">
        <option value="time" ${cur.cue.type !== "stack" ? "selected" : ""}>Saat ve yer</option>
        <option value="stack" ${cur.cue.type === "stack" ? "selected" : ""}>Şundan sonra</option>
      </select>
    </label>
    <div class="row2">
      <label class="field">Saat<input type="time" name="cueTime" value="${esc(cur.cue.time)}"></label>
      <label class="field">Yer<input name="cuePlace" value="${esc(cur.cue.place)}" placeholder="Mutfak masası"></label>
    </div>
    <label class="field">Şundan sonra (alışkanlık)
      <select name="afterHabitId">
        <option value="">— seç —</option>
        ${others.map((o) => `<option value="${o.id}" ${o.id === cur.cue.afterHabitId ? "selected" : ""}>${esc(o.name)}</option>`).join("")}
      </select>
    </label>
    <label class="field">Ödül<input name="reward" value="${esc(cur.reward)}"></label>
    <div class="field">Günler
      <div class="checks">${DAYS.map((d, i) => `<label><input type="checkbox" name="days" value="${i + 1}" ${cur.days.includes(i + 1) ? "checked" : ""}>${d}</label>`).join("")}</div>
    </div>
    <label class="field">Bağlı proje/hobi
      <select name="projectId">
        <option value="">— yok —</option>
        ${S.projects.filter((p) => !p.archived).map((p) => `<option value="${p.id}" ${p.id === cur.projectId ? "selected" : ""}>${esc(p.title)}</option>`).join("")}
      </select>
    </label>
    <button class="primary block" type="submit">Kaydet</button>
  </form>`;
}

function projectCard(p, t) {
  const pctDone = Math.round(milestonePct(p) * 100);
  const wk = p.kind === "hobby" ? weeklyPractice(p, t) : weekDeepMinutes(t, p.id);
  const target = p.kind === "hobby" ? p.weeklyPracticeMin : (p.leadMeasure.weeklyTarget * 60);
  return `<a class="glass item" href="#/project/${p.id}" style="display:grid;gap:10px">
    <div class="hr"><div class="grow"><h2 style="margin:0;font-size:17px">${esc(p.title)}</h2>
      <span class="muted">${DIM[p.dimension] || ""}${p.kind === "hobby" ? ` · Seviye ${p.skillLevel}/10` : ""}</span></div>
      <span class="tag ${pctDone === 100 ? "mint" : ""}">${p.milestones.length ? pctDone + "%" : "—"}</span></div>
    ${bar(target ? wk / target : 0)}
    <span class="faint">${p.kind === "hobby" ? "Bu hafta pratik" : "Bu hafta derin"}: ${Math.round(wk)} / ${target} dk</span>
  </a>`;
}

function viewProjects(kind) {
  const t = todayKey();
  const list = S.projects.filter((p) => !p.archived && p.kind === kind);
  return `
  <header class="head"><div><p class="eyebrow">${kind === "hobby" ? "Hobi ve beceri" : "Hedefe bağlı işler"}</p><h1>${kind === "hobby" ? "Hobiler" : "Projeler"}</h1></div>
    <a class="primary" href="#/project/new/${kind}">Yeni</a></header>
  <div class="seg-tabs" style="margin-bottom:16px">
    <a href="#/projects/project" class="${kind === "project" ? "on" : ""}">Projeler</a>
    <a href="#/projects/hobby" class="${kind === "hobby" ? "on" : ""}">Hobiler</a>
  </div>
  ${list.length ? `<div class="stack">${list.map((p) => projectCard(p, t)).join("")}</div>`
    : `<div class="glass empty"><p class="muted">${kind === "hobby" ? "Bir beceri seç ve haftalık pratik hedefini koy." : "Bir proje ekle; kilometre taşlarını işaretle."}</p><a class="primary" href="#/project/new/${kind}">Ekle</a></div>`}`;
}

function viewProjectDetail(id) {
  const p = S.projects.find((x) => x.id === id);
  if (!p) return notFound("Proje bulunamadı");
  const t = todayKey();
  const run = getRun();
  const running = run && run.projectId === p.id;
  const sessions = S.sessions.filter((s) => s.projectId === p.id).slice(0, 12);
  const outs = S.outputs.filter((o) => o.projectId === p.id);
  const linked = activeHabits().filter((h) => h.projectId === p.id);
  const wk = p.kind === "hobby" ? weeklyPractice(p, t) : weekDeepMinutes(t, p.id);
  const target = p.kind === "hobby" ? p.weeklyPracticeMin : p.leadMeasure.weeklyTarget * 60;
  const ready = levelUpReady(p, t);

  return `
  <a class="back" href="#/projects/${p.kind}">‹ ${p.kind === "hobby" ? "Hobiler" : "Projeler"}</a>
  <header class="head" style="margin-bottom:8px"><div><h1 style="font-size:26px">${esc(p.title)}</h1>
    <span class="muted">${DIM[p.dimension] || ""}${p.dueDate ? " · Hedef: " + fmtShort(p.dueDate) : ""}</span></div></header>

  ${running ? `<div class="glass runbar"><div class="hr" style="gap:10px"><span class="ring-dot"></span><span class="clock" id="runClock">${fmtClock(Date.now() - run.start)}</span></div>
    <div class="row2" style="grid-template-columns:auto auto;gap:8px"><button class="primary" data-a="finishRun">Bitir ve kaydet</button><button class="ghost" data-a="cancelRun">Vazgeç</button></div></div>` : ""}

  <div class="glass card">
    <div class="hr"><div><h3 style="margin:0">Bu hafta</h3><span class="faint">${Math.round(wk)} / ${target} dk</span></div>
      <span class="big" style="font-size:22px">${target ? pct(wk / target) : "—"}</span></div>
    ${bar(target ? wk / target : 0)}
    ${p.kind === "hobby" ? `
      <div class="hr"><span>Seviye ${p.skillLevel}/10</span>
        <div class="seg-group"><button class="seg" data-a="skill" data-id="${p.id}" data-d="-1" ${p.skillLevel <= 1 ? "disabled" : ""}>−</button>
        <button class="seg" data-a="skill" data-id="${p.id}" data-d="1" ${p.skillLevel >= 10 ? "disabled" : ""}>+</button></div></div>
      ${ready ? `<div class="glass nudge good" style="padding:14px 16px"><p>4 haftadır pratik hedefini tutuyorsun. Seviye atlamaya hazırsın.</p>
        <button class="primary block" style="margin-top:10px" data-a="skill" data-id="${p.id}" data-d="1">Seviye +1</button></div>` : ""}
      ${p.outputType ? `<p class="faint">Üretim türü: ${esc(p.outputType)}</p>` : ""}` : ""}
  </div>

  ${!running ? `
  <section class="section"><h3>Derin seans</h3>
    <form class="glass card" data-f="startRun">
      <input type="hidden" name="projectId" value="${p.id}">
      <p class="muted">Başlatmadan önce üç ritüeli onayla.</p>
      <div class="checks">
        <label><input type="checkbox" name="ritual" value="phone">Telefon sessiz ve uzakta</label>
        <label><input type="checkbox" name="ritual" value="one">Tek iş açık</label>
        <label><input type="checkbox" name="ritual" value="goal">Hedef yazılı</label>
      </div>
      <button class="primary block" type="submit">Seansı başlat</button>
    </form>
  </section>` : ""}

  <section class="section"><h3>Manuel seans ekle</h3>
    <form class="glass card" data-f="session">
      <input type="hidden" name="projectId" value="${p.id}">
      <div class="row2">
        <label class="field">Dakika<input type="number" name="minutes" min="1" max="600" required value="30"></label>
        <label class="field">Tür<input value="${p.kind === "hobby" ? "Pratik" : "Derin"}" disabled></label>
      </div>
      <button class="ghost block" type="submit">Ekle</button>
    </form>
  </section>

  <section class="section"><h3>Kilometre taşları</h3>
    <div class="glass card">
      ${p.milestones.length ? p.milestones.map((m, i) => `
        <label class="inline-check"><input type="checkbox" data-a="ms" data-pid="${p.id}" data-i="${i}" ${m.done ? "checked" : ""}>
          <span style="${m.done ? "text-decoration:line-through;color:var(--text2)" : ""}">${esc(m.title)}</span></label>`).join("")
        : `<p class="muted">Kilometre taşı yok. Düzenle ekranından ekleyebilirsin.</p>`}
    </div>
  </section>

  ${linked.length ? `<section class="section"><h3>Bağlı alışkanlıklar</h3>${linked.map((h) => `
    <a class="glass item" href="#/habit/${h.id}"><div class="grow"><h2 style="margin:0;font-size:16px">${esc(h.name)}</h2>
    <span class="muted">${stageOf(reps(h))} · ${pct(consistency(h, t))}</span></div>${chain(h, t, 7)}</a>`).join("")}</section>` : ""}

  <section class="section"><h3>Seanslar</h3>
    ${sessions.length ? sessions.map((s) => `<div class="glass item">
      <div class="grow"><span>${fmtShort(sessDate(s))}</span> <span class="muted">· ${s.minutes} dk · ${s.kind === "practice" ? "pratik" : "derin"}</span></div>
      <button class="linkish" data-a="delSession" data-id="${s.id}">Sil</button></div>`).join("")
      : `<p class="muted">Henüz seans yok.</p>`}
  </section>

  ${p.kind === "hobby" ? `<section class="section"><h3>Üretimler</h3>
    <form class="glass card" data-f="output">
      <input type="hidden" name="projectId" value="${p.id}">
      <label class="field">Ne ürettin?<textarea name="note" required placeholder="Kısa not"></textarea></label>
      <button class="ghost block" type="submit">Kaydet</button>
    </form>
    ${outs.map((o) => `<div class="glass item"><div class="grow"><p>${esc(o.note)}</p><span class="faint">${fmtShort(o.date)}</span></div>
      <button class="linkish" data-a="delOutput" data-id="${o.id}">Sil</button></div>`).join("")}
  </section>` : ""}

  <div class="row2" style="margin-top:22px">
    <a class="ghost" href="#/project/${p.id}/edit" style="text-align:center">Düzenle</a>
    <button class="ghost" data-a="archProj" data-id="${p.id}">Arşivle</button>
  </div>
  <button class="danger block" style="margin-top:12px" data-a="delProj" data-id="${p.id}">Projeyi sil</button>`;
}

function viewProjectForm(id, kindArg) {
  const p = id ? S.projects.find((x) => x.id === id) : null;
  if (id && !p) return notFound("Proje bulunamadı");
  const kind = p?.kind || kindArg || "project";
  const cur = p || { title: "", identityId: "", dimension: "physical", dueDate: "", milestones: [], leadMeasure: { unit: "derin saat", weeklyTarget: 10 }, outputType: "", weeklyPracticeMin: 120, skillLevel: 1 };
  return `
  <a class="back" href="${p ? `#/project/${p.id}` : `#/projects/${kind}`}">‹ Geri</a>
  <h1 style="font-size:26px;margin-bottom:14px">${p ? "Düzenle" : kind === "hobby" ? "Yeni hobi" : "Yeni proje"}</h1>
  <form class="glass card" data-f="project">
    <input type="hidden" name="id" value="${p?.id || ""}">
    <input type="hidden" name="kind" value="${kind}">
    <label class="field">Ad<input name="title" required value="${esc(cur.title)}"></label>
    <label class="field">Kimlik
      <select name="identityId"><option value="">— yok —</option>
        ${S.identities.map((i) => `<option value="${i.id}" ${i.id === cur.identityId ? "selected" : ""}>${esc(i.statement)}</option>`).join("")}</select>
    </label>
    <label class="field">Boyut
      <select name="dimension">${Object.entries(DIM).map(([k, v]) => `<option value="${k}" ${k === cur.dimension ? "selected" : ""}>${v}</option>`).join("")}</select>
    </label>
    <label class="field">Kilometre taşları (her satıra bir tane)<textarea name="milestones">${esc(cur.milestones.map((m) => m.title).join("\n"))}</textarea></label>
    <div class="row2">
      <label class="field">Haftalık ölçü birimi<input name="leadUnit" value="${esc(cur.leadMeasure.unit)}"></label>
      <label class="field">Haftalık hedef<input type="number" name="leadTarget" min="0" step="0.5" value="${cur.leadMeasure.weeklyTarget}"></label>
    </div>
    ${kind === "hobby" ? `
      <div class="row2">
        <label class="field">Haftalık pratik (dk)<input type="number" name="weeklyPracticeMin" min="0" step="15" value="${cur.weeklyPracticeMin}"></label>
        <label class="field">Seviye (1–10)<input type="number" name="skillLevel" min="1" max="10" value="${cur.skillLevel}"></label>
      </div>
      <label class="field">Üretim türü<input name="outputType" value="${esc(cur.outputType)}" placeholder="Örn. resim, şiir, parça"></label>` : ""}
    <label class="field">Hedef tarihi<input type="date" name="dueDate" value="${cur.dueDate || ""}"></label>
    <button class="primary block" type="submit">Kaydet</button>
  </form>`;
}

function viewWeek() {
  const t = todayKey();
  const wk = isoWeek(t);
  const review = S.weeks[wk]?.review || {};
  const days = weekDays(t);
  const hs = activeHabits();
  let sched = 0, done = 0;
  hs.forEach((h) => days.forEach((d) => { if (d <= t && scheduled(h, d)) { sched++; if (isDone(st(h.id, d))) done++; } }));
  const deep = weekDeepMinutes(t);
  return `
  <header class="head"><div><p class="eyebrow">${fmtShort(weekStartKey(t))} – ${fmtShort(addDays(weekStartKey(t), 6))}</p><h1>Hafta</h1></div></header>

  <div class="stats">
    <div class="stat"><span class="faint">Derin saat</span><span class="big">${(deep / 60).toFixed(1)}</span></div>
    <div class="stat"><span class="faint">Alışkanlık</span><span class="big">${done}/${sched}</span></div>
    <div class="stat"><span class="faint">Proje</span><span class="big">${S.projects.filter((p) => !p.archived).length}</span></div>
  </div>

  <section class="section"><h3>Projeler bu hafta</h3>
    ${S.projects.filter((p) => !p.archived).map((p) => {
      const v = p.kind === "hobby" ? weeklyPractice(p, t) : weekDeepMinutes(t, p.id);
      const target = p.kind === "hobby" ? p.weeklyPracticeMin : p.leadMeasure.weeklyTarget * 60;
      return `<a class="glass item" href="#/project/${p.id}" style="display:grid;gap:8px">
        <div class="hr"><span>${esc(p.title)}</span><span class="faint">${Math.round(v)} / ${target} dk</span></div>${bar(target ? v / target : 0)}</a>`;
    }).join("") || `<p class="muted">Henüz proje yok.</p>`}
  </section>

  <section class="section"><h3>Derin bloklar</h3>
    <div class="glass card">${S.profile.deepBlocks.length ? S.profile.deepBlocks.map((b) =>
      `<div class="hr"><span>${DAYS[b.day - 1] || ""} · ${esc(b.time)}</span><span class="muted">${b.minutes} dk</span></div>`).join("")
      : `<p class="muted">Derin blok şablonu Android uygulamasında düzenleniyor.</p>`}</div>
  </section>

  <section class="section"><h3>Haftalık gözden geçirme · ${wk}</h3>
    <form class="glass card" data-f="review">
      <label class="field">Neyi iyi yaptım?<textarea name="worked">${esc(review.worked)}</textarea></label>
      <label class="field">Neyi yapamadım?<textarea name="didnt">${esc(review.didnt)}</textarea></label>
      <label class="field">Gelecek hafta ne değişecek?<textarea name="change">${esc(review.change)}</textarea></label>
      <button class="primary block" type="submit">Kaydet</button>
    </form>
  </section>`;
}

function viewMe() {
  const p = S.profile;
  return `
  <header class="head"><div><p class="eyebrow">${esc(S.user?.email || "")}</p><h1>Ben</h1></div></header>

  <section class="section"><h3>Misyon, roller, değerler</h3>
    <form class="glass card" data-f="profile">
      <label class="field">Misyon<textarea name="mission">${esc(p.mission)}</textarea></label>
      <label class="field">Roller (her satıra bir tane, en fazla 7)<textarea name="roles">${esc(p.roles.join("\n"))}</textarea></label>
      <label class="field">Değerler (her satıra bir tane)<textarea name="values">${esc(p.values.join("\n"))}</textarea></label>
      <label class="field">Çalışma felsefesi
        <select name="workPhilosophy"><option value="">— seç —</option>
          ${PHIL.map(([k, v]) => `<option value="${k}" ${k === p.workPhilosophy ? "selected" : ""}>${v}</option>`).join("")}</select>
      </label>
      <button class="primary block" type="submit">Kaydet</button>
    </form>
  </section>

  <section class="section"><h3>Kimlikler</h3>
    ${S.identities.map((i) => `<div class="glass item"><div class="grow"><p>${esc(i.statement)}</p>
      <span class="faint">${votes(i.id)} oy</span></div>
      <button class="linkish" data-a="delIdentity" data-id="${i.id}">Sil</button></div>`).join("") || `<p class="muted">Henüz kimlik yok.</p>`}
    <form class="glass card" data-f="identity">
      <label class="field">Yeni kimlik cümlesi<input name="statement" required placeholder="Ben … biriyim"></label>
      <button class="ghost block" type="submit">Ekle</button>
    </form>
  </section>

  <section class="section">
    <button class="ghost block" data-a="logout">Çıkış yap</button>
    <p class="faint" style="text-align:center">Bildirimler ve Android'e özgü özellikler bu web sürümünde yok.</p>
  </section>`;
}

function viewLogin() {
  return `
  <main class="login"><div class="glass login-card">
    <img src="icon.png" alt="">
    <div><h1>Gelisio</h1><p class="muted" style="margin-top:8px">Alışkanlıkların, projelerin ve derin çalışman tek yerde. Telefonla aynı hesap, aynı veri.</p></div>
    ${S.error ? `<p class="error">${esc(S.error)}</p>` : ""}
    <button class="primary block" data-a="login">Google ile giriş yap</button>
  </div></main>`;
}

const notFound = (msg) => `<a class="back" href="#/today">‹ Bugün</a><div class="glass empty"><p class="muted">${msg}</p></div>`;

function runBar() {
  const r = getRun();
  if (!r) return "";
  const p = S.projects.find((x) => x.id === r.projectId);
  if (!p) return "";
  return `<a href="#/project/${p.id}" class="glass runbar"><span class="grow"><span class="ring-dot" style="display:inline-block;margin-right:8px"></span>${esc(p.title)}</span>
    <span class="clock" id="runClock">${fmtClock(Date.now() - r.start)}</span></a>`;
}

/* ---------- Yönlendirme ---------- */
const parts = () => location.hash.replace(/^#\/?/, "").split("/").filter(Boolean);

function render() {
  const bar = $("#tabbar");
  if (!S.user) {
    $("#app").innerHTML = viewLogin();
    bar.innerHTML = "";
    return;
  }
  const [a = "", b = "", c = ""] = parts();
  let body;
  let active = "today";
  switch (a) {
    case "habits": active = "habits"; body = viewHabits(); break;
    case "habit":
      active = "habits";
      body = b === "new" ? viewHabitForm(null) : c === "edit" ? viewHabitForm(b) : viewHabitDetail(b);
      break;
    case "projects": active = "projects"; body = viewProjects(b === "hobby" ? "hobby" : "project"); break;
    case "project":
      active = "projects";
      body = b === "new" ? viewProjectForm(null, c) : c === "edit" ? viewProjectForm(b) : viewProjectDetail(b);
      break;
    case "week": active = "week"; body = viewWeek(); break;
    case "me": active = "me"; body = viewMe(); break;
    default: body = viewToday();
  }
  const err = S.error ? `<p class="error" style="margin-bottom:14px">${esc(S.error)}</p>` : "";
  $("#app").innerHTML = `<main>${err}${runBar()}${body}</main>`;
  bar.innerHTML = TABS.map((t) => `<a href="${t.href}" class="${t.id === active ? "on" : ""}" aria-current="${t.id === active ? "page" : "false"}">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="${t.icon}"/></svg>${t.label}</a>`).join("");
}

function scheduleRender() {
  if (renderQueued) return;
  renderQueued = true;
  requestAnimationFrame(() => {
    renderQueued = false;
    if (isEditing()) { deferred = true; return; }
    render();
  });
}

function isEditing() {
  const el = document.activeElement;
  return !!el && !!el.closest?.("form[data-f]") && ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName);
}

document.addEventListener("focusout", () => {
  if (!deferred) return;
  setTimeout(() => { if (!isEditing()) { deferred = false; render(); } }, 150);
});

/* ---------- Eylemler ---------- */
const A = {
  login: async () => { await signInWithPopup(auth, new GoogleAuthProvider()); },
  logout: () => signOut(auth),
  mark: (el) => markHabit(el.dataset.id, el.dataset.s, el.dataset.d),
  accept: async (el) => {
    const h = S.habits.find((x) => x.id === el.dataset.id);
    if (!h) return;
    const t = todayKey();
    const data = { ...h, goldilocksAt: t, days: h.days, createdAt: h.createdAt, difficulty: el.dataset.dir === "up" ? Math.min(5, h.difficulty + 1) : h.difficulty };
    await saveHabit(data);
    toast(el.dataset.dir === "up" ? "Zorluk artırıldı" : "2 dakikalık sürüm bu hafta için");
  },
  archHabit: async (el) => {
    const h = S.habits.find((x) => x.id === el.dataset.id);
    if (h) await saveHabit({ ...h, archived: !h.archived });
  },
  delHabit: async (el) => {
    if (!confirm("Alışkanlık ve tüm kayıtları silinsin mi?")) return;
    const id = el.dataset.id;
    await deleteDoc(doc(colRef("habits"), id));
    Object.keys(S.logs[id] || {}).forEach((date) => deleteDoc(doc(colRef("habitLogs"), `${id}_${date}`)));
    location.hash = "#/habits";
  },
  ms: async (el) => {
    const p = S.projects.find((x) => x.id === el.dataset.pid);
    if (!p) return;
    const milestones = p.milestones.map((m, i) => (i === Number(el.dataset.i) ? { ...m, done: el.checked } : m));
    await setDoc(doc(colRef("projects"), p.id), { ...projectToDoc(p), milestones }, { merge: true });
  },
  skill: async (el) => {
    const p = S.projects.find((x) => x.id === el.dataset.id);
    if (!p) return;
    const lvl = Math.max(1, Math.min(10, p.skillLevel + Number(el.dataset.d)));
    await setDoc(doc(colRef("projects"), p.id), { skillLevel: lvl }, { merge: true });
  },
  archProj: async (el) => {
    const p = S.projects.find((x) => x.id === el.dataset.id);
    if (p) await setDoc(doc(colRef("projects"), p.id), { archived: true }, { merge: true });
    location.hash = `#/projects/${p?.kind || "project"}`;
  },
  delProj: async (el) => {
    if (!confirm("Proje silinsin mi? Bağlı alışkanlıklar kalır, seanslar silinmez.")) return;
    await deleteDoc(doc(colRef("projects"), el.dataset.id));
    location.hash = "#/projects/project";
  },
  delSession: (el) => deleteDoc(doc(colRef("sessions"), el.dataset.id)),
  delOutput: (el) => deleteDoc(doc(colRef("outputs"), el.dataset.id)),
  delIdentity: (el) => { if (confirm("Kimlik silinsin mi?")) deleteDoc(doc(colRef("identities"), el.dataset.id)); },
  finishRun: () => finishRun().catch(fail),
  cancelRun: () => { setRun(null); scheduleRender(); },
};

/* Proje alanlarını Store.kt'deki şekilde yazmak için. */
const projectToDoc = (p) => ({
  kind: p.kind, title: p.title, goalId: p.goalId, role: p.role,
  milestones: p.milestones.map((m) => ({ title: m.title, done: m.done })),
  leadMeasure: { unit: p.leadMeasure.unit, weeklyTarget: p.leadMeasure.weeklyTarget },
  dueDate: p.dueDate || null, identityId: p.identityId, dimension: p.dimension,
  skillLevel: p.skillLevel, weeklyPracticeMin: p.weeklyPracticeMin, outputType: p.outputType, archived: p.archived,
});

async function saveHabit(h) {
  const id = h.id || newId("habits");
  await setDoc(doc(colRef("habits"), id), {
    name: h.name.trim(), identityId: h.identityId || "", type: "build",
    fullVersion: (h.fullVersion || "").trim(), twoMinVersion: (h.twoMinVersion || "").trim(),
    cue: { type: h.cue.type, time: h.cue.time || "", place: h.cue.place || "", afterHabitId: h.cue.afterHabitId || "" },
    reward: (h.reward || "").trim(), days: [...h.days].sort((x, y) => x - y), difficulty: h.difficulty || 1,
    projectId: h.projectId || "", archived: !!h.archived, createdAt: h.createdAt, goldilocksAt: h.goldilocksAt || null,
  });
  return id;
}

/* ---------- Formlar ---------- */
const F = {
  habit: async (fd) => {
    const name = (fd.get("name") || "").trim();
    if (!name) return toast("Alışkanlık adı gerekli");
    const days = fd.getAll("days").map(Number);
    if (!days.length) return toast("En az bir gün seç");
    const existing = S.habits.find((x) => x.id === fd.get("id"));
    const type = fd.get("cueType") === "stack" ? "stack" : "time";
    const id = await saveHabit({
      id: existing?.id || "",
      name,
      identityId: fd.get("identityId") || "",
      fullVersion: fd.get("fullVersion") || "",
      twoMinVersion: fd.get("twoMinVersion") || "",
      cue: {
        type,
        time: type === "time" ? (fd.get("cueTime") || "") : "",
        place: type === "time" ? (fd.get("cuePlace") || "") : "",
        afterHabitId: type === "stack" ? (fd.get("afterHabitId") || "") : "",
      },
      reward: fd.get("reward") || "",
      days,
      difficulty: existing?.difficulty || 1,
      projectId: fd.get("projectId") || "",
      archived: existing?.archived || false,
      createdAt: existing?.createdAt || todayKey(),
      goldilocksAt: existing?.goldilocksAt || null,
    });
    location.hash = `#/habit/${id}`;
  },

  project: async (fd) => {
    const title = (fd.get("title") || "").trim();
    if (!title) return toast("Ad gerekli");
    const existing = S.projects.find((x) => x.id === fd.get("id"));
    const id = existing?.id || newId("projects");
    const prev = existing?.milestones || [];
    const milestones = (fd.get("milestones") || "").split("\n").map((s) => s.trim()).filter(Boolean)
      .map((t) => ({ title: t, done: prev.some((m) => m.title === t && m.done) }));
    const kind = fd.get("kind") === "hobby" ? "hobby" : "project";
    await setDoc(doc(colRef("projects"), id), {
      kind, title, goalId: existing?.goalId || "", role: existing?.role || "", milestones,
      leadMeasure: { unit: fd.get("leadUnit") || "derin saat", weeklyTarget: Number(fd.get("leadTarget")) || 10 },
      dueDate: fd.get("dueDate") || null, identityId: fd.get("identityId") || "", dimension: fd.get("dimension") || "physical",
      skillLevel: Number(fd.get("skillLevel")) || 1, weeklyPracticeMin: Number(fd.get("weeklyPracticeMin")) || 120,
      outputType: fd.get("outputType") || "", archived: existing?.archived || false,
    });
    location.hash = `#/project/${id}`;
  },

  startRun: async (fd) => {
    const ritual = fd.getAll("ritual");
    if (ritual.length < 3) return toast("Üç ritüel onayı gerekli");
    if (getRun()) return toast("Zaten bir seans çalışıyor");
    setRun({ projectId: fd.get("projectId"), start: Date.now(), ritual });
    scheduleRender();
  },

  session: async (fd) => {
    const p = S.projects.find((x) => x.id === fd.get("projectId"));
    const minutes = Math.round(Number(fd.get("minutes")));
    if (!p || !minutes || minutes < 1) return toast("Dakika gerekli");
    await setDoc(doc(colRef("sessions"), newId("sessions")), {
      projectId: p.id, start: Date.now(), end: Date.now() + minutes * 60000, minutes, ritualChecks: [],
      kind: p.kind === "hobby" ? "practice" : "deep",
    });
    toast(`${minutes} dk eklendi`);
  },

  output: async (fd) => {
    const note = (fd.get("note") || "").trim();
    if (!note) return;
    await setDoc(doc(colRef("outputs"), newId("outputs")), { projectId: fd.get("projectId"), note, date: todayKey(), photoUrl: null });
  },

  review: async (fd) => {
    await setDoc(doc(colRef("weeks"), isoWeek(todayKey())), {
      review: { worked: fd.get("worked") || "", didnt: fd.get("didnt") || "", change: fd.get("change") || "" },
    }, { merge: true });
    toast("Gözden geçirme kaydedildi");
  },

  profile: async (fd) => {
    const roles = (fd.get("roles") || "").split("\n").map((s) => s.trim()).filter(Boolean).slice(0, 7);
    const values = (fd.get("values") || "").split("\n").map((s) => s.trim()).filter(Boolean);
    await setDoc(userRef(), {
      mission: (fd.get("mission") || "").trim(), roles, values,
      workPhilosophy: fd.get("workPhilosophy") || null, schemaVersion: 2, updatedAt: serverTimestamp(),
    }, { merge: true });
    toast("Kaydedildi");
  },

  identity: async (fd) => {
    const statement = (fd.get("statement") || "").trim();
    if (!statement) return;
    await setDoc(doc(colRef("identities"), newId("identities")), { statement });
  },
};

/* ---------- Olaylar ---------- */
document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-a]");
  if (!el || el.disabled) return;
  const act = A[el.dataset.a];
  if (!act) return;
  Promise.resolve().then(() => act(el)).catch(fail);
});

document.addEventListener("submit", (e) => {
  const form = e.target.closest?.("form[data-f]");
  if (!form) return;
  e.preventDefault();
  const fn = F[form.dataset.f];
  const fd = new FormData(form);
  Promise.resolve().then(() => fn(fd)).then(() => {
    if (["session", "output", "identity", "review", "profile"].includes(form.dataset.f)) form.reset?.();
  }).catch(fail);
});

window.addEventListener("hashchange", () => { deferred = false; render(); });

setInterval(() => {
  const r = getRun();
  const el = document.getElementById("runClock");
  if (r && el) el.textContent = fmtClock(Date.now() - r.start);
}, 1000);

/* ---------- Açılış ---------- */
onAuthStateChanged(auth, (user) => {
  S.user = user;
  if (user) {
    startListening(user.uid);
    setDoc(userRef(), { displayName: user.displayName || null, email: user.email || null, lastSeenAt: serverTimestamp() }, { merge: true }).catch(() => {});
  } else {
    stopListening();
  }
  render();
});

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}
