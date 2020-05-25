package nl.tudelft.hyperion.pipeline

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.connection.PluginManagerConnection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AbstractPipelinePluginTest {
    private val config = PipelinePluginConfiguration("Plugin", "tcp://localhost:5000")
    private lateinit var pmConn: PluginManagerConnection

    @BeforeEach
    fun initSetup() {
        pmConn = mockk<PluginManagerConnection>(relaxed = true)
        every {
            pmConn.requestConfig("Plugin", "pull")
        } returns """{"isBind":"true","host":"tcp://localhost:1200"}"""
        every {
            pmConn.requestConfig("Plugin", "push")
        } returns """{"isBind":"false","host":"tcp://localhost:1201"}"""
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Request configuration from PluginManager`() {
        val plugin = TestPlugin(config, pmConn)
        plugin.queryConnectionInformation()

        verify {
            pmConn.requestConfig("Plugin", "pull")
            pmConn.requestConfig("Plugin", "push")
        }
    }

    @Test
    fun `Request configuration from PluginManager twice`() {
        val plugin = TestPlugin(config, pmConn)
        plugin.queryConnectionInformation()

        assertThrows<PipelinePluginInitializationException> { plugin.queryConnectionInformation() }
    }

    @InternalCoroutinesApi
    @Test
    fun `Throw exception when run without init`() {
        val plugin = TestPlugin(config)
        var msg = "Nope"

        val handler = CoroutineExceptionHandler { _, exception ->
            msg = exception.message!!
        }

        runBlocking {
            GlobalScope.launch(handler) {
                plugin.runSuspend()
            }.join()
        }

        assertEquals("Cannot run plugin without connection information", msg)
    }
}

class TestPlugin : AbstractPipelinePlugin {
    constructor(config: PipelinePluginConfiguration) : super(config)
    constructor(config: PipelinePluginConfiguration, pmConn: PluginManagerConnection)
        : super(config, pmConn)

    override suspend fun process(input: String): String? {
        return input
    }
}