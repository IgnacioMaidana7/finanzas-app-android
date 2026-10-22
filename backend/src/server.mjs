import { createServer } from 'node:http';
import { createHandler } from './app.mjs';
import { firebaseDependencies } from './firebase.mjs';

const dependencies = firebaseDependencies(process.env.FIREBASE_PROJECT_ID);
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
