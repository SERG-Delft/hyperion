package nl.tudelft.hyperion.plugin.git

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import git4idea.commands.GitLineHandlerListener

/**
 * Line reader that parses the git blame issued by GitLineTracker to
 * track which line the file is located at now.
 */
class OriginBlameLineReader : GitLineHandlerListener {
    var hasReadHeader = false
    var result: OriginBlameReadResult? = null

    override fun onLineAvailable(line: String, outputType: Key<*>) {
        if (hasReadHeader) {
            return
        }

        hasReadHeader = true

        val parts = getParts(line, outputType) ?: return

        result = OriginBlameReadResult(
            parts[0],
            parts[1].toInt()
        )
    }

    private fun getParts(line: String, outputType: Key<*>): List<String>? {
        // If this isn't stdout, it means the resolve failed.
        if (!ProcessOutputType.isStdout(outputType)) {
            result = null
            return null
        }

        // Stdout means we're ok.
        val parts = line.split(" ")
        if (parts.size != 4) {
            // should never happen
            result = null
            return null
        }

        return parts
    }
}

data class OriginBlameReadResult(
    val lastSeenCommit: String,
    val lastSeenLine: Int
)
