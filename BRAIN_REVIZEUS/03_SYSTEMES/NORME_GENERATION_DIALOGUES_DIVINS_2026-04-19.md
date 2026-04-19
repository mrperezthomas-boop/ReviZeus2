<!-- Horodatage logique : 2026-04-19 | Objet : norme de génération des dialogues divins -->
# NORME_GÉNÉRATION_DIALOGUES_DIVINS_2026-04-19

**Date :** 2026-04-19  
**Owner logique :** `brain_core`  
**Statut :** vérité exécutable pour prompts / agents / Cursor / IA de génération

---

## 1. But

Empêcher toute dérive entre :
- la **matière scolaire canonique** d'un dieu,
- sa **personnalité dialoguée**,
- sa **direction artistique**,
- et son **usage produit** dans RéviZeus.

Cette norme impose une séparation stricte :

1. **Matière** = vérité fixe, non négociable.
2. **Personnalité** = couleur du dialogue.
3. **DA** = cadrage visuel et symbolique.
4. **Dialogue généré** = doit refléter la personnalité **sans jamais modifier la matière**.

---

## 2. Règle sacrée

**Interdiction absolue :** un prompt n'a pas le droit de déduire la matière d'un dieu à partir de son ton, de son lore, de sa symbolique ou de son attribut visuel.

La matière est lue depuis la table canonique uniquement.

---

## 3. Table canonique verrouillée

| Dieu | Matière canonique | Couleur | Hex | Rôle synthétique |
|---|---|---|---|---|
| Zeus | Mathématiques | Bleu électrique | `#1E90FF` | logique, preuve, ordre |
| Athéna | Français | Or / Blanc | `#FFD700` | langage, structure, sagesse |
| Poséidon | SVT | Turquoise | `#40E0D0` | vivant, nature, profondeur |
| Arès | Histoire | Rouge doré | `#DAA520` | mémoire des conflits, courage |
| Déméter | Géographie | Vert forêt | `#228B22` | terre, territoires, climat |
| Aphrodite | Art / Musique | Rose lumineux | `#FF69B4` | sensibilité, création, beauté |
| Héphaïstos | Physique-Chimie | Orange flamme | `#FF8C00` | matière, énergie, forge |
| Apollon | Philo / SES | Violet clair | `#DDA0DD` | réflexion, lumière, élévation |
| Hermès | Langues / Anglais | Bleu ciel | `#87CEEB` | communication, vitesse, passage |
| Prométhée | Vie & Projets | Ambre doré | `#FFBF00` | feu intérieur, initiative, relance |

---

## 4. Règle de génération dialoguée

Quand une IA génère un texte pour un dieu, elle doit construire la réponse dans cet ordre :

1. Identifier le dieu demandé.
2. Lire sa **matière canonique fixe**.
3. Lire son **profil de personnalité**.
4. Lire ses **marqueurs DA / symboliques**.
5. Générer un texte cohérent avec la situation.
6. Vérifier qu'aucun mot généré ne requalifie la matière.

Exemple correct :
- Apollon peut parler avec élévation, clarté, nuance, profondeur.
- Apollon **ne devient pas** pour autant le dieu officiel de la musique si la table canonique dit Philo / SES.

Exemple incorrect :
- Héphaïstos parle comme un forgeron pragmatique.
- Le prompt en conclut : "matière = ingénierie".

---

## 5. Couche produit

Dans RéviZeus, un dieu peut avoir plusieurs couches :
- **couche matière**,
- **couche personnalité**,
- **couche narrative**,
- **couche produit / UX**.

La seule couche qui pilote l'affectation scolaire est la **couche matière**.

---

## 6. Consignes pour prompts Cursor / agents

Tout prompt qui touche à :
- dialogues,
- quiz,
- feedbacks,
- onboarding,
- aides contextuelles,
- messages divins,
- fatigue divine,
- récompenses,
- scènes d'aventure,
- contenus d'Oracle,

doit contenir explicitement cette phrase ou son équivalent strict :

> La matière canonique du dieu est fixe et ne doit jamais être modifiée, réinterprétée ni devinée à partir du ton, du lore ou de la direction artistique. La personnalité n'influence que la manière de parler.

---

## 7. Contrôle qualité obligatoire

Avant validation d'un prompt ou d'un texte généré, vérifier :

- Le dieu est-il le bon ?
- Sa matière reste-t-elle strictement canonique ?
- Le ton reflète-t-il bien sa personnalité ?
- La DA implicite est-elle cohérente ?
- Le texte évite-t-il les dérives de remapping ?
- Le texte reste-t-il premium, RPG, immersif, pédagogique ?

---

## 8. Résolution des cas ambigus

### Cas A — storytelling plus large que la matière
Autorisé. Le storytelling peut être plus vaste.
Mais cela n'autorise aucune modification de la matière officielle.

### Cas B — personnalité évoquant une autre discipline
Autorisé. Le ton peut évoquer autre chose.
Mais cela n'autorise aucune migration de matière.

### Cas C — fonction produit transverse
Autorisé. Un dieu peut intervenir hors de sa matière pour des raisons narratives ou UX.
Mais son affectation canonique ne change pas.

---

## 9. Décision exécutable

À partir du 2026-04-19, toute IA branchée sur le Brain RéviZeus doit considérer cette norme comme supérieure à toute formulation plus ancienne ou plus floue sur les dieux.
