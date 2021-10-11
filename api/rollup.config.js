import {nodeResolve} from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import json from '@rollup/plugin-json';

export default {
  input: 'index.mjs',
  output: {
    file: 'bundle.mjs',
    format: 'es'
  }
};
