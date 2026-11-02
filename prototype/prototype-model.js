(function (root) {
  'use strict';
  const pairs = [[8, 7], [9, 6], [6, 8], [7, 5], [5, 9]];
  const pool = [];
  for (let a = 0; a <= 20; a++) for (let b = 0; b <= 20 - a; b++) pool.push([a, b]);
  pool.sort((x, y) => ((x[0] * 37 + x[1] * 17) % 101) - ((y[0] * 37 + y[1] * 17) % 101));
  for (const pair of pool) if (!pairs.some(p => p[0] === pair[0] && p[1] === pair[1])) pairs.push(pair);
  const BANK = pairs.slice(0, 120).map(([a, b]) => ({ id: `${a}+${b}`, a, b, text: `${a} + ${b}`, answer: a + b, explanation: `把 ${a} 和 ${b} 合起来，一共有 ${a + b}。所以 ${a} + ${b} = ${a + b}。` }));
  const validateAnswer = value => String(value).trim() === '' || /^\d+$/.test(String(value).trim());
  function createPractice(total, id) {
    if (!Number.isInteger(total) || total < 1 || total > 100) throw new Error('题量需要是 1—100 之间的整数。');
    return { id, total, questions: BANK.slice(0, total).map(q => ({ ...q })), draftAnswers: Array(total).fill(''), savedIndex: 0, savedAt: null, elapsedSeconds: 0, status: 'IN_PROGRESS', result: null };
  }
  function saveDraft(practice, answers, index, seconds) {
    if (practice.status !== 'IN_PROGRESS') return;
    if (answers.length !== practice.total) throw new Error('答案数量不一致。');
    practice.draftAnswers = answers.map(String);
    practice.savedIndex = index;
    practice.savedAt = new Date().toISOString();
    practice.elapsedSeconds = seconds;
  }
  function exampleAnswers(practice) {
    const correct = Math.floor(practice.total * 0.8);
    const wrong = Math.floor(practice.total * 2 / 15);
    return practice.questions.map((q, i) => i < correct ? String(q.answer) : i < correct + wrong ? String(q.answer + 1) : '');
  }
  function submitPractice(practice, answers, seconds) {
    if (practice.result) return practice.result;
    if (answers.length !== practice.total || answers.some(a => !validateAnswer(a))) throw new Error('请先修正答案格式。');
    const rows = practice.questions.map((q, i) => {
      const input = String(answers[i]).trim();
      return { ...q, index: i, input, blank: input === '', correct: input !== '' && BigInt(input) === BigInt(q.answer) };
    });
    const correct = rows.filter(q => q.correct).length;
    const blank = rows.filter(q => q.blank).length;
    practice.status = 'COMPLETED';
    practice.draftAnswers = answers.map(String);
    practice.elapsedSeconds = seconds;
    practice.result = { rows, correct, blank, wrong: practice.total - correct - blank, percent: (correct / practice.total * 100).toFixed(2), seconds, submittedAt: new Date().toISOString() };
    return practice.result;
  }
  function mistakeGroups(practices) {
    const groups = new Map();
    for (const p of practices) for (const row of p.result?.rows || []) {
      if (row.correct) continue;
      if (!groups.has(row.id)) groups.set(row.id, { ...row, records: [], practiceId: p.id });
      const group = groups.get(row.id);
      if (!group.records.some(r => r.practiceId === p.id && r.index === row.index)) group.records.push({ ...row, practiceId: p.id, submittedAt: p.result.submittedAt });
      group.practiceId = p.id;
      group.input = row.input;
      group.blank = row.blank;
    }
    return [...groups.values()];
  }
  const api = { BANK, validateAnswer, createPractice, saveDraft, exampleAnswers, submitPractice, mistakeGroups };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.PrototypeModel = api;
})(typeof globalThis !== 'undefined' ? globalThis : window);
