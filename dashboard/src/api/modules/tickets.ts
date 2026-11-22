import { request } from "../request";

/** 工单在网线上的形状（后端 `Ticket.to_dict()`，camelCase）。 */
export interface Ticket {
  ticketId: string;
  ownerUserId: number;
  agentId: string;
  threadId: string | null;
  title: string;
  goal: string;
  stage: string;
  priority: number;
  assignmentKind: string;
  assigneeId: string | null;
  openAttention: boolean;
  attentionReason: string | null;
  latestReport: string | null;
  deliveryFailed: boolean;
  createdAt: number;
  updatedAt: number;
  startedAt: number | null;
  endedAt: number | null;
}

export interface TicketListResponse {
  items: Ticket[];
  counts: Record<string, number>;
}

/** 七态，顺序即优先级（person/* 在前，和列表排序一致）。 */
export const TICKET_STAGES = [
  "person/delivery",
  "person/answer",
  "person/review",
  "person/blocked",
  "working/queued",
  "working",
  "idle",
] as const;

export type TicketStage = (typeof TICKET_STAGES)[number];

export async function listTickets(
  params: { stage?: string; agentId?: string; limit?: number; offset?: number } = {},
): Promise<TicketListResponse> {
  const q = new URLSearchParams();
  if (params.stage) q.set("stage", params.stage);
  if (params.agentId) q.set("agent_id", params.agentId);
  if (params.limit) q.set("limit", String(params.limit));
  if (params.offset) q.set("offset", String(params.offset));
  const qs = q.toString();
  return request<TicketListResponse>(`/tickets${qs ? `?${qs}` : ""}`);
}
