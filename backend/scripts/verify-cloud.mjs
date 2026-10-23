// Explicitly opt in only after approving cloud usage. Never runs in normal tests/CI.
import { readFile, writeFile } from 'node:fs/promises';
import { randomBytes, randomUUID } from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';
import assert from 'node:assert/strict';

if (process.env.TP3_APPROVE_SYNTHETIC_CLOUD !== '1') throw new Error('Explicit cloud approval required');
const origin = new URL(process.env.SUMMARY_BASE_URL);
if (origin.protocol !== 'https:' || !/\.(azurecontainerapps\.io|run\.app)$/.test(origin.hostname) ||
    origin.pathname !== '/' || origin.search || origin.username || origin.password || origin.port) {
  throw new Error('Use only the reviewed HTTPS service origin');
}
const config = JSON.parse(await readFile(new URL('../../app/google-services.json', import.meta.url)));
const project = config.project_info.project_id;
if (project !== 'finanzasapp-1f76a') throw new Error('Unexpected Firebase project');
const key = config.client[0].api_key[0].current_key;
const month = new Date().toISOString().slice(0, 7);
const date = Date.parse(`${month}-15T12:00:00Z`);
const accounts = [];
const evidence = { environment: 'azure-real-synthetic-only', createdAt: new Date().toISOString(),
  month, realFinancialDataAccessed: false, serviceOrigin: origin.origin, checks: {}, cleanup: false };
const output = new URL('../../docs/tp3/evidence/cloud-validation.json', import.meta.url);

async function request(url, options = {}) {
  return fetch(url, { redirect: 'error', signal: AbortSignal.timeout(30000), ...options });
}
async function auth(method, body) {
  const res = await request(`https://identitytoolkit.googleapis.com/v1/accounts:${method}?key=${encodeURIComponent(key)}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`FIXTURE_AUTH_HTTP_${res.status}`);
  return res.json();
}
async function createAccount() {
  const email = `tp3-synthetic-${randomUUID()}@example.test`;
  const password = randomBytes(32).toString('base64url');
  const data = await auth('signUp', { email, password, returnSecureToken: true });
  const account = { uid: data.localId, token: data.idToken, email, documents: [] };
  assert(account.uid && account.token);
  accounts.push(account);
  return account;
}
function documentUrl(account, id) {
  return `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents/users/${encodeURIComponent(account.uid)}/transactions/${id}`;
}
async function summary(account, extra = '') {
  return request(`${origin.origin}/v1/summary?month=${month}${extra}`, {
    headers: account ? { Authorization: `Bearer ${account.token}` } : {}
  });
}

let failed = false;
let stage = 'health-and-anonymous';
try {
  const health = await request(`${origin.origin}/healthz`);
  assert.equal(health.status, 200);
  evidence.checks.health = 200;
  assert.equal((await summary()).status, 401);
  evidence.checks.unauthenticated = 401;
  stage = 'synthetic-accounts';
  const a = await createAccount();
  const b = await createAccount();
  // Recovery metadata only; intentionally gitignored. Never store credentials.
  await writeFile(new URL('../../docs/tp3/evidence/cloud-fixtures.local.json', import.meta.url),
    JSON.stringify(accounts.map(({ uid, email }) => ({ uid, email })), null, 2));
  stage = 'synthetic-documents';
  for (let i = 0; i < 10; i++) {
    const id = `tp3-fixture-${i}`;
    // Record before writing so even an ambiguous network failure is cleaned up.
    a.documents.push(id);
    const res = await request(documentUrl(a, id), { method: 'PATCH',
      headers: { Authorization: `Bearer ${a.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ fields: { amount: { integerValue: i < 5 ? '100' : '30' },
        type: { stringValue: i < 5 ? 'INGRESO' : 'GASTO' }, date: { integerValue: String(date) } } })
    });
    assert.equal(res.status, 200);
  }
  stage = 'authenticated-summary-and-isolation';
  const expected = { month, timeZone: 'UTC', unit: 'ARS_INTEGER', income: '500', expense: '150', net: '350', transactionCount: 10 };
  const aResult = await summary(a);
  assert.equal(aResult.status, 200);
  assert.deepEqual(await aResult.json(), expected);
  const bResult = await summary(b);
  assert.equal(bResult.status, 200);
  assert.equal((await bResult.json()).transactionCount, 0);
  assert.equal((await summary(a, '&uid=synthetic-injection')).status, 400);
  const forbidden = await request(documentUrl(a, a.documents[0]), { headers: { Authorization: `Bearer ${b.token}` } });
  assert.equal(forbidden.status, 403);
  evidence.checks.authenticatedSummary = 200;
  evidence.checks.otherUserSeesEmptyMonth = true;
  evidence.checks.uidInjection = 400;
  evidence.checks.firestoreCrossUser = 403;

  // Exactly 1,000 requests, paced at 25/s, at most 32 in flight; all synthetic.
  stage = 'bounded-synthetic-load';
  const started = performance.now();
  const latencies = [];
  let next = 0, errors = 0;
  await Promise.all(Array.from({ length: 32 }, async () => {
    while (next < 1000) {
      const i = next++;
      await delay(Math.max(0, started + i * 40 - performance.now()));
      const before = performance.now();
      try {
        const res = await summary(a);
        assert.equal(res.status, 200);
        assert.deepEqual(await res.json(), expected);
      } catch { errors++; }
      latencies.push(performance.now() - before);
    }
  }));
  latencies.sort((x, y) => x - y);
  evidence.load = { requests: 1000, maxConcurrency: 32, scheduledRequestsPerSecond: 25,
    syntheticDocumentsPerSummary: 10, errors, elapsedMs: Math.round(performance.now() - started),
    p50Ms: Math.round(latencies[499]), p95Ms: Math.round(latencies[949]), p99Ms: Math.round(latencies[989]),
    provesAutoscalingByItself: false };
  assert.equal(errors, 0);

  stage = 'revocation-and-fresh-session';
  await delay(1500);
  const oldToken = a.token;
  const password = randomBytes(32).toString('base64url');
  const changed = await auth('update', { idToken: oldToken, password, returnSecureToken: true });
  a.token = changed.idToken;
  const signedIn = await auth('signInWithPassword', { email: a.email, password, returnSecureToken: true });
  a.token = signedIn.idToken;
  assert.equal((await summary({ token: oldToken })).status, 401);
  evidence.checks.revokedSession = 401;
  assert.equal((await summary(a)).status, 200);
  evidence.checks.freshSession = 200;
} catch {
  failed = true;
  evidence.failed = true;
  evidence.failedStage = stage;
  // Do not print exception details: upstream messages can contain account data.
  console.error('Synthetic cloud verification failed; see sanitized evidence.');
} finally {
  let cleanupErrors = 0;
  for (const account of accounts) {
    for (const id of account.documents) {
      try {
        const res = await request(documentUrl(account, id), { method: 'DELETE', headers: { Authorization: `Bearer ${account.token}` } });
        if (![200, 404].includes(res.status)) cleanupErrors++;
      } catch { cleanupErrors++; }
    }
    try { await auth('delete', { idToken: account.token }); } catch { cleanupErrors++; }
  }
  evidence.cleanup = cleanupErrors === 0;
  evidence.cleanupErrors = cleanupErrors;
  await writeFile(output, JSON.stringify(evidence, null, 2) + '\n');
  console.log(JSON.stringify(evidence, null, 2));
  if (failed || cleanupErrors) process.exitCode = 1;
}
