import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { ApiError, MAX_TRANSACTIONS } from './summary.mjs';

const invalidToken = () => Object.assign(new Error('Invalid session'), { code: 'auth/invalid-id-token' });
const invalidMessages = new Set(['INVALID_ID_TOKEN', 'TOKEN_EXPIRED', 'USER_DISABLED', 'USER_NOT_FOUND']);

async function readJson(response) {
  // A malicious user's own document must not cause unbounded response allocation.
  const reader = response.body.getReader();
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.length;
      if (size > 2 * 1024 * 1024) {
        await reader.cancel();
        throw new ApiError(422, 'INVALID_TRANSACTION_DATA');
      }
      chunks.push(value);
    }
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } finally { reader.releaseLock(); }
}

// Public Firebase client API key, not a service-account key. No administrative ADC.
// Each Firestore request uses that caller's ID token and is also checked by Rules.
export function createFirebaseRestAccess({ projectId, apiKey, verifyJwt, fetchImpl = fetch,
  authOrigin = 'https://identitytoolkit.googleapis.com', firestoreOrigin = 'https://firestore.googleapis.com' }) {
  if (!/^[a-z][a-z0-9-]{4,29}$/.test(projectId ?? '') || !apiKey) {
    throw new Error('Firebase project and client API key are required');
  }
  return {
    verifyToken: async idToken => {
      const claims = await verifyJwt(idToken);
      const response = await fetchImpl(`${authOrigin}/v1/accounts:lookup?key=${encodeURIComponent(apiKey)}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken }), redirect: 'error', signal: AbortSignal.timeout(8000)
      });
      const data = await readJson(response);
      if (!response.ok) {
        if (invalidMessages.has(data.error?.message)) throw invalidToken();
        throw new Error('Authentication unavailable');
      }
      const user = data.users?.length === 1 ? data.users[0] : null;
      if (!user || user.localId !== claims.uid || user.disabled) throw invalidToken();
      // Explicit revocation check, equivalent to Admin auth_time vs valid-after.
      if (!/^\d+$/.test(user.validSince ?? '') || !Number.isSafeInteger(claims.auth_time)) {
        throw new Error('Authentication status unavailable');
      }
      if (BigInt(claims.auth_time) < BigInt(user.validSince)) throw invalidToken();
      return claims;
    },
    readTransactions: async (uid, { start, end }, { idToken, claims }) => {
      if (!idToken || claims.uid !== uid || !uid || ['.', '..'].includes(uid) || uid.includes('/')) throw invalidToken();
      const parent = `projects/${projectId}/databases/(default)/documents/users/${uid}`;
      const encodedParent = `projects/${projectId}/databases/(default)/documents/users/${encodeURIComponent(uid)}`;
      const rangeFilter = (op, value) => ({ fieldFilter: {
        field: { fieldPath: 'date' }, op, value: { integerValue: String(value) }
      } });
      const response = await fetchImpl(`${firestoreOrigin}/v1/${encodedParent}:runQuery`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` },
        redirect: 'error', signal: AbortSignal.timeout(8000), body: JSON.stringify({ structuredQuery: {
          from: [{ collectionId: 'transactions' }],
          select: { fields: ['amount', 'type', 'date'].map(fieldPath => ({ fieldPath })) },
          where: { compositeFilter: { op: 'AND', filters: [
            rangeFilter('GREATER_THAN_OR_EQUAL', start), rangeFilter('LESS_THAN', end)
          ] } },
          orderBy: [{ field: { fieldPath: 'date' }, direction: 'ASCENDING' }], limit: MAX_TRANSACTIONS + 1
        } })
      });
      const data = await readJson(response);
      if (response.status === 401) throw new ApiError(401, 'UNAUTHENTICATED');
      if (!response.ok || !Array.isArray(data)) throw new Error('Firestore unavailable');
      return data.filter(item => item.document).map(({ document }) => {
        if (!document.name.startsWith(`${parent}/transactions/`)) throw new Error('Unexpected document');
        const fields = document.fields ?? {};
        const integer = value => /^-?\d{1,19}$/.test(value?.integerValue ?? '') ? BigInt(value.integerValue) : null;
        return { amount: integer(fields.amount), date: integer(fields.date), type: fields.type?.stringValue };
      });
    }
  };
}

export function firebaseRestDependencies(projectId, apiKey) {
  if (!/^[a-z][a-z0-9-]{4,29}$/.test(projectId ?? '') || !apiKey) {
    throw new Error('Firebase project and client API key are required');
  }
  const isDeployed = process.env.K_SERVICE || process.env.CONTAINER_APP_NAME;
  const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST;
  if ((authHost || firestoreHost) && (isDeployed || projectId !== 'demo-finanzas-tp3' ||
      authHost !== '127.0.0.1:9099' || firestoreHost !== '127.0.0.1:8088')) {
    throw new Error('Emulators require the isolated local demo project');
  }
  const app = initializeApp({ projectId }, 'user-rest');
  const auth = getAuth(app);
  return createFirebaseRestAccess({ projectId, apiKey,
    verifyJwt: token => auth.verifyIdToken(token, false),
    ...(authHost ? { authOrigin: `http://${authHost}/identitytoolkit.googleapis.com`,
      firestoreOrigin: `http://${firestoreHost}` } : {})
  });
}
