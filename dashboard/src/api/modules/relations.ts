import { request } from "../request";

/** 关系簿里的一条：谁、和用户什么关系、备注。 */
export interface Relation {
  person: string;
  relation: string;
  notes: string;
  updatedAt: number;
}

export function listRelations(person?: string): Promise<{ items: Relation[] }> {
  const q = person?.trim() ? `?person=${encodeURIComponent(person.trim())}` : "";
  return request<{ items: Relation[] }>(`/relations${q}`);
}
