import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import { MAX_TRANSACTIONS } from './summary.mjs';

export function firebaseDependencies(projectId) {
  if (!projectId) throw new Error('FIREBASE_PROJECT_ID is required');
  // Cloud Run must never accept unsigned emulator tokens.
  if (process.env.K_SERVICE && (process.env.FIREBASE_AUTH_EMULATOR_HOST || process.env.FIRESTORE_EMULATOR_HOST)) {
    throw new Error('Emulators are forbidden on Cloud Run');
  }
  const app = initializeApp({ projectId, credential: applicationDefault() });
  const auth = getAuth(app);
  const db = getFirestore(app);
  db.settings({ useBigInt: true });
  return {
    // true also checks revocation and disabled users. Project pins audience/issuer.
    verifyToken: token => auth.verifyIdToken(token, true),
    readTransactions: async (uid, { start, end }) => {
      const snapshot = await db.collection('users').doc(uid).collection('transactions')
        .where('date', '>=', start).where('date', '<', end).orderBy('date')
        .select('amount', 'type', 'date').limit(MAX_TRANSACTIONS + 1).get();
      return snapshot.docs.map(doc => doc.data());
    }
  };
}
