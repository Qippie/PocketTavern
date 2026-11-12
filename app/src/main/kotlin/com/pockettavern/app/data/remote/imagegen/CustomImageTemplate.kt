package com.pockettavern.app.data.remote.imagegen

import com.pockettavern.app.domain.model.ForgeGenerationParams
import com.pockettavern.app.domain.model.ImageGenBackendType
import com.pockettavern.app.domain.model.ImageGenConfig
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder

/**
 * Backend-neutral custom image request templating.
 *
 * JSON templates use escaped string substitutions without surrounding quotes, so
 * users can write e.g. { "prompt": "{{prompt}}", "steps": {{steps}} }.
 * URL templates use URL-encoded string substitutions.
 */
object CustomImageTemplate {

    fun selected(
        config: ImageGenConfig,
        params: ForgeGenerationParams,
        backend: ImageGenBackendType
    ): String? {
        val mode = params.workflowModeOverride ?: config.workflowMode
        if (mode != "CUSTOM") return null

        params.customWorkflowOverride?.takeIf { it.isNotBlank() }?.let { return it }

        if (config.customWorkflowTemplate.isBlank()) return null
        if (
            config.customWorkflowBackend.isNotBlank() &&
            config.customWorkflowBackend != backend.name
        ) return null

        return config.customWorkflowTemplate
    }

    fun renderJson(
        template: String,
        params: ForgeGenerationParams,
        model: String = ""
    ): String {
        var out = template
        val stringValues = mapOf(
            "{{prompt}}" to params.prompt,
            "{{negative_prompt}}" to params.negativePrompt,
            "{{sampler}}" to params.sampler,
            "{{model}}" to model
        )
        stringValues.forEach { (token, value) ->
            out = out.replace(token, escapeJsonString(value))
        }

        val numericValues = mapOf(
            "{{seed}}" to params.seed.toString(),
            "{{steps}}" to params.steps.toString(),
            "{{cfg}}" to params.cfgScale.toString(),
            "{{width}}" to params.width.toString(),
            "{{height}}" to params.height.toString(),
            "{{denoise}}" to params.denoisingStrength.toString(),
            "{{clip_skip}}" to params.clipSkip.toString()
        )
        numericValues.forEach { (token, value) ->
            out = out.replace(token, value)
        }
        return out
    }

    fun renderUrl(
        template: String,
        params: ForgeGenerationParams,
        model: String = ""
    ): String {
        var out = template
        val stringValues = mapOf(
            "{{prompt}}" to params.prompt,
            "{{negative_prompt}}" to params.negativePrompt,
            "{{sampler}}" to params.sampler,
            "{{model}}" to model
        )
        stringValues.forEach { (token, value) ->
            out = out.replace(token, URLEncoder.encode(value, "UTF-8"))
        }
        val numericValues = mapOf(
            "{{seed}}" to params.seed.toString(),
            "{{steps}}" to params.steps.toString(),
            "{{cfg}}" to params.cfgScale.toString(),
            "{{width}}" to params.width.toString(),
            "{{height}}" to params.height.toString(),
            "{{denoise}}" to params.denoisingStrength.toString(),
            "{{clip_skip}}" to params.clipSkip.toString()
        )
        numericValues.forEach { (token, value) ->
            out = out.replace(token, value)
        }
        return out
    }

    private fun escapeJsonString(value: String): String {
        val quoted = JsonPrimitive(value).toString()
        return quoted.substring(1, quoted.length - 1)
    }
}
