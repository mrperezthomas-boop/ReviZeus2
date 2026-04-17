# AGENTS.md — RÈGLES PROJET RÉVIZEUS

Tu travailles sur **RéviZeus**, application Android éducative RPG mythologique.

## 1. Vérité projet
En cas de contradiction, appliquer cet ordre :
1. **Code réel du repo**
2. **Ressources réellement présentes**
3. **Blocs préparatoires**
4. **IA_DOCS anciens**

Ne jamais inventer une vérité projet qui n’est pas confirmée par le code actuel.

## 2. Stack obligatoire
- Kotlin
- XML classique
- ViewBinding obligatoire
- Jetpack Compose interdit
- Min SDK 24
- Target SDK 36
- Compile SDK 36
- Toolchain JVM 17
- Room via KSP
- Firebase Auth / Firestore
- Gemini `0.9.0`
- CameraX `1.5.3`
- ML Kit text recognition `19.0.1`
- `lifecycle-runtime-ktx:2.8.7`
- Media3 / ExoPlayer
- Lottie

## 3. Règles absolues de modification
- Ne jamais renommer mes variables existantes.
- Ne jamais casser l’architecture existante.
- Ne jamais supprimer une mécanique existante sans demande explicite.
- Ne jamais modifier des IDs sensibles sans nécessité absolue.
- Conserver 100 % des commentaires, mécaniques et structures utiles déjà en place.
- Insérer uniquement les correctifs nécessaires.
- Générer les **fichiers complets modifiés** quand une modification est demandée.
- Pas de placeholders parasites.
- Pas de pseudocode.
- Pas de refactor global non demandé.
- Toujours commenter intelligemment les ajouts utiles dans le code généré.

## 4. Règles UI / orientation
- Tout RéviZeus est en **portrait fullscreen**.
- Exception : **tout le mode aventure est en paysage fullscreen**.
- Ne pas propager le paysage hors du mode aventure.
- Réutiliser ViewBinding et les layouts XML existants.

## 5. Règles dialogues / feedback
- Toute narration importante doit rester cohérente avec l’univers RPG RéviZeus.
- Les dialogues importants doivent tendre vers le standard RPG du projet.
- Ne pas remplacer un système RPG existant par un AlertDialog banal ou un Toast important.
- Les attentes IA doivent utiliser le système de loading divin existant si le flux s’y prête.

## 6. Règles audio / animation
- Respecter `SoundManager` comme système audio central.
- Éviter les lecteurs isolés si le projet centralise déjà le son.
- Respecter la logique lifecycle (`onPause`, `onResume`, `onDestroy`) pour audio, typewriter, timers, animations et vidéos.

## 7. Base de données / données
- Respecter Room existant.
- Préserver les accès DAO existants.
- Éviter toute migration destructrice improvisée.
- Toute logique lourde IO doit rester hors UI thread.

## 8. Ressources
- Ne jamais inventer une ressource comme si elle existait.
- Si une ressource manque, le dire explicitement.
- Pour toute ressource nouvelle réellement nécessaire, fournir :
  - nom de ressource
  - utilité
  - connexions avec les autres écrans / codes
  - prompt JSON de génération si demandé
- Réutiliser les ressources déjà présentes avant de proposer autre chose.

## 9. Méthode obligatoire
Avant de coder :
1. auditer
2. lister les fichiers concernés
3. lister les risques
4. lister les ressources manquantes
5. proposer le plan minimal
6. seulement ensuite coder

## 10. Format de réponse attendu
Quand tu réponds à une mission de code :
- commence par l’audit bref
- liste les fichiers touchés
- liste les éventuelles ressources manquantes
- puis fournis les fichiers complets modifiés
- termine par les points de vérification Android Studio

## 11. État métier important
- Le mode aventure existe déjà mais seulement au début / socle.
- Le Bloc A est déjà fait.
- La majorité des autres blocs restent à faire.
- Toute nouvelle implémentation doit rester compatible avec la base actuelle, sans l’écraser.
