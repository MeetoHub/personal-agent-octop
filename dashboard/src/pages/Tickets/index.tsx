import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Button, Empty, List, Segmented, Space, Spin, Tag, Typography } from "antd";
import { RefreshCw } from "lucide-react";
import { useTranslation } from "react-i18next";
import PageShell from "../../layouts/PageShell";
import { listTickets, type Ticket } from "../../api/modules/tickets";

const { Text } = Typography;

const STAGE_COLOR: Record<string, string> = {
  "person/delivery": "red",
  "person/answer": "gold",
  "person/review": "blue",
  "person/blocked": "red",
  "working/queued": "default",
  working: "processing",
  idle: "default",
};

function formatTime(seconds: number): string {
  if (!seconds) return "";
  const date = new Date(seconds * 1000);
  return date.toLocaleString();
}

/**
 * 「活」：工单列表。
 *
 * 工单是长任务的唯一事实来源——stage 由后端从原始列推导，前端只展示不计算，
 * 免得出现"界面上是进行中、实际已经交付失败"这种自相矛盾。
 */
export default function TicketsPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<Ticket[]>([]);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<string>("all");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await listTickets({ limit: 200 });
      setItems(res.items ?? []);
      setCounts(res.counts ?? {});
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = useMemo(() => {
    if (filter === "all") return items;
    if (filter === "person") return items.filter((item) => item.stage.startsWith("person/"));
    return items.filter((item) => item.stage === filter);
  }, [items, filter]);

  const waiting = useMemo(
    () => items.filter((item) => item.stage.startsWith("person/")).length,
    [items],
  );

  return (
    <PageShell
      title={t("nav.tickets")}
      subtitle={t("tickets.subtitle")}
      actions={
        <Button icon={<RefreshCw size={16} />} loading={loading} onClick={() => void load()}>
          {t("tickets.refresh")}
        </Button>
      }
    >
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <Segmented
          value={filter}
          onChange={(value) => setFilter(String(value))}
          options={[
            { value: "all", label: `${t("tickets.filterAll")} (${items.length})` },
            { value: "person", label: `${t("tickets.filterPerson")} (${waiting})` },
            {
              value: "working",
              label: `${t("tickets.filterWorking")} (${counts["working"] ?? 0})`,
            },
          ]}
        />

        {error ? (
          <Alert type="error" showIcon message={t("tickets.loadFailed")} description={error} />
        ) : loading && items.length === 0 ? (
          <Spin />
        ) : (
          <List
            dataSource={visible}
            locale={{
              emptyText: <Empty description={t("tickets.empty")} />,
            }}
            renderItem={(item) => (
              <List.Item key={item.ticketId}>
                <List.Item.Meta
                  title={
                    <Space size={8} wrap>
                      <span>{item.title}</span>
                      <Tag color={STAGE_COLOR[item.stage] ?? "default"}>
                        {t(`tickets.stage.${item.stage}`, item.stage)}
                      </Tag>
                      {item.openAttention && item.attentionReason ? (
                        <Text type="warning">{item.attentionReason}</Text>
                      ) : null}
                    </Space>
                  }
                  description={
                    <Space direction="vertical" size={2}>
                      {item.goal ? <Text type="secondary">{item.goal}</Text> : null}
                      {item.latestReport ? (
                        <Text type="secondary">
                          {t("tickets.latestReport")}: {item.latestReport}
                        </Text>
                      ) : null}
                    </Space>
                  }
                />
                <Text type="secondary">{formatTime(item.updatedAt)}</Text>
              </List.Item>
            )}
          />
        )}
      </Space>
    </PageShell>
  );
}
