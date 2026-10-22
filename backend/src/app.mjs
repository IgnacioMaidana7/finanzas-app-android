import { performance } from 'node:perf_hooks';
import { ApiError, monthRange, summarize } from './summary.mjs';

const invalidAuthCodes = new Set([
  'auth/argument-error', 'auth/invalid-argument', 'auth/invalid-id-token',
  'auth/id-token-expired', 'auth/id-token-revoked', 'auth/user-disabled', 'auth/user-not-found'
]);

// Dependencies can be replaced by tests, never by HTTP flags or production env vars.
export function createHandler({ verifyToken, readTransactions, log = () => {} }) {
  return async (req, res) => {
    const started = performance.now();
    let status = 500;
    let code = 'INTERNAL_ERROR';
    const send = (s, data) => {
      status = s;
      res.writeHead(s, {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff'
      });
      res.end(JSON.stringify(data));
    };
    try {
      const url = new URL(req.url, 'http://localhost');
      if (url.pathname === '/healthz' && req.method === 'GET') {
        code = 'HEALTHY';
        return send(200, { status: 'ok' });
      }
      if (url.pathname !== '/v1/summary') throw new ApiError(404, 'NOT_FOUND');
      if (req.method !== 'GET') {
        res.setHeader('Allow', 'GET');
        throw new ApiError(405, 'METHOD_NOT_ALLOWED');
      }
      const header = req.headers.authorization;
      if (typeof header !== 'string' || header.length > 8192 || !/^Bearer \S+$/i.test(header)) {
        throw new ApiError(401, 'UNAUTHENTICATED');
      }
      let claims;
      try {
        claims = await verifyToken(header.slice(7));
      } catch (error) {
        if (invalidAuthCodes.has(error.code)) throw new ApiError(401, 'UNAUTHENTICATED');
        throw new ApiError(503, 'AUTH_UNAVAILABLE');
      }
      // UID comes exclusively from Firebase's verified claims; never from request input.
      if (typeof claims?.uid !== 'string' || !claims.uid || claims.uid.includes('/') ||
          claims.uid.length > 128) throw new ApiError(401, 'UNAUTHENTICATED');
      if ([...url.searchParams.keys()].some(key => key !== 'month') ||
          url.searchParams.getAll('month').length !== 1 ||
          req.headers['transfer-encoding'] || Number(req.headers['content-length'] ?? 0) !== 0) {
        throw new ApiError(400, 'INVALID_QUERY');
      }
      const range = monthRange(url.searchParams.get('month'));
      const rows = await readTransactions(claims.uid, range);
      const result = summarize(rows, range);
      code = 'SUMMARY_OK';
      send(200, result);
    } catch (error) {
      // Never echo Firebase/Firestore errors, paths, personal data, or credentials.
      const safe = error instanceof ApiError ? error : new ApiError(503, 'DATA_UNAVAILABLE');
      code = safe.code;
      send(safe.status, { error: { code } });
    } finally {
      // Only aggregate operational fields. No UID, URL, headers, or amounts.
      log({ status, code, durationMs: Math.round(performance.now() - started) });
    }
  };
}
