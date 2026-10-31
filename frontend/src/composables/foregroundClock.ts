export class ForegroundClock {
  private accumulated=0;
  private started:number|null=null;
  constructor(private now:()=>number=()=>performance.now()){}
  start(){if(this.started===null)this.started=this.now();}
  pause(){this.accumulated=this.elapsed();this.started=null;}
  elapsed(){return Math.max(0,Math.floor(this.accumulated+(this.started===null?0:this.now()-this.started)));}
}
