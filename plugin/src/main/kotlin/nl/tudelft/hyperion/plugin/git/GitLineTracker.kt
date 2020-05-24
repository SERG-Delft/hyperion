package nl.tudelft.hyperion.plugin.git

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

object GitLineTracker {
    /**
     * Takes in a project and file, as well as an old commit hash and a line number. Will
     * attempt to walk both the git history and the unstaged changes to resolve what line
     * number the old line is located at now. If it can be resolved, AND this log line is
     * still in the current file, it will be returned. If it cannot be resolved, or if the
     * log line is now in a different file, returns null.
     *
     * Note: line is 1-based, aka line 1 is the first line. The return value will also
     * be one based.
     *
     * Note that this is a blocking operation and as such should not be ran on the UI thread.
     */
    fun resolveCurrentLine(
        project: Project,
        file: VirtualFile,
        oldCommit: String,
        oldLine: Int
    ): Int? {
        // Assert that we're not blocking the main thread.
        if (ApplicationManager.getApplication().isDispatchThread) {
            throw IllegalStateException("May not call resolveCurrentLine on the dispatch thread.")
        }

        // Retrieve the repo this file is located in.
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file) ?: return null
        val tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(file) ?: return null

        // Run a git blame first.
        val blameResult = runBlame(project, repo, file, oldCommit, oldLine) ?: return null

        // If the last seen commit is not the current head, we lost it somewhere in the commit history.
        // Give up.
        if (blameResult.lastSeenCommit != repo.currentRevision) {
            return null
        }

        // We know where it was in HEAD, but we might have local unstaged changes for the file
        // that changes where the line is right now. First, ask intellij if the line was changed.
        // Note that we need to work with -1 as intellij is 0-based for lines.
        if (!tracker.isLineModified(blameResult.lastSeenLine - 1)) {
            return blameResult.lastSeenLine
        }

        // We will ask intellij if it knows where the file has been moved to.
        val newLine = tracker.transferLineFromVcs(blameResult.lastSeenLine - 1, false)

        if (newLine == UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) {
            // IntelliJ does not know where it went.
            return null
        }

        return newLine + 1
    }

    /**
     * Runs a git blame on the specified project/repo/file pair, trying to find the
     * current location of the specified old commit and line. Returns the result if
     * a success, or null if the blame couldn't be ran.
     */
    private fun runBlame(
        project: Project,
        repo: GitRepository,
        file: VirtualFile,
        oldCommit: String,
        oldLine: Int
    ): BlameReadResult? {
        val handler = GitLineHandler(project, repo.root, GitCommand.BLAME)
        handler.addParameters(
            "--reverse",
            oldCommit,
            "-L",
            "$oldLine,$oldLine",
            "-M1",
            "-C",
            "-C",
            "-w",
            "-n",
            "--porcelain"
        )
        handler.endOptions()
        handler.addRelativeFiles(listOf(file))

        val lineReader = BlameLineReader()
        handler.addLineListener(lineReader)

        val result = Git.getInstance().runCommandWithoutCollectingOutput(handler)

        return if (result.success()) {
            lineReader.result
        } else {
            null
        }
    }
}
