import { createHash } from 'node:crypto';
import { mkdirSync,writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve,dirname } from 'node:path';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const rows=[];
for(let a=0;a<=10;a++)for(let b=a;b<=20-a;b++){
  if(a===0&&b===20)continue;
  rows.push({id:rows.length+1,a,b,stem:`${a} + ${b} =`,standardAnswer:String(a+b),explanation:`从 ${a} 开始，再增加 ${b}，得到 ${a+b}。`,fingerprint:createHash('sha256').update(`add:${a}:${b}`).digest('hex')});
}
mkdirSync(resolve(root,'data'),{recursive:true});
writeFileSync(resolve(root,'data/catalog-120.json'),JSON.stringify(rows,null,2)+'\n');
const quote=s=>"'"+s.replaceAll("'","''")+"'";
const sql="INSERT INTO knowledge_point(id,name,grade,semester) VALUES(1,'20以内加法（综合）',1,'上学期·综合复习');\nINSERT INTO question(id,point_id,stem,standard_answer,explanation,fingerprint,reviewed,enabled) VALUES\n"+rows.map(r=>`(${r.id},1,${quote(r.stem)},${quote(r.standardAnswer)},${quote(r.explanation)},${quote(r.fingerprint)},TRUE,TRUE)`).join(',\n')+';\n';
writeFileSync(resolve(root,'backend/src/main/resources/db/migration/V3__reviewed_catalog.sql'),sql);
mkdirSync(resolve(root,'backend/src/test/resources'),{recursive:true});
writeFileSync(resolve(root,'backend/src/test/resources/acceptance-30.json'),JSON.stringify(rows.slice(0,30).map((r,i)=>({questionId:String(r.id),expected:r.standardAnswer,raw:i<24?r.standardAnswer:i<28?'999':'',outcome:i<24?'CORRECT':i<28?'WRONG':'UNANSWERED'})),null,2)+'\n');
console.log('Prepared 120 original questions and fixed 24/4/2 fixture.');
