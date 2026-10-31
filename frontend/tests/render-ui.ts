import * as Vue from 'vue';
import {createRenderer,h,ssrContextKey} from 'vue';
import {parse,compileScript,compileTemplate} from '@vue/compiler-sfc';
import ts from 'typescript';

type Node={tag:string;text:string;props:Record<string,any>;children:Node[];parent:Node|null};
export function renderUi(component:any,source:string,props:()=>Record<string,unknown>){
 const nodes:Node[]=[];
 const make=(tag:string,text=''):Node=>{const node={tag,text,props:{},children:[],parent:null};nodes.push(node);return node;};
 const renderer=createRenderer<Node,Node>({createElement:tag=>make(tag),createText:text=>make('#text',text),createComment:()=>make('#comment'),setText:(n,t)=>{n.text=t;},setElementText:(n,t)=>{n.text=t;},patchProp:(n,k,_old,v)=>{n.props[k]=v;},insert:(n,p,anchor)=>{if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=p;const i=anchor?p.children.indexOf(anchor):-1;p.children.splice(i<0?p.children.length:i,0,n);},remove:n=>{if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=null;},parentNode:n=>n.parent,nextSibling:n=>n.parent?.children[n.parent.children.indexOf(n)+1]??null});
 // Vitest loads SFCs through SSR; run the real client template on the Vue host renderer.
 const {descriptor}=parse(source);const bindings=compileScript(descriptor,{id:'ui-test'}).bindings;
 const {code}=compileTemplate({source:descriptor.template!.content,filename:'UiTest.vue',id:'ui-test',compilerOptions:{bindingMetadata:bindings}});
 const js=ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText;
 const body=js.replace(/import \{([^}]+)\} from "vue"/g,(_,names)=>`const {${names.replace(/ as /g,':')}} = Vue;`).replace('export function render','return function render');
 const client={...component,render:new Function('Vue',body)(Vue),ssrRender:undefined};
 const app=renderer.createApp({render:()=>h(client,props())});app.provide(ssrContextKey,{modules:new Set()});app.mount(make('root'));
 const text=(n:Node):string=>n.text+n.children.map(text).join('');
 const button=(label:string)=>nodes.find(n=>n.tag==='button'&&n.parent&&text(n)===label);
 return {nodes,app,button,text};
}
