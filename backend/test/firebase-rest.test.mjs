import test from 'node:test';
import assert from 'node:assert/strict';
import { createFirebaseRestAccess } from '../src/firebase-rest.mjs';

const claims = { uid: 'synthetic-a', auth_time: 100 };
const range = { start: 1000, end: 2000 };
const json = (data, status = 200) => new Response(JSON.stringify(data), { status });
const create = (fetchImpl, overrides = {}) => createFirebaseRestAccess({
  projectId: 'demo-finanzas-tp3', apiKey: 'synthetic-client-key',
  verifyJwt: async () => claims, fetchImpl, ...overrides
});

test('REST verifies the caller account and compares revocation time without an admin key', async () => {
  const calls = [];
  const access = create(async (url, options) => {
    calls.push({ url, options });
    return json({ users: [{ localId: claims.uid, validSince: '100' }] });
  });
  assert.deepEqual(await access.verifyToken('synthetic-token'), claims);
  assert.deepEqual(JSON.parse(calls[0].options.body), { idToken: 'synthetic-token' });
  assert.equal(calls[0].options.redirect, 'error');
  assert(!calls[0].options.headers.Authorization);
});

test('REST rejects revoked, disabled, mismatched and absent users', async () => {
  for (const user of [{ localId: claims.uid, validSince: '101' },
    { localId: claims.uid, validSince: '0', disabled: true },
    { localId: 'synthetic-b', validSince: '0' }, null]) {
    const access = create(async () => json({ users: user ? [user] : [] }));
    await assert.rejects(access.verifyToken('synthetic-token'), e => e.code === 'auth/invalid-id-token');
  }
});

test('REST fails closed on missing status and service outages', async () => {
  await assert.rejects(create(async () => json({ users: [{ localId: claims.uid }] })).verifyToken('fixture'));
  for (const message of ['INVALID_ID_TOKEN', 'TOKEN_EXPIRED', 'USER_DISABLED', 'USER_NOT_FOUND']) {
    await assert.rejects(create(async () => json({ error: { message } }, 400)).verifyToken('fixture'),
      e => e.code === 'auth/invalid-id-token');
  }
  await assert.rejects(create(async () => json({ error: { message: 'API_KEY_INVALID' } }, 400))
    .verifyToken('fixture'), /unavailable/);
});

test('REST query carries per-request credentials, UID, field projection, range and cap', async () => {
  let observed;
  const access = create(async (url, options) => {
    observed = { url, options, query: JSON.parse(options.body).structuredQuery };
    return json([{ document: { name: 'projects/demo-finanzas-tp3/databases/(default)/documents/users/synthetic-a/transactions/fixture',
      fields: { amount: { integerValue: '9007199254740993' }, date: { integerValue: '1000' }, type: { stringValue: 'INGRESO' } }
    } }]);
  });
  const rows = await access.readTransactions(claims.uid, range, { idToken: 'fixture-a', claims });
  assert.equal(rows[0].amount, 9007199254740993n);
  assert.equal(observed.options.headers.Authorization, 'Bearer fixture-a');
  assert(observed.url.endsWith('/users/synthetic-a:runQuery'));
  assert.equal(observed.query.limit, 1001);
  assert.deepEqual(observed.query.select.fields.map(f => f.fieldPath), ['amount', 'type', 'date']);
  assert.deepEqual(observed.query.where.compositeFilter.filters.map(f => f.fieldFilter.value.integerValue), ['1000', '2000']);
  await assert.rejects(access.readTransactions('synthetic-b', range, { idToken: 'fixture-a', claims }));
});

test('REST handles empty months and rejects data returned from an unexpected path', async () => {
  const context = { idToken: 'fixture-a', claims };
  assert.deepEqual(await create(async () => json([{ readTime: 'fixture' }]))
    .readTransactions(claims.uid, range, context), []);
  await assert.rejects(create(async () => json([{ document: { name: 'users/other/private' } }]))
    .readTransactions(claims.uid, range, context), /Unexpected/);
  await assert.rejects(create(async () => json({ error: 'denied' }, 403))
    .readTransactions(claims.uid, range, context), /unavailable/);
});

test('REST caps dependency response size', async () => {
  const access = create(async () => json({ users: [], padding: 'x'.repeat(2 * 1024 * 1024) }));
  await assert.rejects(access.verifyToken('fixture'), /INVALID_TRANSACTION_DATA/);
});
