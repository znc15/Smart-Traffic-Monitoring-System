CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL DEFAULT 1,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_name VARCHAR(255) NOT NULL,
    announcement TEXT,
    logo_url VARCHAR(1024),
    footer_text TEXT,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS cameras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255),
    stream_url VARCHAR(1024),
    road_name VARCHAR(255),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS token_llm (
    user_id BIGINT PRIMARY KEY,
    token INTEGER NOT NULL DEFAULT 5000,
    CONSTRAINT fk_token_llm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT IGNORE INTO site_settings (id, site_name, announcement, logo_url, footer_text, updated_at)
VALUES (1, '智能交通监控系统', '', NULL, NULL, NOW());
