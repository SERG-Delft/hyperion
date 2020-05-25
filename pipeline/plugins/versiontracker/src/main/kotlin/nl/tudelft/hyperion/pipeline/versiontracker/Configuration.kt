package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Represents the global configuration necessary for the version tracker plugin to run.
 *
 * @property zmq ZeroMQ specific config
 * @property projects Map of projects to track
 */
data class Configuration(
        val zmq: PipelinePluginConfiguration,
        val projects: Map<String, ProjectConfig>
)

/**
 * Represents the configuration necessary to track a single project.
 *
 * @property repository the URI of the repository to track
 * @property branch the name of the branch to track.
 *  Note that only head branches are tracked
 * @property authentication the necessary information to authenticate remote fetching.
 *  Can either be null for no authentication, SSH for key-based authentication or
 *  HTTPS for username and password based authentication
 */
data class ProjectConfig(
        val repository: String,
        val branch: String,
        val authentication: Authentication?
)

/**
 * Represents the configuration necessary to allow fetching from repositories.
 *
 * @property type which communication method is used for fetching from the repository
 */
@JsonDeserialize(using = AuthenticationDeserializer::class)
sealed class Authentication(private val type: CommunicationProtocol) {
    data class SSH(
            val keyPath: String
    ) : Authentication(CommunicationProtocol.SSH)

    data class HTTPS(
            val username: String,
            val password: String
    ) : Authentication(CommunicationProtocol.HTTPS)
}

/**
 * Represents the possible communication methods for Git.
 */
enum class CommunicationProtocol {
    @JsonProperty("ssh")
    SSH,

    @JsonProperty("https")
    HTTPS
}

/**
 * Custom deserializer to serialize the defined authentication field.
 * Can either deserialize the given JSON tree into [Authentication.SSH] or [Authentication.HTTPS].
 */
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
