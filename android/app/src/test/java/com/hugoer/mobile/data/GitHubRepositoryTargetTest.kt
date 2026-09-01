package com.hugoer.mobile.data

import com.hugoer.mobile.data.model.GitHubRepositoryTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubRepositoryTargetTest {
    @Test
    fun parsesOwnerRepo() {
        val target = GitHubRepositoryTarget.parse("owner/hugo-site")
        assertTrue(target.isValid)
        assertEquals("owner", target.owner)
        assertEquals("hugo-site", target.repository)
        assertEquals("https://owner.github.io/hugo-site/", target.pagesUrl)
        assertFalse(target.isUserOrOrganizationSite)
    }

    @Test
    fun parsesHttpsUrlAndGitSuffix() {
        val target = GitHubRepositoryTarget.parse("https://github.com/Owner/Site.git")
        assertTrue(target.isValid)
        assertEquals("Owner", target.owner)
        assertEquals("Site", target.repository)
    }

    @Test
    fun detectsUserPagesSite() {
        val target = GitHubRepositoryTarget.parse("alice/alice.github.io")
        assertTrue(target.isValid)
        assertTrue(target.isUserOrOrganizationSite)
        assertEquals("https://alice.github.io/", target.pagesUrl)
    }

    @Test
    fun rejectsBlankAndNonGithubHosts() {
        assertFalse(GitHubRepositoryTarget.parse("").isValid)
        assertFalse(GitHubRepositoryTarget.parse("https://gitlab.com/owner/repo").isValid)
        assertFalse(GitHubRepositoryTarget.parse("owner").isValid)
    }
}
