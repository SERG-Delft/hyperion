package nl.tudelft.hyperion.pipeline.versiontracker

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import org.eclipse.jgit.api.LsRemoteCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GitUtilsTest {
    @Test
    fun `lsRemoteCommandBuilder() should add the correct repository`() {
        mockkConstructor(LsRemoteCommand::class)

        val slot = slot<String>()

        every {
            anyConstructed<LsRemoteCommand>().setRemote(capture(slot))
        } returns mockk(relaxed = true)

        every {
            anyConstructed<LsRemoteCommand>().setHeads(any())
        } returns mockk(relaxed = true)

        val remote = "git@github.com:john/project.git"
        lsRemoteCommandBuilder(remote)

        assertEquals(remote, slot.captured)

        unmockkAll()
    }
}