import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Button, Empty, Input, List, Space, Spin, Tag, Typography } from "antd";
import { RefreshCw, Search } from "lucide-react";
import { useTranslation } from "react-i18next";
import PageShell from "../../layouts/PageShell";
import { listRelations, type Relation } from "../../api/modules/relations";

const { Text } = Typography;

/**
 * 「关系」：管家记得的人。
 *
 * 这里只读——关系由管家在对话里记录（`relation_remember`），
 * 用户不该在页面上手工维护一本通讯录，那又回到"配置"的老路上。
 */
export default function RelationsPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<Relation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [needle, setNeedle] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await listRelations();
      setItems(res.items ?? []);
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
    const q = needle.trim().toLowerCase();
    if (!q) return items;
    return items.filter(
      (r) =>
        r.person.toLowerCase().includes(q) || r.relation.toLowerCase().includes(q),
    );
  }, [items, needle]);

  return (
    <PageShell
      title={t("nav.relations")}
      subtitle={t("relations.subtitle")}
      actions={
        <Button icon={<RefreshCw size={16} />} loading={loading} onClick={() => void load()}>
          {t("relations.refresh")}
        </Button>
      }
    >
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <Input
          allowClear
          prefix={<Search size={14} />}
          placeholder={t("relations.searchPlaceholder")}
          value={needle}
          onChange={(e) => setNeedle(e.target.value)}
        />

        {error ? (
          <Alert type="error" showIcon message={t("relations.loadFailed")} description={error} />
        ) : loading && items.length === 0 ? (
          <Spin />
        ) : (
          <List
            dataSource={visible}
            locale={{ emptyText: <Empty description={t("relations.empty")} /> }}
            renderItem={(item) => (
              <List.Item key={item.person}>
                <List.Item.Meta
                  title={
                    <Space size={8} wrap>
                      <span>{item.person}</span>
                      {item.relation ? <Tag color="gold">{item.relation}</Tag> : null}
                    </Space>
                  }
                  description={item.notes ? <Text type="secondary">{item.notes}</Text> : null}
                />
              </List.Item>
            )}
          />
        )}
      </Space>
    </PageShell>
  );
}
