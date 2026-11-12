package com.pockettavern.app.data.remote.imagegen

import com.pockettavern.app.domain.model.ImageGenBackendType
import kotlinx.serialization.json.*

/** Rejects incompatible request templates before they overwrite a saved workflow. */
object WorkflowImportValidator {
    private val json = Json { isLenient = false }

    fun validate(template: String, backend: ImageGenBackendType): String? {
        if (template.isBlank()) return "The selected file is empty."
        val root = try {
            json.parseToJsonElement(template)
        } catch (_: Exception) {
            return "Invalid JSON. Export a JSON API workflow or request template."
        }
        if (root !is JsonObject || root.isEmpty()) return "A workflow must be a non-empty JSON object."

        val nodes = root.values.filterIsInstance<JsonObject>()
        val comfyNodes = nodes.filter { it["class_type"] is JsonPrimitive && it["inputs"] is JsonObject }
        val isComfy = comfyNodes.isNotEmpty()
        val looksLikeComfyEditor = root["nodes"] is JsonArray && root["links"] is JsonArray

        return when (backend) {
            ImageGenBackendType.COMFYUI -> {
                when {
                    looksLikeComfyEditor -> "This is a ComfyUI editor workflow. Export in API format instead."
                    !isComfy || comfyNodes.size != root.size ->
                        "ComfyUI requires an API-format workflow with node IDs, class_type and inputs."
                    comfyNodes.none { it["class_type"]?.jsonPrimitive?.contentOrNull == "KSampler" } ->
                        "This PocketTavern build requires a ComfyUI KSampler node."
                    else -> null
                }
            }
            ImageGenBackendType.SD_WEBUI -> {
                when {
                    isComfy || looksLikeComfyEditor -> "This is a ComfyUI workflow, not an SD WebUI / Forge request template."
                    root["prompt"] == null -> "Forge templates require a prompt field (for example, \"{{prompt}}\")."
                    else -> null
                }
            }
            else -> {
                if (isComfy || looksLikeComfyEditor)
                    "ComfyUI node workflows cannot be used with ${backend.displayName}. Select ComfyUI instead."
                else null
            }
        }
    }
}
