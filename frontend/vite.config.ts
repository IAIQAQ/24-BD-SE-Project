import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { Agent } from 'node:https';
import { readFileSync, existsSync } from 'node:fs';
const certificate = new URL('../.local/dev-server.pem',import.meta.url);
export default defineConfig({ plugins:[vue()],server:{host:'localhost',proxy:{'/api':{target:'https://localhost:18443',secure:true,agent:existsSync(certificate)?new Agent({ca:readFileSync(certificate)}):undefined}}} });
