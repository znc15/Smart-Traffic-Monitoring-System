-- V15: Add title generation model config fields
ALTER TABLE site_settings
    ADD COLUMN llm_title_model_name VARCHAR(255),
    ADD COLUMN llm_title_prompt TEXT;

-- Set default title prompt for existing rows
UPDATE site_settings
SET llm_title_prompt = '根据以下对话内容生成一个简短的中文标题（不超过20字），只返回标题本身，不要加引号或其他格式。'
WHERE llm_title_prompt IS NULL;