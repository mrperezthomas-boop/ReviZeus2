# CURSOR_SKILLS.md — PROMPTS RÉFÉRENCE POUR CURSOR / IA LOCALE
# Dernière mise à jour : 2026-04-17
# BUT : Claude utilise ces templates pour produire des prompts précis.
# Toto peut aussi les utiliser directement dans Cursor si besoin.

---

## SKILL 1 — DÉMARRAGE DE MISSION (AUDIT D'ABORD)

```text
Tu travailles sur RéviZeus (Android Kotlin XML ViewBinding).
Lis AGENTS.md à la racine du repo avant tout.

Mission : [DESCRIPTION]

Avant tout code, donne-moi UNIQUEMENT :
1. Fichiers exacts concernés (chemins complets)
2. Classes/managers/layouts impliqués
3. Risques de régression
4. Ressources manquantes éventuelles
5. Plan minimal d'implémentation

Ne modifie AUCUN fichier. Audit seulement.
```

---

## SKILL 2 — IMPLÉMENTATION CIBLÉE

```text
Mission RéviZeus : [NOM COURT]

Objectif : [CE QUE JE VEUX]

Fichier(s) à modifier :
- [fichier1.kt]
- [fichier2.xml]

Fichier(s) interdits :
- [ne touche pas à X]

Contraintes :
- Kotlin XML ViewBinding, pas de Compose
- Ne renomme rien d'existant
- Fichiers COMPLETS modifiés
- N'invente aucune ressource
- Commente les ajouts

Résultat attendu :
- [comportement 1]
- [comportement 2]

Livre : audit bref → fichiers complets → vérifications Android Studio.
```

---

## SKILL 3 — CORRECTION BUG / BUILD / CRASH

```text
Corrige ce problème RéviZeus avec le patch MINIMAL.

Erreur :
[COLLER L'ERREUR OU LE LOGCAT]

Contraintes :
- Correctif minimal uniquement
- Ne touche rien d'étranger au bug
- Ne renomme rien
- Fichiers complets concernés uniquement

Donne :
1. Cause racine probable
2. Fichier(s) responsable(s)
3. Correctif minimal
4. Vérifications Android Studio
```

---

## SKILL 4 — RECADRAGE (SI L'IA DÉRIVE)

```text
Stop. Tu dérives.

Rappels :
- Ne renomme rien d'existant
- Ne casse pas l'architecture
- Pas de refactor global
- Pas de fichiers hors périmètre
- Pas de ressources inventées
- Fichiers complets uniquement

Repars :
1. Diagnostic corrigé
2. Périmètre réel (fichiers exacts)
3. Plan minimal

Ne code pas encore.
```

---

## SKILL 5 — CONTRÔLE QUALITÉ FINAL

```text
Auto-vérifie ta solution :

1. Variables existantes renommées ? → NON
2. Architecture cassée ? → NON
3. Fichiers hors périmètre touchés ? → NON
4. Placeholders parasites ? → NON
5. Ressources inventées ? → NON
6. ViewBinding/XML/imports cohérents ? → OUI
7. Lifecycle respecté (audio/timer/coroutine) ? → OUI
8. Orientation correcte ? → OUI
9. Fichiers complets fournis ? → OUI

Donne : points OK, points douteux, tests Android Studio à faire.
```

---

## CHECKLIST ANDROID STUDIO (après chaque patch)
- [ ] Build → Rebuild Project
- [ ] Sync Gradle si build.gradle touché
- [ ] Imports rouges → corriger
- [ ] IDs XML/ViewBinding → vérifier
- [ ] Logcat au lancement → vérifier
- [ ] Navigation réelle → tester
- [ ] onPause/onResume → vérifier si audio/timer/animation touché
