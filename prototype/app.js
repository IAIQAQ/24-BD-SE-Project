'use strict';
const M = window.PrototypeModel;
const KEY = 'mathdrill-student-prototype-v1';
const SESSION_KEY = `${KEY}-session`;
const $ = selector => document.querySelector(selector);
const escapeHTML = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const icons = {
  sprout: '<path d="M12 21V11M12 15C5 15 3 10 3 5c7 0 9 3 9 8M12 11c0-6 4-8 9-8 0 5-3 9-9 9"/>',
  arrow: '<path d="M4 12h15m-6-6 6 6-6 6"/>', back: '<path d="m14 5-7 7 7 7"/>',
  check: '<path d="m5 12 4 4 10-10"/>', close: '<path d="m6 6 12 12M6 18 18 6"/>',
  user: '<circle cx="12" cy="8" r="3.5"/><path d="M5 21v-2a7 7 0 0 1 14 0v2"/>',
  lock: '<rect x="5" y="10" width="14" height="11" rx="3"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/>',
  book: '<path d="M12 5C8 2 4 3 3 4v15c3-1 6-1 9 1 3-2 6-2 9-1V4c-3-2-6-1-9 1Zm0 0v15"/>',
  home: '<path d="m3 10 9-7 9 7v10H3Zm6 10v-7h6v7"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  shield: '<path d="m12 3 8 3v6c0 4-5 8-8 9-3-1-8-5-8-9V6Z"/><path d="m8 12 3 3 5-6"/>',
  reset: '<path d="M4 8a8 8 0 1 1-1 7M4 3v5h5"/>',
  wand: '<path d="m4 20 11-11 4 4L8 24M14 10l4 4M5 3v5M2.5 5.5h5M19 2v4m-2-2h4" transform="translate(0 -2)"/>',
  bulb: '<path d="M8 16c0-3-3-3-3-7a7 7 0 1 1 14 0c0 4-3 4-3 7ZM9 20h6M10 23h4M12 16V9m-3 0 3 3 3-3"/>',
  logout: '<path d="M10 3H4v18h6m4-14 5 5-5 5M8 12h11"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7h.01"/>',
  save: '<path d="M4 3h13l4 4v14H3V3Zm3 0v6h9V3M7 21v-8h10v8"/>',
  alert: '<path d="m12 3 10 18H2ZM12 9v5m0 3h.01"/>',
  wifi: '<path d="M2 8a16 16 0 0 1 20 0M5 12a11 11 0 0 1 14 0m-11 4a6 6 0 0 1 8 0m-4 4h.01"/>',
  signal: '<path d="M3 20v-3m5 3v-7m5 7V9m5 11V4"/>',
  leaf: '<path d="M4 20C2 7 8 3 21 3c0 13-5 18-15 15m-2 2L16 8"/>',
};
function icon(name) { return `<svg aria-hidden="true" viewBox="0 0 24 24">${icons[name] || icons.info}</svg>`; }
function decorate() { document.querySelectorAll('[data-icon]').forEach(el => { el.innerHTML = icon(el.dataset.icon); }); }
const state = { route: 'login', data: { practices: [] }, practiceId: null, answers: [], index: 0, seconds: 0, dirty: false, saveStatus: '', storageError: false, failSave: false, pendingSubmit: false, modal: false, filter: 'all', detailId: null, detailBack: 'mistakes', busy: false };
let saveTimer, toastTimer, previousFocus;
try {
  const parsed = JSON.parse(localStorage.getItem(KEY) || 'null');
  if (parsed && Array.isArray(parsed.practices) && parsed.practices.every(p => p.id && Array.isArray(p.questions) && Array.isArray(p.draftAnswers) && p.questions.length === p.total && p.draftAnswers.length === p.total)) state.data = parsed;
  if (sessionStorage.getItem(SESSION_KEY) === 'demo-student') state.route = 'home';
} catch { state.storageError = true; }
function persist() {
  try { localStorage.setItem(KEY, JSON.stringify(state.data)); return true; }
  catch { state.storageError = true; return false; }
}
const practice = () => state.data.practices.find(p => p.id === state.practiceId);
const completed = () => state.data.practices.filter(p => p.result);
const groups = () => M.mistakeGroups(state.data.practices);
const filledCount = () => state.answers.filter(a => String(a).trim() !== '').length;
const duration = seconds => `${Math.floor(seconds / 60)}分${String(seconds % 60).padStart(2, '0')}秒`;
const formatTime = value => value ? new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }) : '尚未保存';
const formatDate = value => new Date(value).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' });
function storageWarning() { return state.storageError ? '<div class="prototype-storage-warning" role="alert">浏览器存储不可用，当前仅内存演示。刷新或关闭后无法保证恢复。</div>' : ''; }
function toast(message) { $('#toast').textContent = message; $('#toast').hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 3200); }
function header(title, back = 'home', end = '') { return `<div class="app-header"><button class="icon-button" data-action="${back}" aria-label="返回">${icon('back')}</button><h2>${title}</h2><div class="header-end">${end}</div></div>`; }
function nav(active) { return `<nav class="bottom-nav" aria-label="学生主导航">${[['home', 'home', '练习'], ['mistakes', 'book', '错题'], ['profile', 'user', '我的']].map(([route, glyph, label]) => `<button class="nav-button ${route === active ? 'active' : ''}" data-action="${route}" ${route === active ? 'aria-current="page"' : ''}>${icon(glyph)}<span>${label}</span></button>`).join('')}</nav>`; }
function showModal(content) {
  previousFocus = document.activeElement;
  state.modal = true;
  $('#app').inert = true;
  $('.guide-panel').inert = true;
  $('.studio-header').inert = true;
  $('#modal-root').innerHTML = `<div class="modal-overlay"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title">${content}</section></div>`;
  requestAnimationFrame(() => $('#modal-root button')?.focus());
}
function closeModal() {
  state.modal = false;
  $('#modal-root').innerHTML = '';
  $('#app').inert = false;
  $('.guide-panel').inert = false;
  $('.studio-header').inert = false;
  if (previousFocus?.isConnected) previousFocus.focus();
}
document.addEventListener('keydown', event => {
  if (!state.modal) return;
  if (event.key === 'Escape') { event.preventDefault(); closeModal(); }
  if (event.key === 'Tab') {
    const buttons = [...$('#modal-root').querySelectorAll('button:not([disabled]),input,a[href]')];
    const first = buttons[0], last = buttons.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
  }
});
function route(next) {
  clearTimeout(saveTimer);
  if (state.modal) closeModal();
  state.route = next;
  render();
  window.scrollTo(0, 0);
  $('#app .screen-content')?.scrollTo(0, 0);
}
function journeyStep() {
  if (state.route === 'login') return 0;
  if (['home', 'config', 'profile'].includes(state.route)) return 1;
  if (state.route === 'answer') return practice()?.savedAt ? 3 : 2;
  if (['submitting', 'checking', 'result'].includes(state.route)) return 4;
  return 5;
}
function updateGuide() {
  const step = journeyStep();
  const labels = ['登录，进入学习', '选择范围与题量', '专注作答', '保存与恢复', '提交并查看结果', '回顾错题'];
  $('#journey').innerHTML = labels.map((label, i) => `<li class="${i === step ? 'active' : i < step ? 'done' : ''}"><span class="journey-number">${i < step ? icon('check') : `0${i + 1}`}</span><span class="journey-label">${label}</span></li>`).join('');
  const guides = [
    ['先和小芽打个招呼', '演示账号已经准备好。点击“进入学习”，开始体验学生的完整操作流程。'],
    ['按自己的节奏开始', '从自主练习开始，选择题量。也可以先看看“我的”，体验退出账号后重新登录。'],
    ['一次只关注一道题', '输入后会模拟自动保存。用上一题、下一题或题号面板切换；提交之前不展示对错。'],
    ['中断，也可以接着来', '“已保存”表示已写入本机演示存储。保存退出并重新登录后，从首页继续上次的练习。'],
    ['每份练习，一个结果', '漏答会再次确认；非法格式先定位修正。也可以开启“模拟提交待核对”，检查结果核对路径。'],
    ['看懂错误，再往前一步', '错答和漏答分别标记。打开题目可看当次答案、解析与历史；首版不提供重练和强化入口。'],
  ];
  $('#guide-title').textContent = guides[step][0];
  $('#guide-copy').textContent = guides[step][1];
  document.querySelector('[data-action="fill-example"]').disabled = state.route !== 'answer';
}
function loginScreen() {
  return `<div class="login-screen"><div class="mini-brand">${icon('sprout')} 小芽口算</div><div class="login-hero" aria-hidden="true"><span class="float-chip">8 + 7</span><svg class="plant-illustration" viewBox="0 0 180 180"><ellipse cx="89" cy="157" rx="61" ry="12" fill="#e7eddd"/><circle cx="92" cy="78" r="64" fill="#edf2df"/><path d="M89 126V76" stroke="#729550" stroke-width="5" stroke-linecap="round"/><path d="M88 98C54 101 48 73 49 57c28 0 44 14 39 41" fill="#accb7d"/><path d="M90 85c-2-30 17-47 47-47 1 30-19 48-47 47" fill="#779d55"/><path d="m60 70 27 27m17-29 21-18" stroke="#d3e4b4" stroke-width="2" stroke-linecap="round"/><path d="M60 121h62l-7 34H69Z" fill="#dfbd91"/><path d="M58 118h66v11H58Z" fill="#e9cba6"/><circle cx="146" cy="104" r="4" fill="#d7e3bb"/><circle cx="41" cy="106" r="3" fill="#c6d9a2"/></svg><span class="float-chip second">每天一点 +1</span></div><h1 class="login-title">让进步，<br>一点一点发芽。</h1><p class="login-subtitle">从一组口算开始，发现自己的小小进步。</p><form id="login-form"><div class="field-wrap"><label class="field-label" for="login-name">学生账号</label><div class="input-with-icon"><span class="input-icon">${icon('user')}</span><input class="field" id="login-name" name="username" value="xiaoming" autocomplete="off" spellcheck="false" required></div></div><div class="field-wrap" style="margin-bottom:3px"><label class="field-label" for="login-password">密码</label><div class="input-with-icon"><span class="input-icon">${icon('lock')}</span><input class="field" id="login-password" name="password" type="password" value="" placeholder="演示模式，无需密码" autocomplete="off"></div></div><div id="login-error" class="form-error" role="alert"></div><button class="primary" type="submit">进入学习 ${icon('arrow')}</button></form><p class="login-demo-note">${icon('shield')}<span>仅供演示 · 账号 xiaoming / 无需密码<br>请勿输入真实账号、密码或个人信息。</span></p>${storageWarning()}<div class="login-footer">每一个小小的努力，都值得被看见。</div></div>`;
}
function practiceCards() {
  if (!state.data.practices.length) return '<div class="empty-state"><div class="empty-symbol">' + icon('leaf') + '</div><h3>第一份练习，等你开始</h3>不用着急，按自己的节奏来。</div>';
  return [...state.data.practices].reverse().map(p => {
    const isPending = p.pendingReview;
    const progress = p.draftAnswers.filter(a => String(a).trim()).length;
    return `<button class="card card-click ${p.status === 'IN_PROGRESS' ? 'continue-card' : ''}" data-action="open-practice" data-id="${escapeHTML(p.id)}"><div class="card-heading"><span>${p.status === 'IN_PROGRESS' ? '继续上次练习' : isPending ? '提交结果待核对' : '20 以内加法'}</span><span class="pill ${isPending ? 'orange' : ''}">${isPending ? '待核对' : p.status === 'IN_PROGRESS' ? '进行中' : '已完成'}</span></div><p class="card-subtitle">自主练习 · ${p.total} 题 ${p.result && !isPending ? `· 正确率 ${p.result.percent}%` : `· 已填 ${progress} 题`}<br>${p.savedAt ? `最近保存 ${formatTime(p.savedAt)}` : '示例知识点 · 按自己的节奏练习'}</p></button>`;
  }).join('');
}
function homeScreen() {
  return `<div class="screen-content"><div class="home-top"><div><div class="home-name">小明，今天好呀<span style="font-size:15px;margin-left:7px">☀</span></div><span class="small muted">一年级 · 每一点进步都算数</span></div><div class="avatar" aria-label="小明头像">🌱</div></div>${storageWarning()}<section class="hero-card"><span class="hero-label">给今天一点小挑战</span><h2>练习一点点，<br>收获多一点。</h2><p>选一组适合自己的口算，<br>不用赶时间，认真完成就很好。</p><button class="primary lime" data-action="config">开始一组练习 ${icon('arrow')}</button></section><div class="home-summary"><div><strong>${completed().length}<small>组</small></strong>已完成练习</div><div><strong>${completed().reduce((n, p) => n + p.total, 0)}<small>题</small></strong>累计完成题量</div><div><strong>${groups().length}<small>题</small></strong>待回顾错题</div></div><div class="section-head"><h3>我的练习</h3><span>自主练习记录</span></div>${practiceCards()}<div class="footer-demo-chip">本机模拟记录 · 示例教材分类尚未核对</div></div>${nav('home')}`;
}
function configScreen() {
  return `${header('自主练习')}<div class="screen-content"><div class="page-intro"><h3>选一组，<br>适合自己的练习。</h3><p>这一版先从一个知识点开始。<br>每组题目不重复，完成后统一查看答案。</p></div><label class="config-label">练习范围</label><div class="grade-row"><span class="grade-tag">一年级</span><span class="grade-tag inactive">演示范围</span></div><label class="config-label">知识点</label><div class="point-card"><div class="point-icon">＋</div><div><h4>20 以内加法</h4><p>示例题库 · 可用 120 题</p></div><span class="check-circle">${icon('check')}</span></div><label class="config-label" for="question-count">这次想练习多少题？</label><div class="count-box"><span>支持 1—100 题</span><div class="stepper"><button data-action="count-minus" aria-label="减少题量">−</button><input id="question-count" type="number" min="1" max="100" step="1" value="30" aria-label="练习题量"><button data-action="count-plus" aria-label="增加题量">＋</button></div></div><div class="preset-row">${[10, 20, 30].map(n => `<button class="preset ${n === 30 ? 'selected' : ''}" data-action="count-preset" data-count="${n}">${n} 题</button>`).join('')}</div><div id="count-error" class="form-error" role="alert"></div><div class="config-summary">${icon('info')}<span>普通练习默认 30 题，也可以按自己的节奏调整。<br>当前分类与题目仅用于交互演示。</span></div></div><div class="page-action"><button class="primary" data-action="create">开始练习 ${icon('arrow')}</button><p class="small">不需要关联家长，也可以自主练习。</p></div>`;
}
function saveMessage() {
  if (state.saveStatus === 'failed') return `${icon('alert')} 保存失败，输入仍在当前页面，请重试`;
  if (state.saveStatus === 'saving') return `${icon('save')} 保存中…`;
  if (state.dirty) return `${icon('clock')} 当前修改尚未保存`;
  const p = practice();
  return p?.savedAt ? `${icon('check')} 已保存于 ${formatTime(p.savedAt)}` : `${icon('info')} 尚未填写，随时可以开始`;
}
function answerScreen() {
  const p = practice(), q = p.questions[state.index];
  return `${header('口算练习', 'exit-answer', '<button data-action="question-grid">' + icon('grid') + '题号</button>')}<div class="exercise-progress"><div class="progress-track"><div id="filled-bar" class="progress-fill" style="width:${filledCount() / p.total * 100}%"></div></div><span id="filled-count">${filledCount()} / ${p.total}</span></div><div class="screen-content exercise-body"><div class="exercise-meta"><span class="pill">20 以内加法</span><span>第 ${state.index + 1} / ${p.total} 题</span></div><div class="question-card"><p class="question-kicker">慢慢想，你可以的</p><div class="equation" aria-label="${q.a} 加 ${q.b} 等于多少">${q.a} <span style="color:#9eaf89;font-weight:400">+</span> ${q.b} <span style="color:#9eaf89;font-weight:400">=</span> <span style="color:#b6c4a2">?</span></div><label class="sr-only" for="answer-input" style="position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)">第 ${state.index + 1} 题答案</label><input id="answer-input" class="answer-input ${M.validateAnswer(state.answers[state.index]) ? '' : 'invalid'}" inputmode="numeric" autocomplete="off" placeholder="输入答案" value="${escapeHTML(state.answers[state.index])}" aria-describedby="answer-error"><div class="answer-hint">填写 0 或正整数，也可以暂时跳过</div></div><div id="answer-error" class="inline-error" role="alert">${M.validateAnswer(state.answers[state.index]) ? '' : '请填写 0 或正整数，不要包含小数、字母或符号。'}</div><div id="save-status" class="save-indicator ${state.saveStatus === 'failed' ? 'failed' : ''}" role="status" aria-live="polite">${saveMessage()}</div><p class="exercise-note">${icon('leaf')} 先专心思考，完成后一起回顾。<br><span id="live-time">本次已练习 ${duration(state.seconds)}</span></p></div><div class="exercise-actions"><div class="question-nav"><button class="secondary" data-action="previous" ${state.index === 0 ? 'disabled' : ''}>${icon('back')} 上一题</button><button class="primary" data-action="${state.index === p.total - 1 ? 'finish' : 'next'}">${state.index === p.total - 1 ? '检查并结束' : '下一题'} ${icon('arrow')}</button></div><div class="exercise-bottom-actions"><button class="plain-link" data-action="save-exit">${icon('save')} 保存并退出</button><button class="plain-link finish-link" data-action="finish">结束练习 ${icon('check')}</button></div></div>`;
}
function resultRow(row, practiceId) {
  return `<button class="answer-row" data-action="result-detail" data-index="${row.index}" data-id="${escapeHTML(practiceId)}"><span class="answer-mark ${row.correct ? '' : 'wrong'}">${icon(row.correct ? 'check' : row.blank ? 'info' : 'close')}</span><span class="row-main"><span class="row-equation">${row.text} = ${row.answer}</span><span class="row-answer" style="display:block">第 ${row.index + 1} 题 · ${row.correct ? '答对了' : row.blank ? '未作答' : '答错了'}<strong>${row.correct ? '' : `你的答案：${escapeHTML(row.input || '未填写')}`}</strong></span></span>${icon('arrow')}</button>`;
}
function resultScreen() {
  const p = practice(), r = p.result;
  const rows = state.filter === 'wrong' ? r.rows.filter(q => !q.correct) : r.rows;
  return `${header('练习结果')}<div class="screen-content"><div class="result-intro"><span class="pill">${icon('check')} 已完成 · 自主练习</span><h3>又完成了一次小小的挑战</h3><p>把错题看懂，就是新的进步。</p></div><div class="score-wrap"><div class="score-circle" style="--score:${r.percent}"><div class="score-inner"><strong>${r.percent}<small>%</small></strong><span>本次正确率</span></div></div></div><div class="result-metrics"><div><strong>${p.total}</strong>总题量</div><div><strong>${r.correct}</strong>答对</div><div class="wrong"><strong>${r.wrong}</strong>答错</div><div class="blank"><strong>${r.blank}</strong>漏答</div></div><div class="result-time">${icon('clock')} 系统记录用时 ${duration(r.seconds)} · 本地模拟</div><div class="result-actions"><button class="primary" data-action="mistakes">查看错题（${r.wrong + r.blank}） ${icon('arrow')}</button><button class="secondary" data-action="home">回到首页</button></div><div class="filter-row"><button data-action="filter-all" class="${state.filter === 'all' ? 'active' : ''}">全部题目</button><button data-action="filter-wrong" class="${state.filter === 'wrong' ? 'active' : ''}">只看错题</button><span>点击查看解析</span></div>${rows.length ? rows.map(row => resultRow(row, p.id)).join('') : '<div class="empty-state">这份练习没有错题，继续保持认真。</div>'}<div class="footer-demo-chip">正式结果不可修改 · 重复查看不新增记录</div></div>`;
}
function mistakesScreen() {
  const items = groups();
  const count = items.reduce((n, g) => n + g.records.length, 0);
  return `<div class="screen-content"><div class="home-top" style="padding-bottom:10px"><span class="eyebrow">每一次回顾，都有收获</span><span class="pill">只读回顾</span></div><h2 class="mistake-title">把不会的，<br>一点点变成会的。</h2><p class="mistake-sub">错答和漏答都会留在这里，<br>先看懂，再向前一步。</p><div class="mistake-summary"><div><strong>${items.length}</strong><span>不同错题</span></div><div><strong>${count}</strong><span>错误作答次数</span></div></div><div class="section-head"><h3>20 以内加法</h3><span>${items.length} 道待回顾</span></div>${items.length ? items.map(g => `<button class="card card-click mistake-card" data-action="mistake-detail" data-id="${escapeHTML(g.id)}"><div class="card-heading"><span>一年级 · 示例知识点</span><span class="pill ${g.blank ? 'orange' : 'red'}">${g.blank ? '漏答' : '错答'}</span></div><div class="row-equation">${g.text} = ?</div><div class="card-row"><small>错答或漏答 ${g.records.length} 次 · 查看解析与历次记录</small>${icon('arrow')}</div></button>`).join('') : '<div class="empty-state"><div class="empty-symbol">' + icon('book') + '</div><h3>暂时还没有错题</h3>完成练习后，可以在这里回顾。</div>'}<div class="footer-demo-chip">答对不会删除旧记录 · 本版暂不提供重练</div></div>${nav('mistakes')}`;
}
function detailScreen() {
  let row, records;
  if (state.detailBack === 'result') {
    row = practice().result.rows[state.detailIndex];
    records = state.data.practices.flatMap(p => (p.result?.rows || []).filter(q => q.id === row.id).map(q => ({ ...q, submittedAt: p.result.submittedAt })));
  } else {
    const group = groups().find(g => g.id === state.detailId);
    if (!group) return `${header('题目详情', 'mistakes')}<div class="empty-state">暂无这道题的记录。</div>`;
    row = group;
    records = state.data.practices.flatMap(p => (p.result?.rows || []).filter(q => q.id === group.id).map(q => ({ ...q, submittedAt: p.result.submittedAt })));
  }
  return `${header('题目回顾', state.detailBack)}<div class="screen-content"><div class="exercise-meta"><span class="pill">20 以内加法</span><span class="pill ${row.correct ? '' : row.blank ? 'orange' : 'red'}">${row.correct ? '答对' : row.blank ? '漏答' : '错答'}</span></div><div class="card"><p class="question-kicker" style="text-align:center;margin:4px 0 0">回到当时的这道题</p><div class="detail-equation">${row.text} = ?</div><div class="answer-comparison"><div>当次答案<strong>${escapeHTML(row.input || '未答')}</strong></div><div>标准答案<strong>${row.answer}</strong></div></div></div><div class="explanation-box"><div class="explanation-title">${icon('bulb')} 一起想一想</div><p>${row.explanation}</p></div><div class="section-head"><h3>历次作答</h3><span>${records.length} 条记录</span></div>${[...records].reverse().map((r, i) => `<div class="history-entry"><span>${formatDate(r.submittedAt)} ${formatTime(r.submittedAt)}${i === 0 ? ' · 最近一次' : ''}<br><small>${r.correct ? '答对' : r.blank ? '漏答' : '错答'} · 答案 ${escapeHTML(r.input || '未填写')}</small></span><span class="pill ${r.correct ? '' : 'orange'}">${r.correct ? '答对' : r.blank ? '漏答' : '错答'}</span></div>`).join('')}<p class="detail-note">这里保留当时的题目与结果。一次答对不代表已经掌握，也不会删除旧的错误记录。</p><button class="secondary" data-action="${state.detailBack}">${icon('back')} 返回${state.detailBack === 'result' ? '练习结果' : '错题列表'}</button></div>`;
}
function profileScreen() {
  return `${header('我的', 'home')}<div class="screen-content"><div class="profile-hero"><div class="avatar">🌱</div><div><h3>小明</h3><span class="pill">学生 · 一年级</span></div></div><div class="profile-item"><span>${icon('user')} 演示账号</span><small>xiaoming</small></div><div class="profile-item"><span>${icon('book')} 已完成练习</span><small>${completed().length} 组</small></div><div class="profile-item"><span>${icon('info')} 当前版本</span><small>交互原型 v1</small></div><p class="profile-note">这个原型仅使用本机模拟数据。<br>退出登录不会删除已保存的演示练习；<br>重新登录后，可从首页继续。<br><br>真实登录、家庭关联、数据同步与 Android 系统行为，需要后续正式实现与验证。</p>${storageWarning()}<button class="secondary" data-action="logout">${icon('logout')} 退出演示账号</button></div>${nav('profile')}`;
}
function checkingScreen() {
  const sending = state.route === 'submitting';
  return `${header(sending ? '正在提交' : '结果待核对', sending ? 'noop' : 'home')}<div class="checking-screen"><div class="checking-orbit">${icon(sending ? 'save' : 'shield')}</div><h3>${sending ? '正在整理这次的努力…' : '先核对，再放心离开。'}</h3><p>${sending ? '正在模拟统一提交，请稍等。<br>不会重复产生新的结果。' : '模拟提交反馈暂时没有到达。<br>这时先核对原结果，不要重新提交。<br>核对期间暂停作答和计时。'}</p><button class="primary" data-action="query-result" ${sending || state.busy ? 'disabled' : ''}>${state.busy ? '正在核对…' : '核对提交结果'} ${icon('arrow')}</button>${sending ? '' : '<button class="secondary" data-action="home">先回练习首页</button>'}<div class="footer-demo-chip">演示场景 · 无真实网络请求</div></div>`;
}
function render() {
  const screens = { login: loginScreen, home: homeScreen, config: configScreen, answer: answerScreen, result: resultScreen, mistakes: mistakesScreen, detail: detailScreen, profile: profileScreen, checking: checkingScreen, submitting: checkingScreen };
  $('#app').innerHTML = (screens[state.route] || homeScreen)();
  updateGuide();
}
function updateSaveUI() {
  const el = $('#save-status');
  if (el) { el.innerHTML = saveMessage(); el.classList.toggle('failed', state.saveStatus === 'failed'); }
}
function saveAnswers() {
  clearTimeout(saveTimer);
  const p = practice();
  if (!p || p.status !== 'IN_PROGRESS') return false;
  if (state.failSave) { state.saveStatus = 'failed'; updateSaveUI(); return false; }
  const before = { draftAnswers: p.draftAnswers, savedIndex: p.savedIndex, savedAt: p.savedAt, elapsedSeconds: p.elapsedSeconds };
  M.saveDraft(p, state.answers, state.index, state.seconds);
  if (!persist()) {
    Object.assign(p, before);
    state.saveStatus = 'failed';
    updateSaveUI();
    toast('本机存储不可用，未能保存。当前页面输入仍保留。');
    return false;
  }
  state.dirty = false;
  state.saveStatus = 'saved';
  updateSaveUI();
  updateGuide();
  return true;
}
function openPractice(id) {
  state.practiceId = id;
  const p = practice();
  if (!p) return toast('这份演示练习已不存在。');
  if (p.pendingReview) return route('checking');
  if (p.result) { state.filter = 'all'; return route('result'); }
  state.answers = [...p.draftAnswers];
  state.index = Math.min(p.savedIndex || 0, p.total - 1);
  state.seconds = p.elapsedSeconds || 0;
  state.dirty = false;
  state.saveStatus = p.savedAt ? 'saved' : '';
  route('answer');
}
function moveQuestion(index) {
  state.index = Math.max(0, Math.min(index, practice().total - 1));
  saveAnswers();
  render();
}
function exitDialog() {
  showModal(`<div class="modal-icon">${icon('save')}</div><h3 id="dialog-title">这次先练到这里？</h3><p>已经填写 ${filledCount()} / ${practice().total} 题。<br>保存后，下次可以从这里继续。尚未成功保存的输入，退出后不能保证恢复。</p><button class="primary" data-action="save-exit">保存并退出 ${icon('arrow')}</button><button class="secondary" data-action="close-modal">继续练习</button>${state.dirty || state.saveStatus === 'failed' ? '<button class="plain-link modal-danger" data-action="discard-exit">放弃未保存修改并退出</button>' : ''}`);
}
function finishDialog() {
  clearTimeout(saveTimer);
  const invalid = state.answers.findIndex(a => !M.validateAnswer(a));
  if (invalid !== -1) {
    state.index = invalid;
    render();
    toast(`第 ${invalid + 1} 题格式不正确，请先填写 0 或正整数。`);
    $('#answer-input').focus();
    return;
  }
  const total = practice().total, filled = filledCount(), blank = total - filled;
  showModal(`<div class="modal-icon">${icon(blank ? 'alert' : 'check')}</div><h3 id="dialog-title">${blank ? `还有 ${blank} 题没有填写` : '准备结束这次练习？'}</h3><p>${blank ? '可以回去补答，也可以确认提交。<br>漏答会计入总题量，并按错误记录。' : '提交后会统一批改，并生成正式结果。<br>请确认已检查答案，完成后不能修改。'}</p><div class="submit-summary"><div><strong>${total}</strong>总题量</div><div><strong>${filled}</strong>已填写</div><div><strong>${blank}</strong>未填写</div></div><button class="primary" data-action="confirm-submit">${blank ? '确认漏答，仍然提交' : '确认提交'} ${icon('arrow')}</button><button class="secondary" data-action="${blank ? 'return-blank' : 'close-modal'}">${blank ? '返回补答' : '再检查一下'}</button>`);
}
function submit() {
  if (state.busy || state.route !== 'answer') return;
  clearTimeout(saveTimer);
  const p = practice();
  const snapshot = [...state.answers];
  state.busy = true;
  route('submitting');
  setTimeout(() => {
    M.submitPractice(p, snapshot, state.seconds);
    p.pendingReview = state.pendingSubmit;
    persist();
    state.busy = false;
    state.dirty = false;
    state.filter = 'all';
    route(p.pendingReview ? 'checking' : 'result');
  }, 650);
}
function questionGrid() {
  const p = practice();
  showModal(`<div class="modal-header"><h3 id="dialog-title">这次练习的题目</h3><button class="icon-button" data-action="close-modal" aria-label="关闭题号面板">${icon('close')}</button></div><div class="legend-row"><span>浅绿：已填写</span><span>描边：当前题</span><span>提交前不显示对错</span></div><div class="question-grid">${p.questions.map((q, i) => `<button data-action="jump-question" data-index="${i}" class="${String(state.answers[i]).trim() ? 'filled' : ''} ${i === state.index ? 'current' : ''}" aria-label="第 ${i + 1} 题，${String(state.answers[i]).trim() ? '已填' : '未填'}">${i + 1}</button>`).join('')}</div>`);
}
function countChange(value) {
  $('#question-count').value = value;
  document.querySelectorAll('.preset').forEach(el => el.classList.toggle('selected', Number(el.dataset.count) === Number(value)));
  $('#count-error').textContent = '';
}
document.addEventListener('submit', event => {
  if (event.target.id !== 'login-form') return;
  event.preventDefault();
  if ($('#login-name').value.trim() !== 'xiaoming') {
    $('#login-error').textContent = '请使用演示账号 xiaoming，无需密码。';
    return;
  }
  try { sessionStorage.setItem(SESSION_KEY, 'demo-student'); } catch { state.storageError = true; }
  route('home');
});
document.addEventListener('input', event => {
  if (event.target.id === 'answer-input') {
    state.answers[state.index] = event.target.value;
    state.dirty = true;
    state.saveStatus = 'saving';
    const valid = M.validateAnswer(event.target.value);
    event.target.classList.toggle('invalid', !valid);
    $('#answer-error').textContent = valid ? '' : '请填写 0 或正整数，不要包含小数、字母或符号。';
    $('#filled-count').textContent = `${filledCount()} / ${practice().total}`;
    $('#filled-bar').style.width = `${filledCount() / practice().total * 100}%`;
    updateSaveUI();
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => { if (state.route === 'answer') saveAnswers(); }, 900);
  }
  if (event.target.id === 'question-count') countChange(event.target.value);
});
document.addEventListener('change', event => {
  if (event.target.id === 'mock-save-fail') {
    state.failSave = event.target.checked;
    if (!state.failSave && state.route === 'answer' && state.dirty) saveAnswers();
  }
  if (event.target.id === 'mock-submit-pending') state.pendingSubmit = event.target.checked;
});
document.addEventListener('click', event => {
  const button = event.target.closest('[data-action]');
  if (!button || button.disabled || state.busy) return;
  event.preventDefault();
  const action = button.dataset.action;
  if (['home', 'config', 'mistakes', 'profile'].includes(action)) return route(action);
  if (action === 'close-modal') return closeModal();
  if (action === 'count-minus') return countChange(Math.max(1, Number($('#question-count').value || 1) - 1));
  if (action === 'count-plus') return countChange(Math.min(100, Number($('#question-count').value || 0) + 1));
  if (action === 'count-preset') return countChange(Number(button.dataset.count));
  if (action === 'create') {
    try {
      const id = `demo-${Date.now()}-${state.data.practices.length + 1}`;
      const p = M.createPractice(Number($('#question-count').value), id);
      state.data.practices.push(p);
      persist();
      return openPractice(id);
    } catch (error) { $('#count-error').textContent = error.message; return; }
  }
  if (action === 'open-practice') return openPractice(button.dataset.id);
  if (action === 'previous') return moveQuestion(state.index - 1);
  if (action === 'next') return moveQuestion(state.index + 1);
  if (action === 'question-grid') return questionGrid();
  if (action === 'jump-question') { closeModal(); return moveQuestion(Number(button.dataset.index)); }
  if (action === 'exit-answer') return exitDialog();
  if (action === 'save-exit') {
    if (!saveAnswers()) { toast('保存失败，先留在当前页面。可关闭模拟失败后重试。'); if (!state.modal) exitDialog(); return; }
    route('home'); toast('已保存，下次可以从首页继续。'); return;
  }
  if (action === 'discard-exit') { state.dirty = false; return route('home'); }
  if (action === 'finish') return finishDialog();
  if (action === 'confirm-submit') return submit();
  if (action === 'return-blank') {
    closeModal();
    state.index = Math.max(0, state.answers.findIndex(a => !String(a).trim()));
    return render();
  }
  if (action === 'fill-example') {
    if (state.route !== 'answer') return;
    state.answers = M.exampleAnswers(practice());
    state.dirty = true;
    saveAnswers();
    render();
    const p = practice(), correct = Math.floor(p.total * .8), wrong = Math.floor(p.total * 2 / 15);
    return toast(`已填入演示：${correct} 对、${wrong} 错、${p.total - correct - wrong} 漏。`);
  }
  if (action === 'query-result') {
    const p = practice();
    if (!p?.result) return toast('模拟记录未就绪，请稍后再次核对。');
    state.busy = true; render();
    setTimeout(() => { p.pendingReview = false; persist(); state.busy = false; state.filter = 'all'; route('result'); toast('已核对到原结果，没有新增成绩。'); }, 500);
    return;
  }
  if (action === 'filter-all' || action === 'filter-wrong') { state.filter = action === 'filter-all' ? 'all' : 'wrong'; return render(); }
  if (action === 'result-detail') { state.practiceId = button.dataset.id; state.detailIndex = Number(button.dataset.index); state.detailBack = 'result'; return route('detail'); }
  if (action === 'mistake-detail') { state.detailId = button.dataset.id; state.detailBack = 'mistakes'; return route('detail'); }
  if (action === 'result') return route('result');
  if (action === 'logout') {
    showModal(`<div class="modal-icon">${icon('logout')}</div><h3 id="dialog-title">退出演示账号？</h3><p>已经成功保存的演示练习会保留在本机。<br>再次登录后，可以继续未完成的练习。</p><button class="primary" data-action="confirm-logout">退出登录</button><button class="secondary" data-action="close-modal">暂不退出</button>`);
    return;
  }
  if (action === 'confirm-logout') {
    try { sessionStorage.removeItem(SESSION_KEY); } catch { /* Prototype falls back to the current page session. */ }
    state.practiceId = null; state.answers = []; state.dirty = false;
    return route('login');
  }
  if (action === 'reset') {
    showModal(`<div class="modal-icon">${icon('reset')}</div><h3 id="dialog-title">重新开始演示？</h3><p>这会删除本原型在当前浏览器保存的演示练习与结果。不会影响其他网站或正式项目文件。</p><button class="primary" data-action="confirm-reset">确认重置演示</button><button class="secondary" data-action="close-modal">保留当前进度</button>`);
    return;
  }
  if (action === 'confirm-reset') {
    clearTimeout(saveTimer);
    try { localStorage.removeItem(KEY); sessionStorage.removeItem(SESSION_KEY); } catch { state.storageError = true; }
    state.data = { practices: [] }; state.answers = []; state.practiceId = null; state.dirty = false; state.failSave = false; state.pendingSubmit = false;
    $('#mock-save-fail').checked = false; $('#mock-submit-pending').checked = false;
    return route('login');
  }
});
setInterval(() => {
  if (state.route === 'answer' && !document.hidden && !state.modal) {
    state.seconds++;
    const el = $('#live-time');
    if (el) el.textContent = `本次已练习 ${duration(state.seconds)}`;
  }
}, 1000);
document.addEventListener('visibilitychange', () => { if (document.hidden && state.route === 'answer') saveAnswers(); });
window.addEventListener('beforeunload', event => {
  if (state.route === 'answer' && state.dirty) { event.preventDefault(); event.returnValue = ''; }
});
decorate();
render();
