import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { deleteApp, getApp } from 'firebase-admin/app';
import { firebaseDependencies } from '../src/firebase.mjs';

test('Cloud Run startup refuses emulator configuration and missing project', () => {
  assert.throws(() => firebaseDependencies(''), /required/);
  const result = spawnSync(process.execPath, ['--input-type=module', '-e',
    "import {firebaseDependencies} from './src/firebase.mjs'; firebaseDependencies('demo-finanzas-tp3')"], {
    cwd: process.cwd(), encoding: 'utf8', env: {
      ...process.env, K_SERVICE: 'synthetic-service', FIREBASE_AUTH_EMULATOR_HOST: '127.0.0.1:9099'
    }
  });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Emulators are forbidden/);
});

test('production Firebase SDK rejects unsigned, wrong-project and wrong-issuer tokens offline', async () => {
  assert(!process.env.FIREBASE_AUTH_EMULATOR_HOST);
  const dependencies = firebaseDependencies('demo-finanzas-tp3');
  const encode = data => Buffer.from(JSON.stringify(data)).toString('base64url');
  const payload = { aud: 'demo-finanzas-tp3', iss: 'https://securetoken.google.com/demo-finanzas-tp3',
    sub: 'synthetic-a', iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600,
    auth_time: Math.floor(Date.now() / 1000) };
  try {
    const tokens = [
      `${encode({ alg: 'none' })}.${encode(payload)}.`,
      `${encode({ alg: 'RS256', kid: 'synthetic' })}.${encode({ ...payload, aud: 'other-project' })}.ZmFrZQ`,
      `${encode({ alg: 'RS256', kid: 'synthetic' })}.${encode({ ...payload, iss: 'https://invalid.example' })}.ZmFrZQ`
    ];
    for (const token of tokens) {
      await assert.rejects(dependencies.verifyToken(token), error =>
        ['auth/argument-error', 'auth/invalid-id-token'].includes(error.code));
    }
  } finally { await deleteApp(getApp()); }
});
