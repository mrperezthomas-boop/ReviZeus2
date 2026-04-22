import * as functions from "firebase-functions";
import {VertexAI} from "@google-cloud/vertexai";
import * as admin from "firebase-admin";

admin.initializeApp();

// Configuration Vertex AI
const PROJECT_ID = "revizeus";
const LOCATION = "us-central1";
const ORACLE_LOCATION = "us-central1";
const FUNCTION_SERVICE_ACCOUNT =
  "revizeus-functions-sa@revizeus.iam.gserviceaccount.com";
const ORACLE_MAX_ATTEMPTS = 3;
const ORACLE_BASE_DELAY_MS = 1500;
const METADATA_TOKEN_URL =
  "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token";
const ORACLE_RESPONSE_SNIPPET_MAX = 240;

// Initialisation Vertex AI
const vertexAI = new VertexAI({
  project: PROJECT_ID,
  location: LOCATION,
});

/**
 * [2026-04-21][FIX_ORACLE_VERTEX_REST]
 * Le transport Oracle ne passe plus par @google-cloud/vertexai 1.x.
 *
 * Cause racine observée en prod :
 * - le callable Firebase passait correctement
 * - l'appel Oracle recevait parfois une page HTML au lieu d'un JSON
 * - le SDK tentait ensuite de parser ce HTML, d'où :
 *   `Unexpected token '<', "<!DOCTYPE"... is not valid JSON`
 *
 * Correctif minimal :
 * - conserver Lyria sur le SDK existant
 * - migrer uniquement le transport Oracle vers l'endpoint REST officiel
 *   Vertex AI generateContent
 * - authentifier via le service account attaché à la function
 * - ajouter des logs défensifs propres
 */

// Limites quotidiennes par utilisateur
// MODE TEST TEMPORAIRE : désactive les quotas pour pouvoir valider le flow
// musical sans attendre le reset quotidien.
const DISABLE_QUOTA = true;
const DAILY_QUOTA_PER_USER = 5;
const DAILY_QUOTA_GLOBAL = 100;
const TEST_REMAINING_QUOTA = 9999;

type OracleInlineImage = {
  mimeType?: string;
  dataBase64?: string;
};

type InvokeDivineOracleRequest = {
  systemInstruction?: string;
  prompt?: string;
  model?: string;
  images?: OracleInlineImage[];
};

type OraclePart = {
  text?: string;
  inlineData?: {
    mimeType: string;
    data: string;
  };
};

type OracleContent = {
  role: string;
  parts: OraclePart[];
};

type OracleGenerateContentResponse = {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
  error?: {
    code?: number;
    message?: string;
    status?: string;
  };
};

type CachedAccessToken = {
  token: string;
  expiresAtMs: number;
};

let cachedAccessToken: CachedAccessToken | null = null;

/**
 * [2026-04-20 23:59][TRANSPORT_IA_TOTAL]
 * Cloud Function callable pour le flux Oracle total.
 *
 * Contrat :
 * - Auth obligatoire
 * - Reçoit systemInstruction + prompt + images déjà construits côté Android
 * - Exécute Gemini côté backend
 * - Retourne UNIQUEMENT le texte brut
 *
 * Aucun prompt métier n'est déplacé ici.
 */
export const invokeDivineOracle = functions
  .region(ORACLE_LOCATION)
  .runWith({
    timeoutSeconds: 120,
    memory: "1GB",
    serviceAccount: FUNCTION_SERVICE_ACCOUNT,
  })
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Tu dois être connecté à l'Olympe pour invoquer l'Oracle."
      );
    }

    const payload = (data ?? {}) as InvokeDivineOracleRequest;

    const systemInstruction =
      typeof payload.systemInstruction === "string" ?
        payload.systemInstruction.trim() :
        "";

    const prompt =
      typeof payload.prompt === "string" ?
        payload.prompt.trim() :
        "";

    const model =
      typeof payload.model === "string" && payload.model.trim().length > 0 ?
        payload.model.trim() :
        "gemini-2.5-flash";

    const images = Array.isArray(payload.images) ? payload.images : [];

    if (!systemInstruction) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "systemInstruction manquante."
      );
    }

    if (!prompt) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "prompt manquant."
      );
    }

    try {
      const text = await generateOracleTextWithRetry(
        systemInstruction,
        prompt,
        model,
        images
      );

      if (!text) {
        throw new functions.https.HttpsError(
          "internal",
          "L'Oracle n'a renvoyé aucun texte exploitable."
        );
      }

      return {text};
    } catch (error: unknown) {
      console.error("Erreur invokeDivineOracle:", error);

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      if (isRetryableOracleError(error)) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "L'Oracle est momentanément saturé. Réessaie dans un instant."
        );
      }

      const message = error instanceof Error ? error.message : "Erreur inconnue";

      throw new functions.https.HttpsError(
        "internal",
        `L'Oracle n'a pas pu répondre : ${message}`
      );
    }
  });

/**
 * Cloud Function : Génère une musique avec Lyria
 *
 * Appelée depuis Android avec :
 * {
 *   "godName": "ZEUS",
 *   "lyrics": "paroles complètes...",
 *   "musicalStyle": "Epic orchestral...",
 *   "courseName": "Nom du savoir"
 * }
 */
export const generateDivineMusic = functions
  .region(LOCATION)
  .runWith({
    timeoutSeconds: 300,
    memory: "1GB",
    serviceAccount: FUNCTION_SERVICE_ACCOUNT,
  })
  .https.onCall(async (data, context) => {
    // Vérifier que l'utilisateur est authentifié
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Tu dois être connecté à l'Olympe pour invoquer Lyria !"
      );
    }

    const userId = context.auth.uid;
    const godName = typeof data?.godName === "string" ? data.godName.trim() : "";
    const lyrics = typeof data?.lyrics === "string" ? data.lyrics.trim() : "";
    const musicalStyle = typeof data?.musicalStyle === "string" ?
      data.musicalStyle.trim() :
      "";
    const courseName = typeof data?.courseName === "string" ?
      data.courseName.trim() :
      undefined;

    // Validation des données
    if (!godName || !lyrics || !musicalStyle) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Les Muses ont besoin du nom du dieu, des paroles et du style musical !"
      );
    }

    try {
      // MODE TEST TEMPORAIRE : les quotas restent disponibles dans le code,
      // mais on les neutralise pour pouvoir tester librement la création.
      const userQuota = DISABLE_QUOTA ? 0 : await checkUserQuota(userId);
      if (!DISABLE_QUOTA && userQuota >= DAILY_QUOTA_PER_USER) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "QUOTA_USER_EXCEEDED"
        );
      }

      const globalQuota = DISABLE_QUOTA ? 0 : await checkGlobalQuota();
      if (!DISABLE_QUOTA && globalQuota >= DAILY_QUOTA_GLOBAL) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "QUOTA_GLOBAL_EXCEEDED"
        );
      }

      // Construire le prompt pour Lyria
      const prompt = buildLyriaPrompt(
        godName,
        lyrics,
        musicalStyle,
        courseName
      );

      // Appeler Lyria via Vertex AI
      const generativeModel = vertexAI.preview.getGenerativeModel({
        model: "lyria-1.5",
      });

      const result = await generativeModel.generateContent({
        contents: [
          {
            role: "user",
            parts: [{text: prompt}],
          },
        ],
      });

      // Extraire l'URL de l'audio généré
      const response = result.response;
      const audioUrl = extractAudioUrl(response);

      if (!audioUrl) {
        throw new functions.https.HttpsError(
          "internal",
          "Lyria n'a pas pu créer de musique divine pour ce savoir."
        );
      }

      // En mode test, on n'incrémente plus les quotas pour laisser le champ libre.
      if (!DISABLE_QUOTA) {
        await incrementUserQuota(userId);
        await incrementGlobalQuota();
      }

      const remainingQuota = DISABLE_QUOTA ?
        TEST_REMAINING_QUOTA :
        DAILY_QUOTA_PER_USER - (userQuota + 1);

      return {
        success: true,
        audioUrl: audioUrl,
        remainingQuota: remainingQuota,
        message: DISABLE_QUOTA ?
          "Les Muses ont chanté ! Mode test divin actif : quota temporairement levé." :
          `Les Muses ont chanté ! ${remainingQuota} invocations restantes aujourd'hui.`,
      };
    } catch (error: unknown) {
      console.error("Erreur génération Lyria:", error);

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      const message = error instanceof Error ? error.message : "Erreur inconnue";

      throw new functions.https.HttpsError(
        "internal",
        `Les cordes de la lyre divine sont rompues : ${message}`
      );
    }
  });

/**
 * Construit le prompt pour Lyria.
 *
 * @param {string} godName Nom du dieu invoqué.
 * @param {string} lyrics Paroles à mettre en musique.
 * @param {string} musicalStyle Style musical souhaité.
 * @param {string | undefined} courseName Nom du savoir lié à la chanson.
 * @return {string} Prompt complet à envoyer au modèle.
 */
function buildLyriaPrompt(
  godName: string,
  lyrics: string,
  musicalStyle: string,
  courseName?: string
): string {
  return `
Créer une musique chantée pour ${godName}, dieu/déesse de l'Olympe.

STYLE MUSICAL :
${musicalStyle}

PAROLES OFFICIELLES À CHANTER (OBLIGATOIRES — INTERDICTION DE MODIFIER, RÉÉCRIRE, RÉSUMER OU AJOUTER DU TEXTE) :
"""
${lyrics}
"""

CONTEXTE :
${courseName ? `Savoir étudié : ${courseName}` : "Chanson éducative divine"}

CONTRAINTES ABSOLUES :
- Les paroles doivent être chantées EXACTEMENT telles qu'elles sont écrites.
- AUCUNE improvisation n'est autorisée.
- AUCUNE parole supplémentaire n'est autorisée.
- AUCUNE reformulation n'est autorisée.
- La musique doit suivre le texte ligne par ligne.
- Chaque ligne doit être distinctement audible et compréhensible.

CONTRAINTES AUDIO :
- Voix chantée claire, bien articulée et intelligible.
- Le volume de la voix doit rester au-dessus de l'instrumental.
- Pas de morceau purement instrumental.
- Pas d'introduction instrumentale longue.
- Priorité absolue au chant et à l'intelligibilité des mots.
- Durée cible : 45 à 90 secondes, en respectant tout le texte fourni.

FORMAT ATTENDU :
- Audio chanté au format .mp3 ou .wav
`.trim();
}

/**
 * Extrait le texte brut de la réponse Gemini.
 *
 * @param {unknown} response Réponse brute du modèle.
 * @return {string | null} Texte si trouvé, sinon null.
 */
function extractGeneratedText(response: unknown): string | null {
  const typedResponse = response as {
    candidates?: Array<{
      content?: {
        parts?: Array<{
          text?: string;
        }>;
      };
    }>;
  };

  const parts = typedResponse.candidates?.[0]?.content?.parts ?? [];
  const text = parts
    .map((part) => typeof part.text === "string" ? part.text : "")
    .join("")
    .trim();

  return text.length > 0 ? text : null;
}

/**
 * Pause utilitaire pour backoff Oracle.
 *
 * @param {number} ms Durée en millisecondes.
 * @return {Promise<void>}
 */
function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Détecte les erreurs retryables Vertex AI / capacité.
 *
 * @param {unknown} error Erreur brute.
 * @return {boolean}
 */
function isRetryableOracleError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error ?? "");
  return (
    message.includes("429") ||
    message.includes("RESOURCE_EXHAUSTED") ||
    message.includes("Resource exhausted") ||
    message.includes("Too Many Requests") ||
    message.includes("503") ||
    message.includes("UNAVAILABLE")
  );
}

/**
 * Appel Oracle avec retry exponentiel tronqué.
 *
 * @param {string} systemInstruction System instruction déjà construite.
 * @param {string} prompt Prompt final déjà construit.
 * @param {string} model Modèle ciblé.
 * @param {OracleInlineImage[]} images Images inline éventuelles.
 * @return {Promise<string>}
 */
async function generateOracleTextWithRetry(
  systemInstruction: string,
  prompt: string,
  model: string,
  images: OracleInlineImage[]
): Promise<string> {
  let lastError: unknown = null;

  for (let attempt = 0; attempt < ORACLE_MAX_ATTEMPTS; attempt++) {
    try {
      const text = await callOracleGenerateContent(
        systemInstruction,
        prompt,
        model,
        images
      );
      if (!text) {
        throw new Error("L'Oracle n'a renvoyé aucun texte exploitable.");
      }
      return text;
    } catch (error) {
      lastError = error;

      if (!isRetryableOracleError(error) || attempt >= ORACLE_MAX_ATTEMPTS - 1) {
        throw error;
      }

      const jitter = Math.floor(Math.random() * 500);
      const delayMs = Math.min(
        ORACLE_BASE_DELAY_MS * Math.pow(2, attempt) + jitter,
        8000
      );

      console.warn("[Oracle] Retry planifié", {
        attempt: attempt + 1,
        nextDelayMs: delayMs,
        model,
        location: ORACLE_LOCATION,
      });

      await sleep(delayMs);
    }
  }

  throw lastError instanceof Error ? lastError : new Error("Erreur Oracle inconnue");
}

/**
 * Appelle l'endpoint REST officiel Vertex AI generateContent.
 *
 * @param {string} systemInstruction Instruction système.
 * @param {string} prompt Prompt utilisateur.
 * @param {string} model Modèle cible.
 * @param {OracleInlineImage[]} images Images inline éventuelles.
 * @return {Promise<string>}
 */
async function callOracleGenerateContent(
  systemInstruction: string,
  prompt: string,
  model: string,
  images: OracleInlineImage[]
): Promise<string> {
  const accessToken = await getMetadataAccessToken();
  const url = buildOracleGenerateContentUrl(model);
  const body = buildOracleGenerateContentBody(systemInstruction, prompt, images);

  console.info("[Oracle] Appel Vertex REST", {
    model,
    location: ORACLE_LOCATION,
    imageCount: images.length,
  });

  const response = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const rawBody = await response.text();
  const contentType = response.headers.get("content-type") ?? "";

  if (!response.ok) {
    throw buildOracleHttpError(response.status, contentType, rawBody, model);
  }

  if (!contentType.toLowerCase().includes("application/json")) {
    throw new Error(
      [
        "[Oracle] Réponse non JSON reçue depuis Vertex.",
        `model=${model}`,
        `location=${ORACLE_LOCATION}`,
        `status=${response.status}`,
        `contentType=${contentType || "unknown"}`,
        `snippet=${sanitizeSnippet(rawBody)}`,
      ].join(" ")
    );
  }

  let parsed: OracleGenerateContentResponse;
  try {
    parsed = JSON.parse(rawBody) as OracleGenerateContentResponse;
  } catch (error) {
    throw new Error(
      [
        "[Oracle] JSON invalide reçu depuis Vertex.",
        `model=${model}`,
        `location=${ORACLE_LOCATION}`,
        `status=${response.status}`,
        `contentType=${contentType || "unknown"}`,
        `snippet=${sanitizeSnippet(rawBody)}`,
        `parseError=${error instanceof Error ? error.message : String(error)}`,
      ].join(" ")
    );
  }

  const text = extractGeneratedText(parsed);
  if (!text) {
    throw new Error(
      [
        "[Oracle] Réponse JSON sans texte exploitable.",
        `model=${model}`,
        `location=${ORACLE_LOCATION}`,
        `status=${response.status}`,
      ].join(" ")
    );
  }

  return text;
}

/**
 * Construit l'URL REST officielle du modèle Vertex.
 *
 * @param {string} model Modèle Gemini.
 * @return {string}
 */
function buildOracleGenerateContentUrl(model: string): string {
  return [
    "https://aiplatform.googleapis.com/v1/projects",
    PROJECT_ID,
    "locations",
    ORACLE_LOCATION,
    "publishers/google/models",
    model + ":generateContent",
  ].join("/");
}

/**
 * Construit le body REST Oracle.
 *
 * @param {string} systemInstruction Instruction système.
 * @param {string} prompt Prompt utilisateur.
 * @param {OracleInlineImage[]} images Images éventuelles.
 * @return {{systemInstruction: OracleContent, contents: OracleContent[]}}
 */
function buildOracleGenerateContentBody(
  systemInstruction: string,
  prompt: string,
  images: OracleInlineImage[]
): {systemInstruction: OracleContent; contents: OracleContent[]} {
  const parts: OraclePart[] = [{text: prompt}];

  for (const image of images) {
    const mimeType =
      typeof image?.mimeType === "string" && image.mimeType.trim().length > 0 ?
        image.mimeType.trim() :
        "image/jpeg";

    const dataBase64 =
      typeof image?.dataBase64 === "string" ?
        image.dataBase64.trim() :
        "";

    if (!dataBase64) {
      continue;
    }

    parts.push({
      inlineData: {
        mimeType,
        data: dataBase64,
      },
    });
  }

  return {
    systemInstruction: {
      role: "system",
      parts: [{text: systemInstruction}],
    },
    contents: [
      {
        role: "user",
        parts,
      },
    ],
  };
}

/**
 * Récupère un access token ADC depuis le metadata server de la function.
 *
 * @return {Promise<string>}
 */
async function getMetadataAccessToken(): Promise<string> {
  const now = Date.now();

  if (cachedAccessToken && now < cachedAccessToken.expiresAtMs) {
    return cachedAccessToken.token;
  }

  const response = await fetch(METADATA_TOKEN_URL, {
    method: "GET",
    headers: {
      "Metadata-Flavor": "Google",
    },
  });

  const rawBody = await response.text();
  if (!response.ok) {
    throw new Error(
      [
        "[Oracle] Impossible de récupérer le token du service account.",
        `status=${response.status}`,
        `snippet=${sanitizeSnippet(rawBody)}`,
      ].join(" ")
    );
  }

  type MetadataTokenResponse = {
    access_token?: string;
    expires_in?: number;
    token_type?: string;
  };

  let parsed: MetadataTokenResponse;
  try {
    parsed = JSON.parse(rawBody) as MetadataTokenResponse;
  } catch (error) {
    throw new Error(
      [
        "[Oracle] Réponse metadata invalide.",
        `snippet=${sanitizeSnippet(rawBody)}`,
        `parseError=${error instanceof Error ? error.message : String(error)}`,
      ].join(" ")
    );
  }

  const accessToken = typeof parsed.access_token === "string" ?
    parsed.access_token.trim() :
    "";
  const expiresIn = typeof parsed.expires_in === "number" ? parsed.expires_in : 300;

  if (!accessToken) {
    throw new Error("[Oracle] Metadata token vide.");
  }

  cachedAccessToken = {
    token: accessToken,
    expiresAtMs: now + Math.max(60, expiresIn - 60) * 1000,
  };

  return accessToken;
}

/**
 * Construit une erreur HTTP détaillée et stable.
 *
 * @param {number} status Code HTTP.
 * @param {string} contentType Content-Type reçu.
 * @param {string} rawBody Corps brut.
 * @param {string} model Modèle ciblé.
 * @return {Error}
 */
function buildOracleHttpError(
  status: number,
  contentType: string,
  rawBody: string,
  model: string
): Error {
  if (contentType.toLowerCase().includes("application/json")) {
    try {
      const parsed = JSON.parse(rawBody) as OracleGenerateContentResponse;
      const apiMessage = parsed.error?.message ?? "Erreur Vertex sans message.";
      const apiStatus = parsed.error?.status ?? "UNKNOWN";
      return new Error(
        [
          "[Oracle] Erreur HTTP Vertex.",
          `model=${model}`,
          `location=${ORACLE_LOCATION}`,
          `status=${status}`,
          `apiStatus=${apiStatus}`,
          `message=${apiMessage}`,
        ].join(" ")
      );
    } catch {
      // fallback ci-dessous
    }
  }

  return new Error(
    [
      "[Oracle] Erreur HTTP non JSON depuis Vertex.",
      `model=${model}`,
      `location=${ORACLE_LOCATION}`,
      `status=${status}`,
      `contentType=${contentType || "unknown"}`,
      `snippet=${sanitizeSnippet(rawBody)}`,
    ].join(" ")
  );
}

/**
 * Nettoie un extrait de réponse pour les logs.
 *
 * @param {string} value Valeur brute.
 * @return {string}
 */
function sanitizeSnippet(value: string): string {
  return value
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, ORACLE_RESPONSE_SNIPPET_MAX);
}

/**
 * Extrait l'URL audio de la réponse Lyria.
 *
 * @param {unknown} response Réponse brute du modèle.
 * @return {string | null} URL audio si trouvée, sinon null.
 */
function extractAudioUrl(response: unknown): string | null {
  const typedResponse = response as {
    candidates?: Array<{
      content?: {
        parts?: Array<{
          fileData?: {
            fileUri?: string;
          };
        }>;
      };
    }>;
  };

  const fileUri = typedResponse.candidates?.[0]?.content?.parts?.[0]?.fileData?.fileUri;
  return typeof fileUri === "string" && fileUri.length > 0 ? fileUri : null;
}

/**
 * Vérifie le quota quotidien d'un utilisateur.
 *
 * @param {string} userId UID Firebase de l'utilisateur.
 * @return {Promise<number>} Nombre d'utilisations aujourd'hui.
 */
async function checkUserQuota(userId: string): Promise<number> {
  const today = getTodayKey();
  const doc = await admin.firestore()
    .collection("lyria_quotas_users")
    .doc(userId)
    .get();

  if (!doc.exists) {
    return 0;
  }

  const data = doc.data();
  if (data?.date === today) {
    return typeof data.count === "number" ? data.count : 0;
  }

  return 0;
}

/**
 * Incrémente le quota quotidien d'un utilisateur.
 *
 * @param {string} userId UID Firebase de l'utilisateur.
 * @return {Promise<void>}
 */
async function incrementUserQuota(userId: string): Promise<void> {
  const today = getTodayKey();
  const ref = admin.firestore()
    .collection("lyria_quotas_users")
    .doc(userId);

  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const currentData = snap.data();

    const nextCount =
      currentData?.date === today && typeof currentData?.count === "number" ?
        currentData.count + 1 :
        1;

    tx.set(ref, {
      date: today,
      count: nextCount,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
  });
}

/**
 * Vérifie le quota global quotidien.
 *
 * @return {Promise<number>} Nombre d'utilisations aujourd'hui.
 */
async function checkGlobalQuota(): Promise<number> {
  const today = getTodayKey();
  const doc = await admin.firestore()
    .collection("lyria_quotas_global")
    .doc(today)
    .get();

  if (!doc.exists) {
    return 0;
  }

  const data = doc.data();
  return typeof data?.count === "number" ? data.count : 0;
}

/**
 * Incrémente le quota global quotidien.
 *
 * @return {Promise<void>}
 */
async function incrementGlobalQuota(): Promise<void> {
  const today = getTodayKey();
  const ref = admin.firestore()
    .collection("lyria_quotas_global")
    .doc(today);

  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const currentData = snap.data();
    const currentCount = typeof currentData?.count === "number" ? currentData.count : 0;

    tx.set(ref, {
      count: currentCount + 1,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
  });
}

/**
 * Retourne la clé journalière YYYY-MM-DD.
 *
 * @return {string}
 */
function getTodayKey(): string {
  return new Date().toISOString().slice(0, 10);
}
