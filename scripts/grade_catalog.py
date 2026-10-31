"""Generate reviewed arithmetic samples; this command writes files, never a database."""
import hashlib
import json
from pathlib import Path
import random


POINTS = [
    (101, 1, '20以内减法'),
    (201, 2, '100以内加法'), (202, 2, '100以内减法'),
    (203, 2, '表内乘法'), (204, 2, '表内除法'),
    (301, 3, '整十、整百数加法（1000以内）'),
    (302, 3, '整十、整百数减法（1000以内）'),
    (303, 3, '两位数乘一位数'), (304, 3, '两三位数除以一位数（整除）'),
]


def build_catalog():
    pools = {
        101: ('－', [(a, b) for a in range(1, 21) for b in range(a + 1)]),
        201: ('＋', [(a, b) for a in range(1, 51) for b in range(max(a, 20), 101 - a)]),
        202: ('－', [(a, b) for a in range(21, 101) for b in range(1, a + 1)]),
        203: ('×', [(a, b) for a in range(1, 10) for b in range(a, 10)]),
        204: ('÷', [(q * b, b) for b in range(2, 10) for q in range(1, 10)]),
        301: ('＋', [(a, b) for a in range(10, 501, 10) for b in range(max(a, 100), 1001 - a, 10)]),
        302: ('－', [(a, b) for a in range(100, 1000, 10) for b in range(10, a + 1, 10)]),
        303: ('×', [(a, b) for a in range(10, 50) for b in range(2, 6)]),
        304: ('÷', [(q * b, b) for b in range(2, 6) for q in range(10, 61)]),
    }
    rows = []
    for point, grade, _ in POINTS:
        op, pool = pools[point]
        for a, b in random.Random(point).sample(pool, 30):
            if op == '＋':
                answer = a + b
                explanation = f'从{a}开始，再加上{b}，得到{answer}。'
            elif op == '－':
                answer = a - b
                explanation = f'从{a}里面去掉{b}，还剩{answer}。'
            elif op == '×':
                answer = a * b
                explanation = (f'{a}个{b}相加，得到{answer}。' if a < 10 else
                               f'先算{a // 10 * 10}×{b}，再算{a % 10}×{b}，合起来是{answer}。')
            else:
                answer = a // b
                explanation = f'因为{b}×{answer}＝{a}，所以{a}÷{b}＝{answer}。'
            rows.append({'id': 10001 + len(rows), 'pointId': point, 'grade': grade,
                         'stem': f'{a} {op} {b} =', 'answer': str(answer),
                         'explanation': explanation,
                         'fingerprint': hashlib.sha256(f'{op}:{a}:{b}'.encode()).hexdigest()})
    return rows


def write_files():
    root = Path(__file__).resolve().parent.parent
    rows = build_catalog()
    quote = lambda s: "'" + s.replace("'", "''") + "'"
    lines = ['-- New samples only; explicitly targets mathdrill_demo. Existing IDs must not be reused.', 'START TRANSACTION;']
    evidence = 'https://www.moe.gov.cn/jyb_xxgk/s5743/s5744/A06/202506/W020250625697282575780.pdf'
    for point, grade, name in POINTS:
        lines.append(f"INSERT INTO mathdrill_demo.knowledge_point(id,name,grade,semester,textbook,evidence) VALUES({point},{quote(name)},{grade},'基础口算练习','自编基础口算样例（非完整教材）',{quote(evidence)});")
    for row in rows:
        values = ','.join([str(row['id']), str(row['pointId']), quote(row['stem']), quote(row['answer']), quote(row['explanation']), quote(row['fingerprint'])])
        lines.append('INSERT INTO mathdrill_demo.question(id,point_id,stem,standard_answer,explanation,fingerprint,reviewed,enabled) VALUES(' + values + ',TRUE,TRUE);')
    lines.extend(['INSERT INTO mathdrill_demo.question_revision(question_id,revision,point_id,stem,standard_answer,explanation,fingerprint) SELECT id,revision,point_id,stem,standard_answer,explanation,fingerprint FROM mathdrill_demo.question WHERE id BETWEEN 10001 AND 10270;', 'COMMIT;'])
    (root / 'data/grade-catalog-270.sql').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (root / 'data/grade-catalog-270.json').write_text(json.dumps({'points': POINTS, 'questions': rows}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Generated 270 questions in 9 topics. No database connection or write performed.')


if __name__ == '__main__':
    write_files()
