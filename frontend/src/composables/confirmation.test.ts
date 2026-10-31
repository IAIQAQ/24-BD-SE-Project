import {afterEach,expect,it} from 'vitest';
import {confirmation,confirmAction,finishConfirmation} from './confirmation';
afterEach(()=>finishConfirmation(false));
it('only explicit acceptance proceeds, and a second request cannot replace the visible operation',async()=>{
  const pending=confirmAction('发送三道题');
  expect(confirmation.value).toBe('发送三道题');
  expect(await confirmAction('解除关联')).toBe(false);
  expect(confirmation.value).toBe('发送三道题');
  finishConfirmation(false);expect(await pending).toBe(false);
  const accepted=confirmAction('发送三道题');finishConfirmation(true);
  expect(await accepted).toBe(true);expect(confirmation.value).toBeNull();
});
