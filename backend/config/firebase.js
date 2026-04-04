import admin from 'firebase-admin';
import { readFile } from 'fs/promises';

// Load the Service Account JSON directly
const serviceAccount = JSON.parse(
  await readFile(new URL('./serviceAccountKey.json', import.meta.url))
);

// Initialize Firebase Admin SDK using the downloaded JSON
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

export const db = admin.firestore();
export const auth = admin.auth();
export const FieldValue = admin.firestore.FieldValue;

export default admin;
