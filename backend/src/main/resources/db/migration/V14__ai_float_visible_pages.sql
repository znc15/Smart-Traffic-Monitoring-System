-- AI 悬浮窗可见页面配置
ALTER TABLE site_settings
    ADD COLUMN ai_float_visible_pages TEXT;

-- 默认值：所有页面可见（逗号分隔的路径前缀列表，空值表示全部可见）
UPDATE site_settings SET ai_float_visible_pages = '' WHERE id = 1;
