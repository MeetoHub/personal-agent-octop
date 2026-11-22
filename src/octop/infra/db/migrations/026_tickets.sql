-- Schema v26: tickets — the durable work-order layer that sits between a
-- conversation turn and the experts/subagents that carry it out.
--
-- Design notes:
--   * Additive only. No existing table is altered, no column is dropped.
--   * `thread_id` is a soft foreign key: threads live in the engine checkpoint
--     store, not in this database, so no FK constraint can be declared.
--   * Timestamps are Unix seconds (INTEGER), matching every other migration.
--   * `owner_user_id` enforces the multi-account boundary; every query in
--     api/routers/tickets.py scopes on it unless the caller is an admin.

CREATE TABLE IF NOT EXISTS tickets (
  id                INTEGER PRIMARY KEY AUTOINCREMENT,
  ticket_id         TEXT NOT NULL UNIQUE,          -- ULID, public identifier
  owner_user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  agent_id          TEXT NOT NULL,                 -- the expert that owns the work
  thread_id         TEXT,                          -- soft FK -> engine thread
  title             TEXT NOT NULL,
  goal              TEXT NOT NULL DEFAULT '',      -- what "done" means
  stage             TEXT NOT NULL DEFAULT 'working/queued',
  priority          INTEGER NOT NULL DEFAULT 0,    -- larger = more urgent
  assignment_kind   TEXT NOT NULL DEFAULT 'expert',-- expert | subagent | team
  assignee_id       TEXT,                          -- agent_id / subagent slug / team id
  open_attention    INTEGER NOT NULL DEFAULT 0,    -- 1 = a human must answer
  attention_reason  TEXT,
  latest_report     TEXT,                          -- progress|waiting|ready|blocked|failed|dropped
  delivery_failed   INTEGER NOT NULL DEFAULT 0,
  created_at        INTEGER NOT NULL,
  updated_at        INTEGER NOT NULL,
  started_at        INTEGER,
  ended_at          INTEGER
);

CREATE INDEX IF NOT EXISTS idx_tickets_owner_stage
  ON tickets(owner_user_id, stage);

CREATE INDEX IF NOT EXISTS idx_tickets_thread
  ON tickets(thread_id);

CREATE INDEX IF NOT EXISTS idx_tickets_agent
  ON tickets(agent_id);

CREATE INDEX IF NOT EXISTS idx_tickets_updated
  ON tickets(owner_user_id, updated_at DESC);

-- Append-only audit trail for a ticket. The UI renders this as the "活" timeline.
CREATE TABLE IF NOT EXISTS ticket_events (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  ticket_id    TEXT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
  kind         TEXT NOT NULL,          -- created|assigned|report|attention|resolved|closed
  actor        TEXT,                   -- user:<id> | agent:<id> | system
  summary      TEXT NOT NULL DEFAULT '',
  payload_json TEXT NOT NULL DEFAULT '{}',
  created_at   INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_events_ticket
  ON ticket_events(ticket_id, created_at);

UPDATE _schema_version SET version = 26;
