package com.specguard.repo;

/**
 * A single file changed in a pull request, as reported by GitHub's "List pull request
 * files" REST API: https://docs.github.com/en/rest/pulls/pulls#list-pull-requests-files
 *
 * Every field here is a direct copy of a method on org.kohsuke.github's
 * GHPullRequestFileDetail — the object GithubClient.fetchChangedFiles() gets back per
 * file from pr.listFiles(). Nothing here is invented; each name below is that library's
 * own method name, just as a record field:
 *   GHPullRequestFileDetail.getFilename()  -> filename
 *   GHPullRequestFileDetail.getStatus()    -> status
 *   GHPullRequestFileDetail.getAdditions() -> additions
 *   GHPullRequestFileDetail.getDeletions() -> deletions
 *   GHPullRequestFileDetail.getPatch()     -> patch
 *
 * @param filename  repo-relative path
 * @param status    one of GitHub's file statuses: added, modified, removed, renamed
 * @param additions lines added
 * @param deletions lines removed
 * @param patch     unified diff hunk for this file; null for binary files or very large
 *                  diffs GitHub declines to include a patch for
 */
public record ChangedFile(
        String filename,
        String status,
        int additions,
        int deletions,
        String patch
) {}