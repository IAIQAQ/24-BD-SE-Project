import collections
import re
import unittest

from grade_catalog import build_catalog


class GradeCatalogTest(unittest.TestCase):
    def test_all_nine_topics_have_thirty_distinct_correct_questions(self):
        rows = build_catalog()
        self.assertEqual(270, len(rows))
        self.assertEqual({101, 201, 202, 203, 204, 301, 302, 303, 304}, {r['pointId'] for r in rows})
        self.assertEqual({30}, set(collections.Counter(r['pointId'] for r in rows).values()))
        self.assertEqual(270, len({r['stem'] for r in rows}))
        self.assertEqual(270, len({r['fingerprint'] for r in rows}))
        for row in rows:
            a, op, b = re.fullmatch(r'(\d+) ([＋－×÷]) (\d+) =', row['stem']).groups()
            a, b = int(a), int(b)
            if op == '＋':
                expected = a + b
            elif op == '－':
                expected = a - b
            elif op == '×':
                expected = a * b
            else:
                self.assertGreater(b, 0)
                self.assertEqual(0, a % b)
                expected = a // b
            self.assertEqual(str(expected), row['answer'])
            self.assertGreaterEqual(expected, 0)
            self.assertIn(str(expected), row['explanation'])
            point = row['pointId']
            if point == 101:
                self.assertEqual('－', op)
                self.assertLessEqual(a, 20)
            elif point in (201, 202):
                self.assertLessEqual(max(a, b, expected), 100)
            elif point == 203:
                self.assertEqual('×', op)
                self.assertTrue(1 <= a <= b <= 9)
            elif point == 204:
                self.assertEqual('÷', op)
                self.assertTrue(1 <= expected <= 9 and 2 <= b <= 9)
            elif point in (301, 302):
                self.assertEqual(0, a % 10)
                self.assertEqual(0, b % 10)
                self.assertLessEqual(max(a, b, expected), 1000)
                self.assertGreaterEqual(max(a, b), 100)
            elif point == 303:
                self.assertEqual('×', op)
                self.assertTrue(10 <= a <= 99 and 2 <= b <= 9)
            elif point == 304:
                self.assertEqual('÷', op)
                self.assertTrue(10 <= a <= 999 and 2 <= b <= 9)

    def test_generation_is_repeatable(self):
        self.assertEqual(build_catalog(), build_catalog())


if __name__ == '__main__':
    unittest.main()
