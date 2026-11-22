/**
 * 个人智能体形态下的产品开关。
 *
 * 在这个形态里，智能体团队由主智能体自己创建（`agent_create`），
 * 用户只负责查看与配置，不手动建人；专家市场也一样关闭。
 *
 * 之所以做成常量而不是删代码：将来要做"多主智能体"或开放市场时，
 * 改这两个值即可，不用把按钮重写一遍。
 */
export const ALLOW_MANUAL_AGENT_CREATION = false;
export const SHOW_EXPERT_MARKET = false;
