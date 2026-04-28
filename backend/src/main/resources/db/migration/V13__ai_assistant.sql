-- AI 助手相关表
-- LLM 配置扩展到 site_settings
ALTER TABLE site_settings
    ADD COLUMN llm_provider VARCHAR(32),
    ADD COLUMN llm_api_base_url VARCHAR(512),
    ADD COLUMN llm_api_key VARCHAR(512),
    ADD COLUMN llm_model_name VARCHAR(128);

-- AI 对话表
CREATE TABLE IF NOT EXISTS ai_chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT '新对话',
    road_context VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- AI 消息表
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_chat_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_conv_user ON ai_chat_conversations(user_id);
CREATE INDEX idx_ai_msg_conv ON ai_chat_messages(conversation_id);
