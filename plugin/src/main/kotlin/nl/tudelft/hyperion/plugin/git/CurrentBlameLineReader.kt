package nl.tudelft.hyperion.plugin.git

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import git4idea.commands.GitLineHandlerListener

/**
 * Line reader that parses the git blame issued by GitLineTracker to
 * track which line the file is located at now.
 */
class CurrentBlameLineReader(
    val origin: OriginBlameReadResult
) : GitLineHandlerListener {
    var result: CurrentBlameReadResult? = null

    override fun onLineAvailable(line: String, outputType: Key<*>) {
        // If this isn't stdout, it means the resolve failed.
        if (!ProcessOutputType.isStdout(outputType)) {
            result = null
            return
        }

        // Stdout means we're ok.
        // If this is a line that originated from the commit, check if the original line matches.
        if (!line.trim().startsWith(origin.lastSeenCommit)) return

        val parts = line.split(" ")
        if (parts[1].toInt() != origin.lastSeenLine) return

        // Line and commit matches.
        result = CurrentBlameReadResult(parts[2].toInt())
    }
}

data class CurrentBlameReadResult(
    val currentLine: Int
)
