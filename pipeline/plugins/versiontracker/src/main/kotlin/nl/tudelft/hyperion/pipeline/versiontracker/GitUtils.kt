package nl.tudelft.hyperion.pipeline.versiontracker

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.LsRemoteCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS

/**
 * Creates an SSH Factory with the given private ssh key.
 *
 * @param keyPath path of the private key file
 */
private fun createSSHFactory(keyPath: String) = object : JschConfigSessionFactory() {
    override fun configure(hc: OpenSshConfig.Host?, session: Session?) = Unit

    override fun createDefaultJSch(fs: FS?): JSch {
        val default = super.createDefaultJSch(fs)

        // add private key path
        default.addIdentity(keyPath)

        return default
    }
}

/**
 * Modifies the given receiver [GitCommand] to add a callback which adds
 * an [JschConfigSessionFactory] for SSH communication.
 *
 * @param C the receiver command
 * @param T the result type of executing the [GitCommand]
 * @param keyPath path of the private key file
 * @return the modified [GitCommand]
 */
private fun <C : GitCommand<*>?, T> TransportCommand<C, T>.addSSHAuthentication(
    keyPath: String
): C =
    setTransportConfigCallback { transport ->
        val sshTransport = transport as SshTransport
        sshTransport.sshSessionFactory = createSSHFactory(keyPath)
    }

/**
 * Base builder for the [LsRemoteCommand], assumes no repo is given.
 *
 * @param remote the URL of the remote repository to fetch from
 * @return [LsRemoteCommand] to call
 */
fun lsRemoteCommandBuilder(remote: String): LsRemoteCommand =
    LsRemoteCommand(null)
        .setRemote(remote)
        .setHeads(true)

/**
 * Overloaded builder that adds HTTPS based authentication on top of the base command
 *
 * @param remote the URL of the remote repository to fetch from
 * @param username git username to use for authentication
 * @param password git password to use for authentication
 * @return [LsRemoteCommand] to call
 */
fun lsRemoteCommandBuilder(remote: String, username: String, password: String): LsRemoteCommand =
    lsRemoteCommandBuilder(remote)
        .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))

/**
 * Overloaded builder that adds SSH based authentication on top of the base command
 *
 * @param remote the URL of the remote repository to fetch from
 * @param keyPath path of the private key file
 * @return [LsRemoteCommand] to call
 */
fun lsRemoteCommandBuilder(remote: String, keyPath: String): LsRemoteCommand =
    lsRemoteCommandBuilder(remote)
        .addSSHAuthentication(keyPath)
