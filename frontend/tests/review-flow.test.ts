import {beforeEach,expect,test,vi} from 'vitest';
import {useReviewSelection} from '../src/composables/useReviewSelection';
import {practiceApi} from '../src/api/client';
import {confirmAction} from '../src/composables/confirmation';
vi.mock('../src/api/client',()=>({practiceApi:{reviewOptions:vi.fn(),replay:vi.fn(),reinforce:vi.fn(),parentReviewOptions:vi.fn(),parentReinforcePreview:vi.fn()}}));
vi.mock('../src/composables/confirmation',()=>({confirmAction:vi.fn()}));
const options={replay:{allowed:true,reason:'',total:1,items:[{id:'12',stem:'2 + 3 =',grade:1,pointId:'1',pointName:'加法'}]},grades:[1],groups:[{grade:1,seedItemIds:['12'],points:[{pointId:'1',pointName:'加法',available:120,reason:''}]}]};
beforeEach(()=>{vi.clearAllMocks();vi.mocked(practiceApi.reviewOptions).mockResolvedValue(options);vi.mocked(confirmAction).mockResolvedValue(true);});
test('cancel leaves selection and makes no practice',async()=>{
 const state=useReviewSelection(()=> '1');await state.prepare(['12']);vi.mocked(confirmAction).mockResolvedValue(false);
 expect(await state.replay()).toBeNull();expect(state.selectedIds.value).toEqual(['12']);expect(state.options.value).toEqual(options);expect(practiceApi.replay).not.toHaveBeenCalled();
});
test('uncertain create retries the same key; changing selection uses another key',async()=>{
 const state=useReviewSelection(()=> '1');await state.prepare(['12']);vi.mocked(practiceApi.replay).mockRejectedValueOnce(new Error('network')).mockResolvedValue({id:'new'} as any);
 expect(await state.replay()).toBeNull();expect((await state.replay())?.id).toBe('new');const calls=vi.mocked(practiceApi.replay).mock.calls;expect(calls[0]).toEqual(calls[1]);
 state.toggle('13');await state.prepare();await state.replay();expect(vi.mocked(practiceApi.replay).mock.calls[2][1]).not.toBe(calls[0][1]);
});
test('response for an old account cannot open a practice after switching user',async()=>{
 let student='1';const state=useReviewSelection(()=>student);await state.prepare(['12']);let finish!:(v:any)=>void;vi.mocked(practiceApi.replay).mockImplementation(()=>new Promise(r=>finish=r));
 const pending=state.replay();await Promise.resolve();student='2';state.clear();finish({id:'old-account-practice'});expect(await pending).toBeNull();expect(state.selectedIds.value).toEqual([]);
});

test('reinforcement validates allocation and retries uncertain creation with the same key',async()=>{
 const state=useReviewSelection(()=> '1');await state.prepare(['12']);state.setAllocation('1',1.5);expect(await state.reinforce()).toBeNull();expect(practiceApi.reinforce).not.toHaveBeenCalled();
 state.setAllocation('1',3);vi.mocked(practiceApi.reinforce).mockRejectedValueOnce(new Error('offline')).mockResolvedValue({id:'reinforce'} as any);
 expect(await state.reinforce()).toBeNull();expect((await state.reinforce())?.id).toBe('reinforce');const calls=vi.mocked(practiceApi.reinforce).mock.calls;expect(calls[0]).toEqual(calls[1]);expect(calls[0][0]).toEqual(['12']);expect(calls[0][1]).toEqual([{pointId:'1',total:3}]);
 state.setAllocation('1',2);await state.reinforce();expect(vi.mocked(practiceApi.reinforce).mock.calls[2][2]).not.toBe(calls[0][2]);
});
test('cross grade choices create only the explicitly selected grade group',async()=>{
 const mixed={...options,replay:{...options.replay,allowed:false,items:[...options.replay.items,{id:'22',stem:'3 + 4 =',grade:2,pointId:'2',pointName:'二年级'}]},grades:[1,2],groups:[...options.groups,{grade:2,seedItemIds:['22'],points:[{pointId:'2',pointName:'二年级',available:20,reason:''}]}]};
 vi.mocked(practiceApi.reviewOptions).mockResolvedValue(mixed);vi.mocked(practiceApi.reinforce).mockResolvedValue({id:'grade2'} as any);const state=useReviewSelection(()=> '1');await state.prepare(['12','22']);state.selectGrade(2);state.setAllocation('2',2);await state.reinforce();expect(vi.mocked(practiceApi.reinforce).mock.calls[0][0]).toEqual(['22']);
});

test('reinforcement keeps all historical sources even when replay deduplicates them',async()=>{
 const grouped={...options,groups:[{grade:1,seedItemIds:['11','12'],points:[...options.groups[0].points,{pointId:'2',pointName:'后来分类',available:20,reason:''}]}]};
 vi.mocked(practiceApi.reviewOptions).mockResolvedValue(grouped);const state=useReviewSelection(()=> '1');await state.prepare(['11','12']);state.setAllocation('1',1);state.setAllocation('2',1);await state.reinforce();expect(vi.mocked(practiceApi.reinforce).mock.calls[0][0]).toEqual(['11','12']);
});

test('reopening unchanged selection preserves counts and uncertain reinforcement key',async()=>{
 const state=useReviewSelection(()=> '1');await state.prepare(['12']);state.setAllocation('1',7);vi.mocked(practiceApi.reinforce).mockRejectedValueOnce(new Error('response lost')).mockResolvedValue({id:'same'} as any);
 await state.reinforce();state.close();await state.prepare(['12']);expect(state.allocations.value['1']).toBe(7);await state.reinforce();const calls=vi.mocked(practiceApi.reinforce).mock.calls;expect(calls[1]).toEqual(calls[0]);
 state.toggle('13');await state.prepare();await state.reinforce();expect(vi.mocked(practiceApi.reinforce).mock.calls[2][2]).not.toBe(calls[0][2]);
});

test('reopening unchanged replay selection preserves uncertain request key',async()=>{
 const state=useReviewSelection(()=> '1');await state.prepare(['12']);vi.mocked(practiceApi.replay).mockRejectedValueOnce(new Error('response lost')).mockResolvedValue({id:'same'} as any);await state.replay();state.close();await state.prepare(['12']);await state.replay();const calls=vi.mocked(practiceApi.replay).mock.calls;expect(calls[1]).toEqual(calls[0]);
});

test('parent previews use family endpoints and a changed child cannot receive an old preview',async()=>{
 let child='1';const state=useReviewSelection(()=>child,true);vi.mocked(practiceApi.parentReviewOptions).mockResolvedValue(options);await state.prepare(['12']);expect(practiceApi.parentReviewOptions).toHaveBeenCalledWith('1',['12']);state.setAllocation('1',2);let finish!:(v:any)=>void;vi.mocked(practiceApi.parentReinforcePreview).mockImplementation(()=>new Promise(r=>finish=r));const pending=state.reinforce();child='2';state.clear();finish({id:'old-preview'});expect(await pending).toBeNull();expect(practiceApi.parentReviewOptions).toHaveBeenCalledWith('1',['12']);expect(practiceApi.reinforce).not.toHaveBeenCalled();expect(practiceApi.replay).not.toHaveBeenCalled();expect(await state.replay()).toBeNull();
});
