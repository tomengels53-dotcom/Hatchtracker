import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  browserLocalPersistence,
  onAuthStateChanged,
  sendPasswordResetEmail,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js";
import {
  doc,
  getDoc,
  getFirestore,
  setDoc
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyB1FWNnl3TOjz6YKUici-yYriZFWhSCVAo",
  authDomain: "hatchtracker-8fae7.firebaseapp.com",
  projectId: "hatchtracker-8fae7",
  storageBucket: "hatchtracker-8fae7.firebasestorage.app"
};

const LEGAL_VERSIONS = {
  terms: "1.1.0",
  privacy: "1.1.0"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
const googleProvider = new GoogleAuthProvider();

googleProvider.setCustomParameters({
  prompt: "select_account"
});

await setPersistence(auth, browserLocalPersistence);

function deriveCountryCode() {
  const locale = Intl.DateTimeFormat().resolvedOptions().locale || "en-US";
  const region = locale.split("-")[1];
  return region || "US";
}

function deriveCurrencyCode(countryCode) {
  const currencyByCountry = {
    BE: "EUR",
    NL: "EUR",
    DE: "EUR",
    FR: "EUR",
    ES: "EUR",
    IT: "EUR",
    PT: "EUR",
    IE: "EUR",
    US: "USD",
    GB: "GBP",
    SE: "SEK",
    NO: "NOK",
    DK: "DKK",
    PL: "PLN",
    CZ: "CZK",
    CH: "CHF"
  };

  return currencyByCountry[countryCode] || "USD";
}

function buildSafeProfile(user) {
  const countryCode = deriveCountryCode();
  const now = Date.now();

  return {
    userId: user.uid,
    displayName: user.displayName || "",
    profilePictureUrl: user.photoURL || "",
    adsEnabled: true,
    createdAt: now,
    lastUpdated: now,
    termsVersionAccepted: LEGAL_VERSIONS.terms,
    privacyVersionAccepted: LEGAL_VERSIONS.privacy,
    consentTimestamp: now,
    countryCode,
    currencyCode: deriveCurrencyCode(countryCode),
    subscriptionTier: "FREE",
    preferredLanguage: navigator.language || ""
  };
}

async function ensureProfile(user) {
  const userRef = doc(db, "users", user.uid);
  const snapshot = await getDoc(userRef);

  if (!snapshot.exists()) {
    const profile = buildSafeProfile(user);
    await setDoc(userRef, profile);
    return profile;
  }

  return snapshot.data();
}

export async function signInWithEmail(email, password) {
  const result = await signInWithEmailAndPassword(auth, email, password);
  const profile = await ensureProfile(result.user);
  return { user: result.user, profile };
}

export async function signInWithGoogle() {
  const result = await signInWithPopup(auth, googleProvider);
  const profile = await ensureProfile(result.user);
  return { user: result.user, profile };
}

export async function requestPasswordReset(email) {
  await sendPasswordResetEmail(auth, email);
}

export async function loadSessionProfile(user) {
  return ensureProfile(user);
}

export async function logout() {
  await signOut(auth);
}

export function watchAuth(callback) {
  return onAuthStateChanged(auth, callback);
}

export { auth, db, LEGAL_VERSIONS };
