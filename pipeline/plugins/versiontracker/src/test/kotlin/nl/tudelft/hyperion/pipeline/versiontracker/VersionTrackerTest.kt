package nl.tudelft.hyperion.pipeline.versiontracker

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.readJSONContent
import org.eclipse.jgit.api.LsRemoteCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectIdRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionTrackerTest {

    @Test
    fun `process() should add version if available for project`() {
        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(
                "sap" to ProjectConfig(
                    "https://github.com/john/sap.git",
                    "master",
                    authentication = null
                )
            )
        )

        val message = """{"project": "sap", "message":"INFO - Log"}"""
        val commitHash = "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15"

        // add version to project 'sap'
        val plugin = VersionTracker(config)
        plugin.projectVersions["sap"] = commitHash

        var json: Map<String, Any>? = null

        runBlocking {
            val processedMessage = plugin.resolveCommitHash(message)
            json = processedMessage?.let { readJSONContent(it) }
        }

        assertNotNull(json)
        assertEquals(commitHash, json!!["version"])
    }

    @Test
    fun `resolveCommitHash() should return null if not available for project`() {
        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(
                "sap" to ProjectConfig(
                    "https://github.com/john/sap.git",
                    "master",
                    authentication = null
                )
            )
        )

        val message = """{"project": "sap", "message":"INFO - Log"}"""

        // process message
        val plugin = VersionTracker(config)
        val processedMessage = plugin.resolveCommitHash(message)

        assertNull(processedMessage)
    }

    @Test
    fun `resolveCommitHash() should return null if project field does not exist`() {
        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(
                "sap" to ProjectConfig(
                    "https://github.com/john/sap.git",
                    "master",
                    authentication = null
                )
            )
        )

        val message = """{"message":"INFO - Log"}"""

        // process message
        val plugin = VersionTracker(config)
        val processedMessage = plugin.resolveCommitHash(message)

        assertNull(processedMessage)
    }

    @Test
    fun `updateRefs() should add a reference if one exists`() {
        mockkConstructor(LsRemoteCommand::class)

        // construct reference
        val branch = "production"
        val reference = ObjectIdRef.PeeledNonTag(mockk(), branch, ObjectId(1, 1, 1, 1, 1))
        val commitHash = reference.objectId.name

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf(branch to reference)

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            branch,
            authentication = null
        )

        val projectName = "sap"

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(projectName to projectConfig)
        )

        val plugin = VersionTracker(config)

        // project versions must be empty to begin
        assertTrue(plugin.projectVersions.isEmpty())

        plugin.updateRefs(projectName, projectConfig)

        // assert that the hash is added to the plugin's .projectVersions map
        assertEquals(commitHash, plugin.projectVersions["sap"])

        unmockkAll()
    }

    @Test
    fun `updateRefs() should not add any reference if the branch does not exist`() {
        mockkConstructor(LsRemoteCommand::class)

        // construct reference
        val branch = "production"
        val reference = ObjectIdRef.PeeledNonTag(mockk(), branch, ObjectId(1, 1, 1, 1, 1))

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf("unknown branch" to reference)

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            branch,
            authentication = null
        )

        val projectName = "sap"

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(projectName to projectConfig)
        )

        val plugin = VersionTracker(config)

        plugin.updateRefs(projectName, projectConfig)

        // project versions should be empty
        assertTrue(plugin.projectVersions.isEmpty())

        unmockkAll()
    }

    @Test
    fun `updateRefs() should not add any reference if the branch exists but is null`() {
        mockkConstructor(LsRemoteCommand::class)

        // construct reference
        val branch = "production"

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf(branch to null)

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            branch,
            authentication = null
        )

        val projectName = "sap"

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(projectName to projectConfig)
        )

        val plugin = VersionTracker(config)

        plugin.updateRefs(projectName, projectConfig)

        // project versions should be empty
        assertTrue(plugin.projectVersions.isEmpty())

        unmockkAll()
    }

    @Test
    fun `VersionTracker should update refs`() = runBlocking {
        mockkConstructor(LsRemoteCommand::class)

        // construct initial reference
        val branch = "production"
        val reference = ObjectIdRef.PeeledNonTag(mockk(), branch, ObjectId(1, 1, 1, 1, 1))
        val commitHash = reference.objectId.name

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf(branch to reference)

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            branch,
            authentication = null,
            updateInterval = 1
        )

        val projectName = "sap"

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf(projectName to projectConfig)
        )

        val plugin = VersionTracker(config)

        // project versions must be empty to begin
        assertTrue(plugin.projectVersions.isEmpty())

        delay(100)

        // assert that the hash is added to the plugin's .projectVersions map
        assertEquals(commitHash, plugin.projectVersions["sap"])

        // create new reference
        val newReference = ObjectIdRef.PeeledNonTag(mockk(), branch, ObjectId(1, 1, 1, 1, 1))
        val newCommitHash = newReference.objectId.name

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf(branch to newReference)

        delay(3000)

        // assert that the hash is updated
        assertEquals(newCommitHash, plugin.projectVersions["sap"])

        unmockkAll()
    }

    @Test
    fun `VersionTracker should add HTTPS authentication if it is given`() {
        mockkConstructor(LsRemoteCommand::class)

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf()

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            "production",
            authentication = Authentication.HTTPS("john", "password")
        )

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf("sap" to projectConfig)
        )

        VersionTracker(config)

        verify {
            anyConstructed<LsRemoteCommand>().setCredentialsProvider(any())
        }

        unmockkAll()
    }

    @Test
    fun `VersionTracker should add SSH authentication if it is given`() {
        mockkConstructor(LsRemoteCommand::class)

        every {
            anyConstructed<LsRemoteCommand>().callAsMap()
        } returns mapOf()

        // construct plugin
        val projectConfig = ProjectConfig(
            "https://github.com/john/sap.git",
            "production",
            authentication = Authentication.SSH("./id_rsa")
        )

        val config = Configuration(
            PipelinePluginConfiguration("VersionTracker", "localhost:5555"),
            mapOf("sap" to projectConfig)
        )

        VersionTracker(config)

        verify {
            anyConstructed<LsRemoteCommand>().setTransportConfigCallback(any())
        }

        unmockkAll()
    }
}