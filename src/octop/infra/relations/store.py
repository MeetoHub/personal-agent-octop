"""人际关系记忆：谁是谁、什么关系。

和聊天记忆的区别：这里存的是**稳定事实**（"张总是我老板"），不随对话轮次变化。
按 ``(owner_user_id, person)`` 唯一，重复提起只更新关系与备注，不新增行。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from octop.infra.db.pool import DatabasePool
from octop.infra.db.repos._base import now_ts


@dataclass(frozen=True)
class Relation:
    person: str
    relation: str
    notes: str
    updated_at: int

    def to_dict(self) -> dict[str, Any]:
        return {
            "person": self.person,
            "relation": self.relation,
            "notes": self.notes,
            "updatedAt": self.updated_at,
        }


def _row_to_relation(row: Any) -> Relation:
    return Relation(
        person=str(row["person"]),
        relation=str(row["relation"]),
        notes=str(row["notes"]),
        updated_at=int(row["updated_at"]),
    )


class RelationStore:
    """一个用户一本"关系簿"，管理员也不能跨用户读。"""

    def __init__(self, db: DatabasePool) -> None:
        self._db = db

    def upsert(
        self,
        *,
        owner_user_id: int,
        person: str,
        relation: str,
        notes: str = "",
        source_agent_id: str = "",
    ) -> Relation:
        name = person.strip()
        if not name:
            raise ValueError("person must not be empty")
        rel = relation.strip()
        note = notes.strip()
        ts = now_ts()
        with self._db.transaction() as conn:
            conn.execute(
                """
                INSERT INTO relations
                    (owner_user_id, person, relation, notes, source_agent_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(owner_user_id, person) DO UPDATE SET
                    relation = excluded.relation,
                    notes = CASE
                        WHEN excluded.notes != '' THEN excluded.notes
                        ELSE relations.notes
                    END,
                    updated_at = excluded.updated_at
                """,
                (int(owner_user_id), name, rel, note, source_agent_id, ts, ts),
            )
        return Relation(person=name, relation=rel, notes=note, updated_at=ts)

    def lookup(self, owner_user_id: int, person: str, *, limit: int = 10) -> list[Relation]:
        needle = person.strip()
        with self._db.connect() as conn:
            if needle:
                rows = conn.execute(
                    """
                    SELECT person, relation, notes, updated_at FROM relations
                    WHERE owner_user_id = ? AND person LIKE ?
                    ORDER BY updated_at DESC LIMIT ?
                    """,
                    (int(owner_user_id), f"%{needle}%", limit),
                ).fetchall()
            else:
                rows = conn.execute(
                    """
                    SELECT person, relation, notes, updated_at FROM relations
                    WHERE owner_user_id = ?
                    ORDER BY updated_at DESC LIMIT ?
                    """,
                    (int(owner_user_id), limit),
                ).fetchall()
        return [_row_to_relation(r) for r in rows]
