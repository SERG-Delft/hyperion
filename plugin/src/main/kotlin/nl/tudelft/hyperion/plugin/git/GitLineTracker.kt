package nl.tudelft.hyperion.plugin.git

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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

        // Run our blames
        val originBlameResult = runOriginBlame(project, repo, file, oldCommit, oldLine) ?: return null
        return runCurrentBlame(project, repo, file, originBlameResult)?.currentLine ?: return null
    }

    /**
     * Runs a git blame on the specified project/repo/file pair, trying to find the
     * origin commit of the specified line in the specified file on the specified
     * commit. Returns the hash and line of the commit that introduced that line, or
     * null if it could not be resolved.
     */
    private fun runOriginBlame(
        project: Project,
        repo: GitRepository,
        file: VirtualFile,
        oldCommit: String,
        oldLine: Int
    ): OriginBlameReadResult? {
        val params = listOf(oldCommit, "-L", "$oldLine,$oldLine", "-M1", "-C", "-C", "-w", "-n", "--porcelain")
        val handler = setupHandler(project, repo.root, params, file)

        val lineReader = OriginBlameLineReader()
        handler.addLineListener(lineReader)

        val result = Git.getInstance().runCommandWithoutCollectingOutput(handler)

        if (result.success()) return lineReader.result
        return null
    }

    /**
     * Runs a git blame on the current working tree to see if any lines in the
     * current file can be traced back to the specified origin commit. Returns
     * the result if the line can be traced back, or null otherwise.
     */
    private fun runCurrentBlame(
        project: Project,
        repo: GitRepository,
        file: VirtualFile,
        origin: OriginBlameReadResult
    ): CurrentBlameReadResult? {
        val params = listOf("-p", "-l", "-t", "-w")
        val handler = setupHandler(project, repo.root, params, file)

        val lineReader = CurrentBlameLineReader(origin)
        handler.addLineListener(lineReader)

        val result = Git.getInstance().runCommandWithoutCollectingOutput(handler)
        if (result.success()) return lineReader.result
        return null
    }

    private fun setupHandler(project: Project, root: VirtualFile, params: List<String>, file: VirtualFile):
        GitLineHandler {
        val handler = GitLineHandler(project, root, GitCommand.BLAME)
        handler.addParameters(params)
        handler.endOptions()
        handler.addRelativeFiles(listOf(file))

        return handler
    }
}
