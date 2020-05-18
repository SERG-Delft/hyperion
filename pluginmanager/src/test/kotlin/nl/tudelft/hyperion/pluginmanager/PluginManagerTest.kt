package nl.tudelft.hyperion.pluginmanager

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.zeromq.SocketType
import org.zeromq.ZMQ

class PluginManagerTest() {

    private val host = "tcp://localhost:5560"
    private val plugins = listOf(
        mapOf("name" to "Datasource", "host" to "tcp://localhost:1200"),
        mapOf("name" to "Renamer", "host" to "tcp://localhost:1201"),
        mapOf("name" to "Aggregator", "host" to "tcp://localhost:1202")
    )
    private val config = Configuration(host, plugins)

    @Test
    fun `Register valid plugin pull`() {
        val req = "{\"id\":\"Renamer\",\"type\":\"pull\"}"
        val ctx = ZMQ.context(1)
        val res = ctx.socket(SocketType.REP)
        mockkObject(res)

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("{\"isBind\":\"false\",\"host\":\"tcp://localhost:1200\"}")
        }
    }

    @Test
    fun `Register valid plugin push`() {
        val req = "{\"id\":\"Renamer\",\"type\":\"push\"}"
        val ctx = ZMQ.context(1)
        val res = ctx.socket(SocketType.REP)
        mockkObject(res)

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        pluginManager.handleRegister(req, res)

        verify {
            res.send("{\"isBind\":\"true\",\"host\":\"tcp://localhost:1201\"}")
        }
    }

    @Test
    fun `Register invalid request type`() {
        val req = "{\"id\":\"Renamer\",\"type\":\"chicken\"}"
        val ctx = ZMQ.context(1)
        val res = ctx.socket(SocketType.REP)
        mockkObject(res)

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
    fun `Register invalid plugin push`() {
        val req = "{\"id\":\"chicken\",\"type\":\"push\"}"
        val ctx = ZMQ.context(1)
        val res = ctx.socket(SocketType.REP)
        mockkObject(res)

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        val exception: Exception = assertThrows("Plugin chicken does not exist in current pipeline") {
            pluginManager.handleRegister(req, res)
        }

        val expectedMessage = "Plugin chicken does not exist in current pipeline"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun `Register invalid plugin pull`() {
        val req = "{\"id\":\"chicken\",\"type\":\"pull\"}"
        val ctx = ZMQ.context(1)
        val res = ctx.socket(SocketType.REP)
        mockkObject(res)

        every {
            res.send(any<String>())
        } returns true

        val pluginManager = PluginManager(config)
        val exception: Exception = assertThrows("Plugin chicken does not exist in current pipeline") {
            pluginManager.handleRegister(req, res)
        }

        val expectedMessage = "Plugin chicken does not exist in current pipeline"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
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
