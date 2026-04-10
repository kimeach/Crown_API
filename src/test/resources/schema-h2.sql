-- H2 테스트용 스키마 (MySQL 호환 모드)

CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(128) UNIQUE,
    email VARCHAR(255),
    display_name VARCHAR(100),
    photo_url VARCHAR(500),
    provider VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_project (
    project_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    title VARCHAR(200),
    status VARCHAR(20) DEFAULT 'PENDING',
    video_url VARCHAR(500),
    html_url VARCHAR(500),
    script_json LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_job (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    started_at DATETIME,
    finished_at DATETIME
);

CREATE TABLE IF NOT EXISTS sm_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    thumbnail_url VARCHAR(500),
    config_json LONGTEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    plan_name VARCHAR(50),
    status VARCHAR(20),
    started_at DATETIME,
    expires_at DATETIME
);

CREATE TABLE IF NOT EXISTS sm_token_wallet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT UNIQUE,
    balance INT DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_token_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    amount INT,
    reason VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    order_id VARCHAR(100),
    payment_key VARCHAR(200),
    amount INT,
    status VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_usage_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    usage_type VARCHAR(50),
    tokens_used INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    path VARCHAR(500),
    method VARCHAR(10),
    status_code INT,
    duration_ms INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_blog_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    title VARCHAR(300),
    content LONGTEXT,
    status VARCHAR(20) DEFAULT 'draft',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_planning (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    description TEXT,
    category VARCHAR(30),
    status VARCHAR(30) DEFAULT 'IDEA',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sm_dev_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    planning_id BIGINT,
    title VARCHAR(200),
    description TEXT,
    category VARCHAR(30),
    status VARCHAR(30) DEFAULT 'PENDING',
    priority TINYINT DEFAULT 3,
    auto_assignable BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 인스타그램 관련 (auto-dev 생성 대비)
CREATE TABLE IF NOT EXISTS sm_instagram_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    instagram_user_id VARCHAR(100),
    access_token VARCHAR(500),
    token_expires_at DATETIME,
    username VARCHAR(100),
    profile_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
