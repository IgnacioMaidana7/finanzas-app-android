import { createServer } from 'node:http';
import { createHandler } from './app.mjs';
import { firebaseDependencies } from './firebase.mjs';
import { firebaseRestDependencies } from './firebase-rest.mjs';

const mode = process.env.FIREBASE_DATA_ACCESS ?? 'admin';
if (!['admin', 'user-rest'].includes(mode)) throw new Error('Invalid FIREBASE_DATA_ACCESS');
const dependencies = mode === 'user-rest'
  ? firebaseRestDependencies(process.env.FIREBASE_PROJECT_ID, process.env.FIREBASE_WEB_API_KEY)
  : firebaseDependencies(process.env.FIREBASE_PROJECT_ID);
const server = createServer({ maxHeaderSize: 12288 }, createHandler({
  ...dependencies, log: entry => console.log(JSON.stringify(entry))
}));
server.requestTimeout = 15000;
server.headersTimeout = 10000;
server.listen(Number(process.env.PORT ?? 8080), '0.0.0.0');
process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(0), 9000).unref();
});
