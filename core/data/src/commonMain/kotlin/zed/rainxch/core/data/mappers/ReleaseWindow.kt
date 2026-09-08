package zed.rainxch.core.data.mappers

import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.domain.model.account.github.GithubRelease
import zed.rainxch.core.domain.model.account.github.isEffectivelyPreRelease

// One shared release-window normalization: drop drafts, newest first,
// map to domain, gate pre-releases. Every forge source funnels through
// here so ordering and pre-release semantics stay identical across them.
fun List<ReleaseNetwork>.toReleaseWindow(includePreReleases: Boolean): List<GithubRelease> =
    asSequence()
        .filter { it.draft != true }
        .sortedByDescending { it.publishedAt ?: it.createdAt ?: "" }
        .map { it.toDomain() }
        .filter { includePreReleases || !it.isEffectivelyPreRelease() }
        .toList()
