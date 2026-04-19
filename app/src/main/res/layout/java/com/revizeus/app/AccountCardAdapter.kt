package com.revizeus.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class AccountCardAdapter(
    private val context: Context,
    private val accounts: List<AccountRegistry.AccountCache>,
    private val activeUid: String,
    private val onAccountSelected: (AccountRegistry.AccountCache) -> Unit
) : RecyclerView.Adapter<AccountCardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivCardAvatar)
        val tvPseudo: TextView = itemView.findViewById(R.id.tvCardPseudo)
        val tvLevel: TextView = itemView.findViewById(R.id.tvCardLevel)
        val tvEmail: TextView = itemView.findViewById(R.id.tvCardEmail)
        val tvPlayTime: TextView = itemView.findViewById(R.id.tvCardPlayTime)
        val tvSavoirs: TextView = itemView.findViewById(R.id.tvCardSavoirs)
        val tvEclats: TextView = itemView.findViewById(R.id.tvCardEclats)
        val tvAmbroisie: TextView = itemView.findViewById(R.id.tvCardAmbroisie)
        val tvFragmentZeus: TextView = itemView.findViewById(R.id.tvFragmentZeus)
        val tvFragmentAthena: TextView = itemView.findViewById(R.id.tvFragmentAthena)
        val tvFragmentPoseidon: TextView = itemView.findViewById(R.id.tvFragmentPoseidon)
        val tvFragmentAres: TextView = itemView.findViewById(R.id.tvFragmentAres)
        val tvFragmentAphrodite: TextView = itemView.findViewById(R.id.tvFragmentAphrodite)
        val tvFragmentHermes: TextView = itemView.findViewById(R.id.tvFragmentHermes)
        val tvFragmentDemeter: TextView = itemView.findViewById(R.id.tvFragmentDemeter)
        val tvFragmentHephaistos: TextView = itemView.findViewById(R.id.tvFragmentHephaistos)
        val tvFragmentApollon: TextView = itemView.findViewById(R.id.tvFragmentApollon)
        val tvFragmentPromethee: TextView = itemView.findViewById(R.id.tvFragmentPromethee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardViewHolder(LayoutInflater.from(context).inflate(R.layout.item_account_card, parent, false))
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val account = accounts[position]

        val avatarResId = context.resources.getIdentifier(account.avatarResName, "drawable", context.packageName)
        holder.ivAvatar.setImageResource(if (avatarResId != 0) avatarResId else android.R.drawable.sym_def_app_icon)

        holder.tvPseudo.text = account.pseudo.ifBlank { "Héros" }
        holder.tvLevel.text = "LVL ${account.level}"

        val rememberedEmail = try {
            AccountRegistry.getRememberedAccountEmail(context, account.uid).trim()
        } catch (_: Exception) { "" }
        holder.tvEmail.text = if (rememberedEmail.isNotBlank()) rememberedEmail else "UID : ${account.uid}"

        holder.tvPlayTime.text = "⏱ ${AccountRegistry.formatPlayTime(account.totalPlayTimeSeconds)}"
        holder.tvSavoirs.text = "📚 ${account.totalCoursScanned} savoir" + if (account.totalCoursScanned > 1) "s" else ""
        holder.tvEclats.text = "${account.eclatsSavoir} Éclats de savoir"
        holder.tvAmbroisie.text = "${account.ambroisie} Ambroisie"

        val fragments = parseFragmentMap(account.fragmentDetailsJson)
        holder.tvFragmentZeus.text = fragments["zeus"].toString()
        holder.tvFragmentAthena.text = fragments["athena"].toString()
        holder.tvFragmentPoseidon.text = fragments["poseidon"].toString()
        holder.tvFragmentAres.text = fragments["ares"].toString()
        holder.tvFragmentAphrodite.text = fragments["aphrodite"].toString()
        holder.tvFragmentHermes.text = fragments["hermes"].toString()
        holder.tvFragmentDemeter.text = fragments["demeter"].toString()
        holder.tvFragmentHephaistos.text = fragments["hephaistos"].toString()
        holder.tvFragmentApollon.text = fragments["apollon"].toString()
        holder.tvFragmentPromethee.text = fragments["promethee"].toString()

        holder.itemView.setOnClickListener { onAccountSelected(account) }
    }

    override fun getItemCount(): Int = accounts.size

    private fun parseFragmentMap(raw: String): Map<String, Int> {
        val base = linkedMapOf(
            "zeus" to 0, "athena" to 0, "poseidon" to 0, "ares" to 0, "aphrodite" to 0,
            "hermes" to 0, "demeter" to 0, "hephaistos" to 0, "apollon" to 0, "promethee" to 0
        )
        return try {
            if (raw.isBlank()) return base
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val mapped = mapFragmentKeyToGod(key)
                base[mapped] = (base[mapped] ?: 0) + json.optInt(key, 0).coerceAtLeast(0)
            }
            base
        } catch (_: Exception) { base }
    }

    private fun mapFragmentKeyToGod(rawKey: String): String {
        val key = rawKey.trim().lowercase()
        return when {
            "zeus" in key || "math" in key || "mathem" in key -> "zeus"
            "athena" in key || "fran" in key || "litter" in key || "gramma" in key -> "athena"
            "poseidon" in key || "svt" in key || "bio" in key || "vivant" in key -> "poseidon"
            "ares" in key || "hist" in key -> "ares"
            "aphrodite" in key || "art" in key || "musique" in key -> "aphrodite"
            "hermes" in key || "langue" in key || "anglais" in key || "espagnol" in key || "allemand" in key -> "hermes"
            "demeter" in key || "geo" in key || "géographie" in key -> "demeter"
            "hephaistos" in key || "phys" in key || "chim" in key -> "hephaistos"
            "apollon" in key || "philo" in key || "ses" in key -> "apollon"
            "promethee" in key || "projet" in key || "vie" in key -> "promethee"
            else -> "zeus"
        }
    }
}
