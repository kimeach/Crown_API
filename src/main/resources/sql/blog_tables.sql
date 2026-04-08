-- 블로그 말투 프로필
CREATE TABLE IF NOT EXISTS sm_blog_tone (
    tone_id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    style           VARCHAR(100) NOT NULL COMMENT '분석된 스타일명 (예: 친근하면서 전문적인)',
    characteristics JSON         NOT NULL COMMENT '특징 배열 (예: ["짧은 문장", "~요 체"])',
    sample_text     TEXT         NOT NULL COMMENT '사용자가 입력한 샘플 텍스트',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_blog_tone_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 블로그 포스트
CREATE TABLE IF NOT EXISTS sm_blog_post (
    post_id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    subject         VARCHAR(300) NOT NULL COMMENT '글 주제',
    content         MEDIUMTEXT            COMMENT 'AI 생성된 HTML 본문',
    media_urls      JSON                  COMMENT '첨부 미디어 URL 배열',
    additional_info TEXT                  COMMENT '사용자 추가 정보',
    platform        VARCHAR(20)  NOT NULL DEFAULT 'naver' COMMENT 'naver | tistory',
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT 'draft | generating | ready | published | scheduled | error',
    error_message   VARCHAR(500)          COMMENT '오류 메시지',
    published_url   VARCHAR(500)          COMMENT '발행된 글 URL',
    scheduled_at    DATETIME              COMMENT '예약 발행 시각',
    published_at    DATETIME              COMMENT '실제 발행 시각',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blog_post_member (member_id),
    INDEX idx_blog_post_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
