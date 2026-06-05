package com.cijian.common.mq;

/**
 * RocketMQ Topic 常量
 */
public final class RocketMQTopics {

    private RocketMQTopics() {}

    /** 作品发布事件 */
    public static final String WORK_PUBLISHED = "work_published";

    /** 互动通知事件 */
    public static final String INTERACTION_NOTIFY = "interaction_notify";

    /** 搜索索引更新事件 */
    public static final String SEARCH_INDEX_UPDATE = "search_index_update";

    /** 内容审核事件 */
    public static final String CONTENT_REVIEW = "content_review";

    /** AI 分析完成事件 */
    public static final String AI_ANALYSIS_FINISHED = "ai_analysis_finished";

    /** 用户行为埋点 */
    public static final String USER_ACTION_LOG = "user_action_log";
}
