import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import { deleteApp, getApp } from 'firebase-admin/app';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, collection, getDocs } from 'firebase/firestore';
import { firebaseDependencies } from '../src/firebase.mjs';
import { firebaseRestDependencies } from '../src/firebase-rest.mjs';
import { createHandler } from '../src/app.mjs';
import { monthRange } from '../src/summary.mjs';

const projectId = 'demo-finanzas-tp3';
// Hard fail rather than ever falling back to production/ADC.
assert.equal(process.env.GCLOUD_PROJECT, projectId);
assert.equal(process.env.FIREBASE_AUTH_EMULATOR_HOST, '127.0.0.1:9099');
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8088');

test('real emulator SDK: tenant isolation, date range, token rejection, rules and cap', async () => {
  const dependencies = firebaseDependencies(projectId);
  const db = getFirestore();
  const auth = getAuth();
  const start = monthRange('2026-10').start;
  const a = 'tp3-synthetic-a';
  const b = 'tp3-synthetic-b';
  const environment = await initializeTestEnvironment({ projectId,
    firestore: { host: '127.0.0.1', port: 8088, rules: await readFile('../firestore.rules', 'utf8') }
  });
  const restDependencies = firebaseRestDependencies(projectId, 'synthetic');
  const server = createServer(createHandler(dependencies));
  const restServer = createServer(createHandler(restDependencies));
  server.listen(0, '127.0.0.1');
  restServer.listen(0, '127.0.0.1');
  await once(server, 'listening');
  if (!restServer.listening) await once(restServer, 'listening');
  const request = async (token, query = 'month=2026-10') => {
    const res = await fetch(`http://127.0.0.1:${server.address().port}/v1/summary?${query}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    return { status: res.status, body: await res.json() };
  };
  try {
    await environment.clearFirestore();
    await auth.createUser({ uid: a, email: 'tp3-synthetic-a@example.test', password: 'synthetic-local-Only-123!' });
    await auth.createUser({ uid: b });
    const customToken = await auth.createCustomToken(a);
    const signIn = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=synthetic', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: customToken, returnSecureToken: true })
    });
    const { idToken } = await signIn.json();
    assert.equal(typeof idToken, 'string');
    await db.doc(`users/${a}/transactions/income`).set({ amount: 9007199254740993n, type: 'INGRESO', date: start });
    await db.doc(`users/${a}/transactions/expense`).set({ amount: 30, type: 'GASTO', date: start });
    await db.doc(`users/${a}/transactions/outside`).set({ amount: 999, type: 'GASTO', date: start - 1 });
    await db.doc(`users/${b}/transactions/private`).set({ amount: 777, type: 'INGRESO', date: start });
    assert.equal((await request(null)).status, 401);
    assert.equal((await request('forged')).status, 401);
    assert.equal((await request(idToken, `month=2026-10&uid=${b}`)).status, 400);
    const summary = await request(idToken);
    assert.equal(summary.status, 200);
    assert.equal(summary.body.income, '9007199254740993');
    assert.equal(summary.body.expense, '30');
    assert.equal(summary.body.transactionCount, 2);
    await restDependencies.verifyToken(idToken);
    const restResponse = await fetch(`http://127.0.0.1:${restServer.address().port}/v1/summary?month=2026-10`, {
      headers: { Authorization: `Bearer ${idToken}` }
    });
    const restBody = await restResponse.json();
    assert.equal(restResponse.status, 200, JSON.stringify(restBody));
    assert.deepEqual(restBody, summary.body);

    const own = environment.authenticatedContext(a).firestore();
    const anonymous = environment.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(own, `users/${a}/transactions/income`)));
    await assertFails(getDoc(doc(own, `users/${b}/transactions/private`)));
    await assertFails(setDoc(doc(own, `users/${b}/transactions/injected`), { amount: 1 }));
    await assertFails(getDoc(doc(anonymous, `users/${a}/transactions/income`)));
    await assertFails(getDocs(collection(own, `users/${b}/transactions`)));
    for (const child of ['accounts', 'categories', 'budgets', 'goals']) {
      await assertSucceeds(setDoc(doc(own, `users/${a}/${child}/fixture`), { synthetic: true }));
    }
    await assertSucceeds(setDoc(doc(own, `users/${a}/goals/fixture/contributions/fixture`), { amount: 1 }));
    await assertFails(setDoc(doc(own, `users/${a}/unapproved/fixture`), { amount: 1 }));
    await assertFails(setDoc(doc(own, 'admin/fixture'), { role: 'admin' }));

    const writer = db.bulkWriter();
    for (let i = 0; i < 1000; i++) {
      writer.set(db.doc(`users/${a}/transactions/cap-${i}`), { amount: 1, type: 'GASTO', date: start });
    }
    await writer.close();
    const capped = await request(idToken);
    assert.equal(capped.status, 422);
    assert.equal(capped.body.error.code, 'SUMMARY_TOO_LARGE');
    await auth.updateUser(a, { disabled: true });
    assert.equal((await request(idToken)).status, 401);
    assert.equal((await fetch(`http://127.0.0.1:${restServer.address().port}/v1/summary?month=2026-10`, {
      headers: { Authorization: `Bearer ${idToken}` }
    })).status, 401);
  } finally {
    restServer.closeAllConnections();
    await new Promise(resolve => restServer.close(resolve));
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
    await environment.clearFirestore();
    await environment.cleanup();
    await auth.deleteUsers([a, b]);
    await deleteApp(getApp());
    await deleteApp(getApp('user-rest'));
  }
});
