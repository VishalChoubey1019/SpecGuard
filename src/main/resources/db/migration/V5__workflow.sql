-- V5: workflow automation tables. Records what SpecGuard actually did in response to
-- violations found on a PR: created/updated a Jira ticket, sent a Slack alert, or posted a
-- GitHub PR comment.
--
-- No WORKFLOW (rule-config) table yet, on purpose: which severities trigger which actions
-- is still a hardcoded threshold in application code, not DB-driven config — same reasoning
-- as the policy engine being deferred (see HANDOFF). Add one later only if/when workflow
-- rules actually need to be admin-editable without a redeploy.
--
-- No dedicated slack_alerts or pr_comments table: unlike Jira, neither needs "does this
-- already exist" idempotency beyond what workflow_executions can already answer via a
-- query (action + pull_request_id + status). Jira gets its own table specifically because
-- it needs a queryable issue_key to update an existing ticket instead of creating a
-- duplicate on a later push to the same PR.

CREATE TABLE jira_issues (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    pull_request_id   UUID NOT NULL REFERENCES pull_requests (id) ON DELETE CASCADE,
    issue_key         VARCHAR(64) NOT NULL,   -- e.g. "SPEC-123", as returned by Jira's create-issue API
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One Jira ticket per PR: the workflow service checks for an existing row here before
-- creating a new issue, so re-analyzing a PR (e.g. on a new push) updates the same ticket.
CREATE UNIQUE INDEX uq_jira_issues_pull_request_id ON jira_issues (pull_request_id);
CREATE INDEX idx_jira_issues_tenant_id ON jira_issues (tenant_id);

CREATE TABLE workflow_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    pull_request_id   UUID NOT NULL REFERENCES pull_requests (id) ON DELETE CASCADE,
    action            VARCHAR(32) NOT NULL,   -- CREATE_JIRA_ISSUE | POST_SLACK_ALERT | POST_PR_COMMENT
    status            VARCHAR(16) NOT NULL,   -- SUCCESS | FAILED
    detail            TEXT,                   -- e.g. Jira issue key, PR comment URL, or an error message
    executed_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workflow_executions_pull_request_id ON workflow_executions (pull_request_id);
CREATE INDEX idx_workflow_executions_tenant_id ON workflow_executions (tenant_id);