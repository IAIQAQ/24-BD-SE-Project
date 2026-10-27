import {expect,test} from 'vitest';
import {ForegroundClock} from '../src/composables/foregroundClock';
test('visible time accumulates once, paused time does not count',()=>{
  let now=0;const clock=new ForegroundClock(()=>now);
  clock.start();now=1200;clock.start();expect(clock.elapsed()).toBe(1200);
  clock.pause();now=10000;expect(clock.elapsed()).toBe(1200);
  clock.start();now=10800;clock.pause();expect(clock.elapsed()).toBe(2000);
});
