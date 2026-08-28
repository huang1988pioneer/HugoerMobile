package com.hugoer.mobile.data.service

import com.hugoer.mobile.data.model.GitHubFileContent
import com.hugoer.mobile.data.model.SiteProfile
import com.hugoer.mobile.data.remote.GitHubApiService

/**
 * Finds the Hugo site configuration file inside the repo, trying the same common
 * locations the desktop app's `PathHelper.FindConfigFile` checks.
 */
class ConfigLocator(private val api: GitHubApiService) {

    companion object {
        private val CANDIDATE_PATHS = listOf(
            "hugo.toml",
            "config.toml",
            "config/_default/hugo.toml",
            "config/_default/config.toml"
        )
    }

    val defaultPath: String get() = CANDIDATE_PATHS[0]

    /** Returns the first existing config file, or null if none exist yet. */
    suspend fun find(site: SiteProfile, token: String): GitHubFileContent? {
        for (path in CANDIDATE_PATHS) {
            val file = api.tryGetFile(site.owner, site.repository, path, site.defaultBranch, token)
            if (file != null) return file
        }
        return null
    }
}
