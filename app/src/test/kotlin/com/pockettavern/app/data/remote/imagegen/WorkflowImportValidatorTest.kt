package com.pockettavern.app.data.remote.imagegen

import com.pockettavern.app.domain.model.ImageGenBackendType
import org.junit.Assert.*
import org.junit.Test

class WorkflowImportValidatorTest {
    private val comfy = """{"100":{"class_type":"KSampler","inputs":{"seed":1}}}"""
    private val forge = """{"prompt":"{{prompt}}","steps":20}"""

    @Test fun acceptsComfyApiWorkflow() {
        assertNull(WorkflowImportValidator.validate(comfy, ImageGenBackendType.COMFYUI))
    }

    @Test fun rejectsComfyWorkflowUnderForge() {
        assertNotNull(WorkflowImportValidator.validate(comfy, ImageGenBackendType.SD_WEBUI))
    }

    @Test fun rejectsForgeTemplateUnderComfy() {
        assertNotNull(WorkflowImportValidator.validate(forge, ImageGenBackendType.COMFYUI))
    }

    @Test fun acceptsForgeTemplate() {
        assertNull(WorkflowImportValidator.validate(forge, ImageGenBackendType.SD_WEBUI))
    }

    @Test fun rejectsEmptyAndMalformedJson() {
        for (value in listOf("", "{}", "{bad json", "[]")) {
            assertNotNull(WorkflowImportValidator.validate(value, ImageGenBackendType.COMFYUI))
        }
    }

    @Test fun rejectsComfyEditorExport() {
        val editor = """{"nodes":[],"links":[]}"""
        assertTrue(WorkflowImportValidator.validate(editor, ImageGenBackendType.COMFYUI)!!.contains("API format"))
    }

    @Test fun rejectsComfyWorkflowWithoutKSampler() {
        val noSampler = """{"100":{"class_type":"CheckpointLoaderSimple","inputs":{}}}"""
        assertNotNull(WorkflowImportValidator.validate(noSampler, ImageGenBackendType.COMFYUI))
    }
}
