import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname,resolve} from 'node:path';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const rows=JSON.parse(readFileSync(resolve(root,'data/catalog-120.json'),'utf8'));
assert.equal(rows.length,120);const seen=new Set();
for(const r of rows){
  assert.ok(Number.isInteger(r.a)&&Number.isInteger(r.b)&&r.a>=0&&r.a<=r.b&&r.b<=20);
  // 独立用计数序列长度核对，不调用生成器或判分函数。
  const counted=[...Array.from({length:r.a}),...Array.from({length:r.b})].length;
  assert.ok(counted<=20);assert.equal(r.standardAnswer,String(counted));
  assert.equal(r.stem,`${r.a} + ${r.b} =`);assert.equal(r.explanation,`从 ${r.a} 开始，再增加 ${r.b}，得到 ${counted}。`);
  const canonical=`${r.a},${r.b}`;assert.ok(!seen.has(canonical));seen.add(canonical);
}
const fixture=JSON.parse(readFileSync(resolve(root,'backend/src/test/resources/acceptance-30.json'),'utf8'));
assert.deepEqual(['CORRECT','WRONG','UNANSWERED'].map(s=>fixture.filter(r=>r.outcome===s).length),[24,4,2]);
console.log('CATALOG_REVIEW PASS: 120 unique, arithmetic/explanations/range checked; fixture 24/4/2.');
