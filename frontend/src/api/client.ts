export interface UserProfile { id:string; loginName:string; nickname:string; role:string; grade:number }
export class ApiError extends Error { constructor(public readonly status:number,message:string,public readonly code=''){super(message);} }
const base = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? '/api/v1' : 'https://localhost:18443/api/v1');
async function request<T>(path:string, options:RequestInit = {}):Promise<T> {
  const response = await fetch(`${base}${path}`, { ...options, credentials:'include',signal:options.signal??AbortSignal.timeout(12000) });
  if (response.status === 204) return undefined as T;
  const body = await response.json();
  if (!response.ok) throw new ApiError(response.status,body.message || `请求失败（${response.status}）`,body.code);
  return body.data ?? body;
}
async function post<T>(path:string, body?:unknown,method='POST'):Promise<T> {
  const csrf = await request<{token:string;headerName:string}>('/auth/csrf');
  return request<T>(path, {method,headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:body === undefined ? undefined : JSON.stringify(body)});
}
export const health = () => request<{status:string}>('/health');
export const login = (loginName:string,password:string) => post<UserProfile>('/auth/login',{loginName,password});
export const me = () => request<UserProfile>('/auth/me');
export const logout = () => post<void>('/auth/logout');
import type {Point,Summary,Draft,Editor,Result,Page,Mistake,Probe,Reconciled} from './contracts';
export const practiceApi={
  points:()=>request<Point[]>('/catalog/points'),
  list:(page=1)=>request<Page<Summary>>(`/practices?page=${page}`),
  create:(pointId:string,total:number,operationKey:string)=>post<Summary>('/practices',{pointId,total,operationKey}),
  open:(id:string,operationKey:string,takeover=false)=>post<Editor>(`/practices/${id}/open`,{operationKey,takeover}),
  draft:(id:string)=>request<Draft>(`/practices/${id}/draft`),
  save:(id:string,payload:unknown)=>post<{version:number}>(`/practices/${id}/answers`,payload,'PUT'),
  segment:(id:string,payload:unknown)=>post<{elapsedMs:number}>(`/practices/${id}/segments`,payload),
  submit:(id:string,payload:unknown)=>post<Result>(`/practices/${id}/submit`,payload),
  reconcile:(id:string,payload:Probe)=>post<Reconciled>(`/practices/${id}/submission-status`,payload),
  result:(id:string)=>request<Result>(`/practices/${id}/result`),
  mistakes:(student:string,page=1,pointId='')=>request<Page<Mistake>>(`/students/${student}/mistakes?page=${page}${pointId?'&pointId='+encodeURIComponent(pointId):''}`),
};
