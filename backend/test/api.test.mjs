import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { createHandler } from '../src/app.mjs';
import { monthRange, summarize } from '../src/summary.mjs';

const range = monthRange('2026-10');
const transaction = (amount, type = 'GASTO', date = range.start) => ({ amount, type, date });

async function withApi(run, overrides = {}) {
  const calls = [];
  const logs = [];
  const server = createServer(createHandler({
    verifyToken: async token => {
      if (token !== 'valid-a' && token !== 'valid-b') {
        throw Object.assign(new Error('secret-token'), { code: 'auth/invalid-id-token' });
      }
      return { uid: token === 'valid-a' ? 'synthetic-a' : 'synthetic-b' };
    },
    readTransactions: async (uid, r) => {
      calls.push({ uid, range: r });
      return [transaction(uid === 'synthetic-a' ? 100n : 900n, 'INGRESO'), transaction(30n)];
    },
    log: entry => logs.push(entry), ...overrides
  }));
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const request = async (path = '/v1/summary?month=2026-10', token = 'valid-a', method = 'GET') => {
    const response = await fetch(`http://127.0.0.1:${server.address().port}${path}`, {
      method, headers: token === null ? {} : { Authorization: `Bearer ${token}` }
    });
    return { status: response.status, headers: response.headers, body: await response.json() };
  };
  try { await run({ request, calls, logs }); }
  finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
}

test('authenticated users see only their own summary; responses are never cached', async () => {
  await withApi(async ({ request, calls, logs }) => {
    const a = await request();
    const b = await request(undefined, 'valid-b');
    assert.equal(a.status, 200);
    assert.deepEqual(a.body, { month: '2026-10', timeZone: 'UTC', unit: 'ARS_INTEGER',
      income: '100', expense: '30', net: '70', transactionCount: 2 });
    assert.equal(b.body.income, '900');
    assert.deepEqual(calls.map(c => c.uid), ['synthetic-a', 'synthetic-b']);
    assert.equal(a.headers.get('cache-control'), 'no-store');
    assert(!JSON.stringify(logs).includes('synthetic'));
    assert(!JSON.stringify(logs).includes('valid-a'));
  });
});

test('missing, malformed and invalid tokens cannot query data', async () => {
  await withApi(async ({ request, calls }) => {
    for (const token of [null, '', 'forged', 'x'.repeat(8200)]) {
      assert.equal((await request(undefined, token)).status, 401);
    }
    assert.equal(calls.length, 0);
  });
});

for (const code of ['auth/id-token-expired', 'auth/id-token-revoked', 'auth/user-disabled', 'auth/user-not-found']) {
  test(`rejects ${code}`, async () => withApi(async ({ request, calls }) => {
    assert.equal((await request()).status, 401);
    assert.equal(calls.length, 0);
  }, { verifyToken: async () => { throw Object.assign(new Error('private'), { code }); } }));
}

test('rejects UID injection, repeated/missing/invalid month and unexpected parameters', async () => {
  await withApi(async ({ request, calls }) => {
    for (const query of ['month=2026-10&uid=other', 'month=2026-10&month=2026-11',
      '', 'month=2026-13', 'month=2026-1', 'month=1900-01', 'month=2026-10&limit=9999']) {
      assert.equal((await request(`/v1/summary?${query}`)).status, 400, query);
    }
    assert.equal(calls.length, 0);
  });
});

test('health, unsupported methods and unknown routes', async () => {
  await withApi(async ({ request, calls }) => {
    assert.equal((await request('/healthz', null)).status, 200);
    assert.equal((await request(undefined, 'valid-a', 'POST')).status, 405);
    assert.equal((await request('/users/other/summary')).status, 404);
    assert.equal(calls.length, 0);
  });
});

test('infrastructure failures reveal no internal detail and fail closed', async () => {
  await withApi(async ({ request }) => {
    const result = await request();
    assert.equal(result.status, 503);
    assert.deepEqual(result.body, { error: { code: 'DATA_UNAVAILABLE' } });
  }, { readTransactions: async () => { throw new Error('users/private secret-token'); } });
  await withApi(async ({ request, calls }) => {
    assert.equal((await request()).body.error.code, 'AUTH_UNAVAILABLE');
    assert.equal(calls.length, 0);
  }, { verifyToken: async () => { throw new Error('IAM private'); } });
});

test('month boundaries include leap day and year rollover', () => {
  assert.equal(monthRange('2024-02').end - monthRange('2024-02').start, 29 * 86400000);
  assert.equal(monthRange('2026-12').end, Date.UTC(2027, 0, 1));
  assert.equal(summarize([], range).net, '0');
  assert.equal(summarize([transaction(1n, 'INGRESO', range.end - 1)], range).income, '1');
  assert.throws(() => summarize([transaction(1n, 'INGRESO', range.end)], range), /INVALID_TRANSACTION/);
});

test('integer precision, invalid data, Long overflow and 1000-document cap', () => {
  assert.equal(summarize([transaction(9007199254740993n, 'INGRESO')], range).income, '9007199254740993');
  for (const amount of [-1, 0.1, Number.MAX_SAFE_INTEGER + 1, '10', null, 9223372036854775808n]) {
    assert.throws(() => summarize([transaction(amount)], range), /INVALID_TRANSACTION_DATA/);
  }
  assert.throws(() => summarize([transaction(1n, 'unknown')], range), /INVALID_TRANSACTION_DATA/);
  assert.throws(() => summarize([transaction(9223372036854775807n), transaction(1n)], range), /AMOUNT_OVERFLOW/);
  assert.equal(summarize(Array.from({ length: 1000 }, () => transaction(1n)), range).expense, '1000');
  assert.throws(() => summarize(Array.from({ length: 1001 }, () => transaction(1n)), range), /SUMMARY_TOO_LARGE/);
});
