export interface UserProfile { id:string; loginName:string; nickname:string; role:string; grade:number }
export class ApiError extends Error { constructor(public readonly status:number,message:string){super(message);} }
const base = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? '/api/v1' : 'https://localhost:18443/api/v1');
async function request<T>(path:string, options:RequestInit = {}):Promise<T> {
  const response = await fetch(`${base}${path}`, { ...options, credentials:'include' });
  if (response.status === 204) return undefined as T;
  const body = await response.json();
  if (!response.ok) throw new ApiError(response.status,body.message || `请求失败（${response.status}）`);
  return body.data ?? body;
}
async function post<T>(path:string, body?:unknown):Promise<T> {
  const csrf = await request<{token:string;headerName:string}>('/auth/csrf');
  return request<T>(path, {method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:body === undefined ? undefined : JSON.stringify(body)});
}
export const health = () => request<{status:string}>('/health');
export const login = (loginName:string,password:string) => post<UserProfile>('/auth/login',{loginName,password});
export const me = () => request<UserProfile>('/auth/me');
export const logout = () => post<void>('/auth/logout');
