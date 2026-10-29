export interface Point {id:string;name:string;grade:number;semester:string;available:number}
export interface Summary {id:string;pointId:string;total:number;status:string;version:number;currentSeq:number;elapsedMs:number;timingComplete:boolean;submittedAt:string;source?:string;senderId?:string;senderName?:string;grade?:number;pointName?:string;contentError?:boolean;errorNote?:string;practiceKind?:'NORMAL'|'REPLAY'|'REINFORCE'}
export interface DraftItem {id:string;seq:number;stem:string;raw:string}
export interface Draft extends Summary {items:DraftItem[]}
export interface Editor extends Summary {editorSessionId:string}
export interface ResultItem extends DraftItem {standardAnswer:string;explanation:string;outcome:string;practiceId:string;grade?:number;pointId?:string;pointName?:string;pointRevision?:number;practiceKind?:string;replayAllowed?:boolean;replayBlockReason?:string}
export interface Result extends Summary {studentId:string;correct:number;mistakes:number;unanswered:number;accuracy:string;items:ResultItem[]}
export interface Mistake extends ResultItem {count:number;history:ResultItem[]}
export interface Page<T> {items:T[];total:number;page:number;size:number;totalErrors?:number}
export interface Probe {operationKey:string;editorSessionId:string;baseVersion:number}
export interface Reconciled {status:'COMPLETED'|'CHECKING'|'NOT_COMMITTED'|'VOID'|'WITHDRAWN';canEdit:boolean;editorSessionId?:string;version:number;result?:Result}
export interface ReinforceGroup {grade:number;seedItemIds:string[];points:{pointId:string;pointName:string;available:number;reason:string}[]}
export interface ReviewOptions {replay:{allowed:boolean;reason:string;total:number;items:{id:string;stem:string;grade:number;pointId:string;pointName:string}[]};grades:number[];groups:ReinforceGroup[]}
export interface NormalAllocation {pointId:string;total:number}
