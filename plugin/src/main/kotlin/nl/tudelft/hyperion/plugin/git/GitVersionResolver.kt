package nl.tudelft.hyperion.plugin.git

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitImpl
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

/**
 * Utilities for resolving the git version.
 */
object GitVersionResolver {
    /**
     * Returns the version hash of the current projects origin branch.
     * It does this by getting the repository associated with the current
     * project's `.idea` folder.
     *
     * @return most recent commit hash of origin/branch, null if the branch
     *  does not have a remote.
     */
    fun getCurrentOriginCommit(ideProject: Project): String? {
        if (ApplicationManager.getApplication().isDispatchThread) {
            throw IllegalStateException("May not call resolveCurrentLine on the dispatch thread.")
        }

        require(!ideProject.isDisposed) {
            "IDE project must not be disposed"
        }

        val (repo, branch) = getCurrentRepoInfo(ideProject)
        val gitCommandResult = runRevParse(ideProject, repo, branch)

        return if (gitCommandResult.success()) {
            // The first line is the version hash
            gitCommandResult.output[0]
        } else {
            null
        }
    }

    private fun runRevParse(
        ideProject: Project,
        repo: GitRepository?,
        branch: String?
    ): GitCommandResult =
        GitImpl().runCommand {
            GitLineHandler(ideProject, repo!!.root, GitCommand.REV_PARSE).apply {
                addParameters(branch!!)
                endOptions()
            }
        }

    private fun getCurrentRepoInfo(ideProject: Project): Pair<GitRepository?, String?> {
        // XXX: uses the workspace file to get repository
        val repo = ideProject.workspaceFile?.let {
            GitUtil.getRepositoryManager(ideProject).getRepositoryForFile(it)
        }
            ?: throw IllegalStateException("Current project does not have a repository attached to it")

        val branch = repo.getBranchTrackInfo(repo.currentBranchName!!)?.remoteBranch?.name
        return Pair(repo, branch)
    }
}
