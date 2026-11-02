// Uses the real HTTP handler with synthetic dependencies. No Firebase or cloud access.
import { createServer } from 'node:http';
import { once } from 'node:events';
import { performance } from 'node:perf_hooks';
import { writeFile, mkdir } from 'node:fs/promises';
import { createHandler } from '../src/app.mjs';

const count = 400;
const concurrency = 16;
const server = createServer(createHandler({
  verifyToken: async token => {
    if (token !== 'synthetic-only') throw new Error('invalid synthetic fixture');
    return { uid: 'synthetic-user' };
  },
  readTransactions: async (_uid, range) => {
    await new Promise(resolve => setTimeout(resolve, 15));
    return Array.from({ length: 100 }, (_, i) => ({
      amount: BigInt(i + 1), type: i % 2 ? 'INGRESO' : 'GASTO', date: range.start
    }));
  }
}));
server.listen(0, '127.0.0.1');
await once(server, 'listening');
const durations = [];
let errors = 0;
let next = 0;
const started = performance.now();
try {
  await Promise.all(Array.from({ length: concurrency }, async () => {
    while (next++ < count) {
      const start = performance.now();
      try {
        const result = await fetch(`http://127.0.0.1:${server.address().port}/v1/summary?month=2026-10`, {
          headers: { Authorization: 'Bearer synthetic-only' }, signal: AbortSignal.timeout(5000)
        });
        const body = await result.json();
        if (result.status !== 200 || body.income !== '2550' || body.expense !== '2500' ||
            body.net !== '50' || body.transactionCount !== 100) errors++;
      } catch { errors++; }
      durations.push(performance.now() - start);
    }
  }));
  durations.sort((a, b) => a - b);
  const elapsedMs = performance.now() - started;
  const percentile = p => Number(durations[Math.ceil(durations.length * p) - 1].toFixed(2));
  const report = { environment: 'local-synthetic-http', createdAt: new Date().toISOString(),
    node: process.version, requests: count, concurrency, syntheticRowsPerRequest: 100,
    injectedReadLatencyMs: 15, errors, elapsedMs: Math.round(elapsedMs),
    requestsPerSecond: Number((count / (elapsedMs / 1000)).toFixed(2)),
    p50Ms: percentile(0.5), p95Ms: percentile(0.95), p99Ms: percentile(0.99),
    provesCloudAutoscaling: false };
  await mkdir('../docs/tp3/evidence', { recursive: true });
  await writeFile('../docs/tp3/evidence/load-local.json', JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2));
  if (errors) process.exitCode = 1;
} finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
