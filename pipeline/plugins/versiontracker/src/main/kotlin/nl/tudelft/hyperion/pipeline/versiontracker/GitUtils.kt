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

private fun createSSHFactory(keyPath: String) = object : JschConfigSessionFactory() {
    override fun configure(hc: OpenSshConfig.Host?, session: Session?) {}

    override fun createDefaultJSch(fs: FS?): JSch {
        val default = super.createDefaultJSch(fs)

        // add private key path
        default.addIdentity(keyPath)

        return default
    }
}

private fun <C : GitCommand<*>?, T> TransportCommand<C, T>.addSSHAuthentication(
        keyPath: String
) =
        setTransportConfigCallback { transport ->
            val sshTransport = transport as SshTransport
            sshTransport.sshSessionFactory = createSSHFactory(keyPath)
        }

private fun lsRemoteCommandBuilder(remote: String): LsRemoteCommand =
        LsRemoteCommand(null)
                .setRemote(remote)
                .setHeads(true)

private fun lsRemoteCommandBuilder(remote: String, username: String, password: String): LsRemoteCommand =
        lsRemoteCommandBuilder(remote)
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))

private fun lsRemoteCommandBuilder(remote: String, keyPath: String): LsRemoteCommand =
        lsRemoteCommandBuilder(remote)
                .addSSHAuthentication(keyPath)