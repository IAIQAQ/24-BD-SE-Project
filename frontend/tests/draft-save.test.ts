import { expect,test } from 'vitest';
import { SaveQueue } from '../src/composables/saveQueue';

test('an in-flight save acknowledges only its snapshot and serializes newer input',async()=>{
  let resolveFirst!:(value:{version:number})=>void;let resolveSecond!:(value:{version:number})=>void;
  const requests:any[]=[];
  const queue=new SaveQueue(1,async payload=>{requests.push(payload);return new Promise(resolve=>{if(requests.length===1)resolveFirst=resolve;else resolveSecond=resolve;});});
  queue.change();const first=queue.save({answers:[{raw:'15'}]});await new Promise(r=>setTimeout(r,0));queue.change();
  const second=queue.save({answers:[{raw:'16'}]});expect(requests).toHaveLength(1);
  resolveFirst({version:2});await first;expect(queue.dirty).toBe(true);await new Promise(r=>setTimeout(r,0));resolveSecond({version:3});await second;
  expect(requests[1].baseVersion).toBe(2);expect(queue.dirty).toBe(false);expect(queue.version).toBe(3);
});
test('a lost response retries exactly the same key and snapshot before newer input',async()=>{
  const requests:any[]=[];let fail=true;
  const queue=new SaveQueue(1,async payload=>{requests.push(payload);if(fail){fail=false;throw new Error('network');}return {version:requests.length===2?2:3};});
  queue.change();await expect(queue.save({answers:[{raw:'15'}]})).rejects.toThrow();queue.change();
  await queue.save({answers:[{raw:'16'}]});
  expect(requests[0]).toEqual(requests[1]);expect(requests[2].answers[0].raw).toBe('16');expect(queue.dirty).toBe(false);
});
