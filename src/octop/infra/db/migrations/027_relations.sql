-- 人际关系记忆：谁是谁、什么关系。
-- 和聊天记忆不同：这里存的是稳定事实（"张总是我老板"），不随对话轮次变化。
CREATE TABLE IF NOT EXISTS relations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id   INTEGER NOT NULL,
    person          TEXT    NOT NULL COLLATE NOCASE,
    relation        TEXT    NOT NULL DEFAULT '',
    notes           TEXT    NOT NULL DEFAULT '',
    source_agent_id TEXT    NOT NULL DEFAULT '',
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_relations_owner_person
    ON relations (owner_user_id, person);

CREATE INDEX IF NOT EXISTS idx_relations_updated
    ON relations (owner_user_id, updated_at);

UPDATE _schema_version SET version = 27;
