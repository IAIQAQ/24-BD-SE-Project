import {expect,test} from 'vitest';
import {useNormalComposition} from '../src/composables/useNormalComposition';
const points=[{id:'2',name:'A',available:120,grade:1,semester:'上'},{id:'10',name:'B',available:120,grade:1,semester:'上'}];
test('starts with 30 and redistributes selected points with exact remainder',()=>{
 const s=useNormalComposition();s.reset(points);expect(s.total.value).toBe(30);expect(s.allocations.value).toEqual({'2':30});s.setTotal(5);s.toggle('10');expect(s.allocations.value).toEqual({'2':3,'10':2});s.setAllocation('2',2);s.setAllocation('10',3);expect(s.request('child',points).allocations).toEqual([{pointId:'2',total:2},{pointId:'10',total:3}]);
});
test('rejects fractional, mismatching, insufficient and too many selections without growing the total',()=>{
 const s=useNormalComposition();s.reset(points);s.setTotal(1);s.toggle('10');expect(s.total.value).toBe(1);expect(()=>s.request('child',points)).toThrow();s.setTotal(5);s.setAllocation('2',1.5);expect(()=>s.request('child',points)).toThrow();s.setAllocation('2',1);expect(()=>s.request('child',points)).toThrow();s.setAllocation('2',3);expect(()=>s.request('child',[{...points[0],available:2},points[1]])).toThrow(/需要.*3.*可用.*2/);
});
test('same configuration reuses uncertain key but config and owner changes invalidate it',()=>{
 const s=useNormalComposition();s.reset(points);const a=s.request('one',points);expect(s.request('one',points)).toEqual(a);s.setTotal(5);expect(s.request('one',points).operationKey).not.toBe(a.operationKey);const b=s.request('one',points);expect(s.request('two',points).operationKey).not.toBe(b.operationKey);s.reset(points);expect(s.request('one',points).operationKey).not.toBe(a.operationKey);
});
test('re-entering unchanged values preserves an uncertain creation key',()=>{
 const s=useNormalComposition();s.reset(points);const first=s.request('student',points);s.setAllocation('2',30);expect(s.request('student',points)).toEqual(first);s.setTotal(30);expect(s.request('student',points)).toEqual(first);
});