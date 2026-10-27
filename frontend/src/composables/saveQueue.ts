type Snapshot=Record<string,unknown>;
type Pending={payload:Snapshot;revision:number};
export class SaveQueue {
  private revision=0;
  private acknowledged=0;
  private pending:Pending|null=null;
  private tail:Promise<unknown>=Promise.resolve();
  constructor(public version:number,private send:(payload:any)=>Promise<{version:number}>){ }
  get dirty(){return this.revision>this.acknowledged;}
  change(){this.revision++;}
  async flush(){await this.tail;}
  private async retry(){
    if(!this.pending)return;
    const sending=this.pending;
    const result=await this.send(sending.payload);
    this.version=result.version;this.acknowledged=sending.revision;this.pending=null;
  }
  save(snapshot:Snapshot):Promise<void>{
    const revision=this.revision;const copy=structuredClone(snapshot);
    const operation=this.tail.catch(()=>{}).then(async()=>{
      await this.retry();
      if(revision<=this.acknowledged)return;
      this.pending={revision,payload:{...copy,baseVersion:this.version,operationKey:crypto.randomUUID()}};
      await this.retry();
    });
    this.tail=operation;return operation;
  }
}
