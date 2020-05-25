package nl.tudelft.hyperion.plugin.metric

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import nl.tudelft.hyperion.plugin.git.GitLineTracker

/**
 * Represents a version of [FileMetrics] where all line numbers are resolved to
 * line numbers in the local version of the file, then aggregated by summing all
 * versions that are still relevant in this file.
 */
class ResolvedFileMetrics(
    val metrics: FileMetrics,
    val lineSums: Map<Int, Map<Int, Int>>
) {
    companion object {
        /**
         * Using the specified file in the specified project, uses [GitLineTracker]
         * to resolve the current lines for the historical data points, then sums as
         * appropriate based on whether or not they exist.
         */
        fun resolve(metrics: FileMetrics, project: Project, file: VirtualFile): ResolvedFileMetrics {
            val lineSums = mutableMapOf<Int, MutableMap<Int, Int>>()

            for ((originalLine, lineData) in metrics.lines) {
                for ((interval, versions) in lineData.intervals) {
                    for (version in versions) {
                        val newLine = GitLineTracker.resolveCurrentLine(
                            project, file, version.version,
                            originalLine
                        )
                            ?: continue

                        val lineMap = lineSums.getOrPut(newLine, ::mutableMapOf)
                        lineMap[interval] = lineMap.getOrDefault(interval, 0) + version.count
                    }
                }
            }

            return ResolvedFileMetrics(metrics, lineSums)
        }
    }
}
