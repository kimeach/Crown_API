-- 효과음 테이블
CREATE TABLE IF NOT EXISTS sm_sfx (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(200)  NOT NULL,
    tags       VARCHAR(500)  DEFAULT '[]',
    duration   DECIMAL(7,2)  DEFAULT 0,
    s3_url     VARCHAR(1000) NOT NULL,
    source     VARCHAR(100)  DEFAULT 'upload',
    created_at DATETIME      DEFAULT NOW()
);

-- 배경음악 테이블
CREATE TABLE IF NOT EXISTS sm_bgm (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(200)  NOT NULL,
    tags       VARCHAR(500)  DEFAULT '[]',
    duration   DECIMAL(7,2)  DEFAULT 0,
    s3_url     VARCHAR(1000) NOT NULL,
    source     VARCHAR(100)  DEFAULT 'upload',
    created_at DATETIME      DEFAULT NOW()
);
