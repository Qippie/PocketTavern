package com.pockettavern.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Optional per-character settings, independently stored for every backend.
 * GLOBAL means inherit the backend's existing global workflow mode.
 */
@Serializable
data class CharacterImageWorkflowSettings(
    val workflowModes: Map<String, String> = emptyMap(),
    val customWorkflows: Map<String, CustomImageWorkflowDefinition> = emptyMap()
) {
    fun modeFor(backend: ImageGenBackendType): String = workflowModes[backend.name] ?: "GLOBAL"

    fun templateFor(backend: ImageGenBackendType): CustomImageWorkflowDefinition? =
        customWorkflows[backend.name]
}

/** Backed up automatically because the JSON file lives inside filesDir/extensions. */
@Serializable
data class CharacterImageWorkflowFile(
    val characters: Map<String, CharacterImageWorkflowSettings> = emptyMap()
)

/**
 * Central resolver used for every image request carrying a character file identity.
 * A character's explicit STANDARD/CUSTOM choice wins over an extension request.
 * Custom without a character template inherits the backend's global custom template.
 */
object CharacterImageWorkflowResolver {
    fun resolve(
        config: ImageGenConfig,
        request: ForgeGenerationParams,
        character: CharacterImageWorkflowSettings?
    ): ForgeGenerationParams {
        val mode = character?.modeFor(config.activeBackendType) ?: "GLOBAL"
        return when (mode) {
            "GLOBAL" -> request
            "STANDARD" -> request.copy(
                workflowModeOverride = "STANDARD", customWorkflowOverride = null,
                preserveCustomWorkflowModel = false
            )
            "CUSTOM" -> {
                val ownTemplate = character?.templateFor(config.activeBackendType)
                    ?.template?.takeIf { it.isNotBlank() }
                request.copy(
                    workflowModeOverride = "CUSTOM",
                    customWorkflowOverride = ownTemplate,
                    preserveCustomWorkflowModel = ownTemplate != null
                )
            }
            else -> throw IllegalArgumentException("Unknown character image workflow mode: $mode")
        }
    }
}
