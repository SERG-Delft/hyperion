package nl.tudelft.hyperion.plugin.git

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import git4idea.commands.GitLineHandlerListener

/**
 * Line reader that parses the git blame issued by GitLineTracker to
 * track which line the file is located at now.
 */
class BlameLineReader : GitLineHandlerListener {
    var hasReadHeader = false
    var result: BlameReadResult? = null

    override fun onLineAvailable(line: String, outputType: Key<*>) {
        if (hasReadHeader) {
            return
        }

        hasReadHeader = true

        // If this isn't stdout, it means the resolve failed.
        if (!ProcessOutputType.isStdout(outputType)) {
            result = null
            return
        }

        // Stdout means we're ok.
        val parts = line.split(" ")
        if (parts.size != 4) {
            // should never happen
            result = null
            return
        }

        result = BlameReadResult(
            parts[0],
            parts[1].toInt()
        )
    }
}

data class BlameReadResult(
    val lastSeenCommit: String,
    val lastSeenLine: Int
)
