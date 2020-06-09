package nl.tudelft.hyperion.pluginmanager

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZMQ

class PluginManagerTest {

    private val host = "tcp://localhost:5560"
    private val plugins = listOf(
        PipelinePluginConfig("Datasource", "tcp://localhost:1200"),
        PipelinePluginConfig("Renamer", "tcp://localhost:1201"),
        PipelinePluginConfig("Aggregator", "tcp://localhost:1202")
    )
    private val config = Configuration(host, plugins)

    @Test
    fun `Register valid plugin pull`() {
        val req = """{"id":"Renamer","type":"pull"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("""{"host":"tcp://localhost:1200","isBind":false}""")
        }
    }

    @Test
    fun `Register valid plugin push`() {
        val req = """{"id":"Renamer","type":"push"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("""{"host":"tcp://localhost:1201","isBind":true}""")
        }
    }

    @Test
    fun `Register datasource as pull (invalid)`() {
        val req = """{"id":"Datasource","type":"pull"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("""{"host":null,"isBind":false}""")
        }
    }

    @Test
    fun `Register aggregator as push (invalid)`() {
        val req = """{"id":"Aggregator","type":"push"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("""{"host":null,"isBind":true}""")
        }
    }

    @Test
    fun `Register invalid request type`() {
        val req = """{"id":"Renamer","type":"chicken"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("Invalid Request")
        }
    }

    @Test
    fun `Register invalid plugin`() {
        val req = """{"id":"chicken","type":"push"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("Invalid Request")
        }
    }

    @Test
    fun `Register invalid missing id`() {
        val req = """{"type":"push"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("Invalid Request")
        }
    }

    @Test
    fun `Register invalid missing type`() {
        val req = """{"id":"Renamer"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("Invalid Request")
        }
    }

    @Test
    fun `Register invalid message structure`() {
        val req = """You can't handle me"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("Invalid Request")
        }
    }

    @Test
    fun `Register multiple plugins`() {
        val req1 = """{"id":"Renamer","type":"pull"}"""
        val req2 = """{"id":"Renamer","type":"push"}"""
        val res = mockk<ZMQ.Socket>()

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req1, res)
        pluginManager.handleRegister(req2, res)

        verify {
            res.send("""{"host":"tcp://localhost:1200","isBind":false}""")
            res.send("""{"host":"tcp://localhost:1201","isBind":true}""")
        }
    }

    @Test
    fun `Cleanup ZMQ connection after Interrupt`() {
        mockkStatic("org.zeromq.ZMQ")

        val ctx = ZMQ.context(1)
        mockkObject(ctx)
        every {
            ZMQ.context(any())
        } returns ctx

        every {
            ctx.term()
        } returns Unit

        val res = ctx.socket(SocketType.REP)
        mockkObject(res)
        every {
            ctx.socket(any<SocketType>())
        } returns res

        // sets the isInterrupted flag, prevents the main loop from running at all.
        Thread.currentThread().interrupt()

        val pluginManager = PluginManager(config)
        pluginManager.launchListener()

        verify {
            res.close()
            ctx.term()
        }
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }
}
