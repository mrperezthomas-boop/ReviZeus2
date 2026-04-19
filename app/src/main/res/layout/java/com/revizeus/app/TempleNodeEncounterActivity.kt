package com.revizeus.app

import android.content.Intent
import android.os.Bundle
import com.revizeus.app.databinding.ActivityTempleNodeEncounterBinding

class TempleNodeEncounterActivity : BaseAdventureActivity() {

    companion object {
        const val EXTRA_NODE_ID = "extra_node_id"
        const val EXTRA_NODE_TITLE = "extra_node_title"
        const val EXTRA_NODE_TYPE = "extra_node_type"
        const val EXTRA_NODE_DESCRIPTION = "extra_node_description"
        const val EXTRA_GOD_ID = "extra_god_id"
        const val EXTRA_RESOLUTION_MESSAGE = "extra_resolution_message"
        const val EXTRA_CAN_VALIDATE = "extra_can_validate"

        const val RESULT_NODE_ID = "result_node_id"
        const val RESULT_VALIDATED = "result_validated"
    }

    private lateinit var binding: ActivityTempleNodeEncounterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTempleNodeEncounterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nodeId = intent.getStringExtra(EXTRA_NODE_ID).orEmpty()
        val nodeTitle = intent.getStringExtra(EXTRA_NODE_TITLE).orEmpty().ifBlank { "Rencontre" }
        val nodeType = intent.getStringExtra(EXTRA_NODE_TYPE).orEmpty().ifBlank { "NODE" }
        val nodeDescription = intent.getStringExtra(EXTRA_NODE_DESCRIPTION).orEmpty().ifBlank { "Aucune description." }
        val godId = intent.getStringExtra(EXTRA_GOD_ID).orEmpty().ifBlank { "prometheus" }
        val resolutionMessage = intent.getStringExtra(EXTRA_RESOLUTION_MESSAGE).orEmpty().ifBlank { "Cette rencontre est prête." }
        val canValidate = intent.getBooleanExtra(EXTRA_CAN_VALIDATE, false)

        binding.tvEncounterTitle.text = nodeTitle
        binding.tvEncounterType.text = nodeType.replace('_', ' ')
        binding.tvEncounterDescription.text = nodeDescription
        binding.tvEncounterGod.text = "Guide divin : ${godId.replaceFirstChar { it.uppercaseChar() }}"
        binding.tvEncounterMessage.text = resolutionMessage

        binding.btnEncounterBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnEncounterValidate.text = if (canValidate) "Valider la rencontre" else "Retour à la carte"
        binding.btnEncounterValidate.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(RESULT_NODE_ID, nodeId)
                putExtra(RESULT_VALIDATED, canValidate)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
