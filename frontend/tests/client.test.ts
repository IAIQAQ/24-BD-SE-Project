import { beforeEach, expect, test, vi } from 'vitest';
import { health, login, me } from '../src/api/client';
beforeEach(() => vi.unstubAllGlobals());
test('health checks the real endpoint with cookie credentials and exposes errors', async () => {
  const fetcher = vi.fn().mockResolvedValue({ ok:true, json:async () => ({status:'UP'}) });
  vi.stubGlobal('fetch', fetcher);
  await expect(health()).resolves.toEqual({status:'UP'});
  expect(fetcher.mock.calls[0][0]).toMatch(/\/api\/v1\/health$/);
  expect(fetcher.mock.calls[0][1].credentials).toBe('include');
  fetcher.mockResolvedValue({ok:false,status:503,json:async()=>({message:'unavailable'})});
  await expect(health()).rejects.toThrow();
});
test('expired authentication retains its HTTP status for the view', async()=>{
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:false,status:401,json:async()=>({message:'请先登录'})}));
  await expect(me()).rejects.toMatchObject({status:401});
});
test('login fetches a CSRF token and includes it without storing the password', async () => {
  const profile = {id:'1',loginName:'student_a',nickname:'小明',role:'STUDENT'};
  const fetcher = vi.fn().mockResolvedValueOnce({ok:true,json:async()=>({data:{token:'csrf-test',headerName:'X-CSRF-TOKEN'}})})
    .mockResolvedValueOnce({ok:true,json:async()=>({data:profile})});
  vi.stubGlobal('fetch',fetcher);
  await expect(login('student_a','only-for-test')).resolves.toEqual(profile);
  expect(fetcher.mock.calls[0][0]).toMatch(/\/auth\/csrf$/);
  expect(fetcher.mock.calls[1][1].headers['X-CSRF-TOKEN']).toBe('csrf-test');
  expect(fetcher.mock.calls[1][1].credentials).toBe('include');
});
