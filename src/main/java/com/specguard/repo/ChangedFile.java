package com.specguard.repo;

/**
 * A single file changed in a pull request, as reported by the GitHub REST API.
 * https://docs.github.com/en/enterprise-server@3.19/rest/pulls/pulls?apiVersion=2022-11-28#get-a-pull-request
 *
 * Will need these:
 * @param filename  repo-relative path
 * @param status    one of GitHub's file statuses: added, modified, removed, renamed
 * @param additions lines added
 * @param deletions lines removed
 * @param patch     unified diff hunk for this file; null for binary files or very large diffs
 *                  (Since GitHub declines to include a patch)
 */
public record ChangedFile(
        // Todo: Add correct fields from github api, use demo field names for now if required
) {}