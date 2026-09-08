import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
export default defineConfig({root:path.resolve('standalone'),publicDir:path.resolve('public'),plugins:[react()],resolve:{alias:{'@':path.resolve('.')}},css:{postcss:path.resolve('.')},build:{outDir:path.resolve('server/web'),emptyOutDir:true}});
