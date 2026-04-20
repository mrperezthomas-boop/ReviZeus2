import * as functions from "firebase-functions";
import {VertexAI} from "@google-cloud/vertexai";
import * as admin from "firebase-admin";

admin.initializeApp();

// Configuration Vertex AI
const PROJECT_ID = "revizeus";
const LOCATION = "us-central1";
const ORACLE_LOCATION = "europe-west1";
const FUNCTION_SERVICE_ACCOUNT =
  "revizeus-functions-sa@revizeus.iam.gserviceaccount.com";

// Initialisation Vertex AI
const vertexAI = new VertexAI({
  project: PROJECT_ID,
  location: LOCATION,
});

/**
 * [2026-04-20][TRANSPORT_ORACLE_TEXTE]
 * Instance dédiée au flux Oracle texte.
 * On la place explicitement en europe-west1 selon la demande,
 * sans migrer la musique existante pour éviter une casse latérale.
 */
const oracleVertexAI = new VertexAI({
  project: PROJECT_ID,
  location: ORACLE_LOCATION,
});

// Limites quotidiennes par utilisateur
// MODE TEST TEMPORAIRE : désactive les quotas pour pouvoir valider le flow
// musical sans attendre le reset quotidien.
const DISABLE_QUOTA = true;
const DAILY_QUOTA_PER_USER = 5;
const DAILY_QUOTA_GLOBAL = 100;
const TEST_REMAINING_QUOTA = 9999;

/**
 * [2026-04-20][TRANSPORT_ORACLE_TEXTE]
 * Cloud Function callable pour le flux Oracle texte.
 *
 * Contrat :
 * - Auth obligatoire
 * - Reçoit systemInstruction + prompt déjà construits par GeminiManager
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

    const systemInstruction =
      typeof data?.systemInstruction === "string" ?
        data.systemInstruction.trim() :
        "";
    const prompt =
      typeof data?.prompt === "string" ?
        data.prompt.trim() :
        "";
    const model =
      typeof data?.model === "string" && data.model.trim().length > 0 ?
        data.model.trim() :
        "gemini-2.5-flash";

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
      const generativeModel = oracleVertexAI.getGenerativeModel({
        model,
        systemInstruction: {
          role: "system",
          parts: [{text: systemInstruction}],
        },
      });

      const result = await generativeModel.generateContent({
        contents: [
          {
            role: "user",
            parts: [{text: prompt}],
          },
        ],
      });

      const text = extractGeneratedText(result.response);

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
  const docRef = admin.firestore()
    .collection("lyria_quotas_users")
    .doc(userId);

  await admin.firestore().runTransaction(async (transaction) => {
    const doc = await transaction.get(docRef);

    if (!doc.exists || doc.data()?.date !== today) {
      transaction.set(docRef, {date: today, count: 1});
    } else {
      transaction.update(docRef, {
        count: admin.firestore.FieldValue.increment(1),
      });
    }
  });
}

/**
 * Vérifie le quota quotidien global.
 *
 * @return {Promise<number>} Nombre total d'utilisations aujourd'hui.
 */
async function checkGlobalQuota(): Promise<number> {
  const today = getTodayKey();
  const doc = await admin.firestore()
    .collection("lyria_quotas_global")
    .doc("daily")
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
 * Incrémente le quota quotidien global.
 *
 * @return {Promise<void>}
 */
async function incrementGlobalQuota(): Promise<void> {
  const today = getTodayKey();
  const docRef = admin.firestore()
    .collection("lyria_quotas_global")
    .doc("daily");

  await admin.firestore().runTransaction(async (transaction) => {
    const doc = await transaction.get(docRef);

    if (!doc.exists || doc.data()?.date !== today) {
      transaction.set(docRef, {date: today, count: 1});
    } else {
      transaction.update(docRef, {
        count: admin.firestore.FieldValue.increment(1),
      });
    }
  });
}

/**
 * Retourne une clé unique pour la date du jour au format YYYY-MM-DD.
 *
 * @return {string} Clé de date du jour.
 */
function getTodayKey(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}