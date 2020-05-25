package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

data class Configuration(
        val zmq: PipelinePluginConfiguration,
        val projects: Map<String, ProjectConfig>
)

data class ProjectConfig(
        val repository: String,
        val branch: String,
        val authentication: Authentication?
)

class AuthenticationDeserializer : JsonDeserializer<Authentication>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Authentication {
        val root: JsonNode = p.codec.readTree(p)

        if (root["type"] == null) {
            throw InvalidFormatException(p, "Invalid format, missing 'type'", root["type"], Authentication::class.java)
        }

        return when (val type = root.get("type").textValue().toLowerCase()) {
            "ssh" -> run {
                if (root["keyPath"] == null) {
                    throw InvalidFormatException(
                            p,
                            "Missing keyPath field in ssh authentication",
                            root,
                            Authentication::class.java
                    )
                }

                Authentication.SSH(root.get("keyPath").textValue())
            }

            "https" -> run {
                if (root["username"] == null || root["password"] == null) {
                    throw InvalidFormatException(
                            p,
                            "Missing username or password field in https authentication",
                            root,
                            Authentication::class.java
                    )
                }

                Authentication.HTTPS(
                        root.get("username").textValue(),
                        root.get("password").textValue()
                )
            }

            else ->
                throw InvalidFormatException(p, "Expected ssh or https but got $type", type, Authentication::class.java)
        }
    }
}

@JsonDeserialize(using = AuthenticationDeserializer::class)
sealed class Authentication(val type: CommunicationProtocol) {
    data class SSH(
            val keyPath: String
    ) : Authentication(CommunicationProtocol.SSH)

    data class HTTPS(
            val username: String,
            val password: String
    ) : Authentication(CommunicationProtocol.HTTPS)
}

enum class CommunicationProtocol {
    @JsonProperty("ssh")
    SSH,

    @JsonProperty("https")
    HTTPS
}

object Parser {
    var mapper: ObjectMapper = ObjectMapper(YAMLFactory())

    init {
        mapper.registerModule(KotlinModule())
    }

    inline fun <reified T : Any> parseYAMLConfig(content: String): T {
        return mapper.readValue(content)
    }
}
