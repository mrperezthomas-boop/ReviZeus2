from __future__ import annotations

from collections import Counter

from brain_runtime import AgentContext, git


BEHAVIOR_TEMPLATES = {
    "BOOT": "Impact probable sur le flux de lancement et d’accès au jeu.",
    "AUTH": "Impact probable sur les comptes, héros ou l’onboarding.",
    "CORE_LOOP": "Impact probable sur le hub principal et la navigation centrale.",
    "BLOC_A": "Impact probable sur le socle transverse et la stabilité technique.",
    "BLOC_B": "Impact probable sur les dialogues RPG immersifs.",
    "BLOC_B2": "Impact probable sur les personas divines et l’orchestration narrative.",
    "C_AUDIO": "Impact probable sur l’audio, la TTS ou le loading diégétique.",
    "D_ORACLE": "Impact probable sur le pipeline Oracle et la génération IA.",
    "E_QUIZ": "Impact probable sur le moteur quiz ou l’entraînement.",
    "F_ECONOMIE": "Impact probable sur l’économie, l’XP ou les récompenses.",
    "G_FORGE": "Impact probable sur la forge, le crafting ou l’inventaire.",
    "H_AVENTURE": "Impact probable sur le mode aventure, les maps ou la progression.",
    "I_SAVOIRS": "Impact probable sur les savoirs, temples matière ou la bibliothèque.",
    "K_ADAPTATIVE": "Impact probable sur l’IA adaptative joueur/héros.",
    "L_ANALYTICS": "Impact probable sur l’analytics pédagogique ou le debug.",
    "M_RECO": "Impact probable sur les recommandations et synthèses.",
    "O_META": "Impact probable sur les badges ou la méta-progression.",
    "DATA_ROOM": "Impact probable sur la persistance Room et les entités.",
    "CORE_PANTHEON": "Impact probable sur le mapping panthéon / matières.",
}


def analyze_changes(ctx: AgentContext, changed_files: list[str], mapping: dict) -> dict:
    diff_stat = git(["diff", "--stat", "HEAD~1", "HEAD"], ctx.project_root)
    mapped_files = mapping.get("mapped_files", [])
    bloc_counter = Counter(row["bloc"] for row in mapped_files)
    owner_counter = Counter(row.get("owner", "unknown") for row in mapped_files)
    unknown_files = [row["file"] for row in mapped_files if row["bloc"] == "UNKNOWN"]

    dominant_bloc = bloc_counter.most_common(1)[0][0] if bloc_counter else "UNKNOWN"
    dominant_owner = owner_counter.most_common(1)[0][0] if owner_counter else "unknown"

    key_behaviors = []
    for bloc, _count in bloc_counter.most_common(5):
        template = BEHAVIOR_TEMPLATES.get(bloc)
        if template:
            key_behaviors.append(template)

    if unknown_files:
        key_behaviors.append(f"{len(unknown_files)} fichier(s) restent non cartographié(s) et demandent une règle explicite si critiques.")

    if not key_behaviors and changed_files:
        key_behaviors.append("Modifications détectées mais impact métier non déterminé automatiquement.")

    confidence = "HIGH"
    if dominant_bloc == "UNKNOWN":
        confidence = "LOW"
    elif unknown_files:
        confidence = "MEDIUM"

    return {
        "diff_stat": diff_stat,
        "dominant_bloc": dominant_bloc,
        "dominant_owner": dominant_owner,
        "unknown_files": unknown_files,
        "key_behaviors": key_behaviors,
        "summary": f"{len(changed_files)} fichier(s) surveillé(s) modifié(s), bloc dominant = {dominant_bloc}, owner dominant = {dominant_owner}, confiance = {confidence}.",
        "confidence": confidence,
    }
