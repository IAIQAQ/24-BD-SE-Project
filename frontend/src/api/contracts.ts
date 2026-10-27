export interface Point {id:string;name:string;grade:number;semester:string;available:number}
export interface Summary {id:string;pointId:string;total:number;status:string;version:number;currentSeq:number;elapsedMs:number;timingComplete:boolean;submittedAt:string}
export interface DraftItem {id:string;seq:number;stem:string;raw:string}
export interface Draft extends Summary {items:DraftItem[]}
export interface Editor extends Summary {editorSessionId:string}
export interface ResultItem extends DraftItem {standardAnswer:string;explanation:string;outcome:string;practiceId:string}
export interface Result extends Summary {studentId:string;correct:number;mistakes:number;unanswered:number;accuracy:string;items:ResultItem[]}
export interface Mistake extends ResultItem {count:number;history:ResultItem[]}
export interface Page<T> {items:T[];total:number;page:number;size:number;totalErrors?:number}
export interface Probe {operationKey:string;editorSessionId:string;baseVersion:number}
export interface Reconciled {status:'COMPLETED'|'CHECKING'|'NOT_COMMITTED';canEdit:boolean;editorSessionId?:string;version:number;result?:Result}
