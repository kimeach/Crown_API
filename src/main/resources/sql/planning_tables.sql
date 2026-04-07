-- 기획 테이블
CREATE TABLE IF NOT EXISTS sm_planning (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(200) NOT NULL,
  description  TEXT,
  category     VARCHAR(50)  COMMENT '영상에디터/배치자동화/CCR/UI/인프라/기타',
  status       VARCHAR(30)  DEFAULT '아이디어' COMMENT '아이디어/기획중/확정/진행중/완료/보류',
  priority     TINYINT      DEFAULT 3 COMMENT '1=긴급 2=높음 3=보통 4=낮음',
  target_date  DATE,
  source       VARCHAR(30)  DEFAULT 'manual' COMMENT 'manual/proposal/ccr',
  proposal_id  BIGINT       COMMENT 'sm_feature_roadmap.id (제안에서 올린 경우)',
  created_at   DATETIME     DEFAULT NOW(),
  updated_at   DATETIME     DEFAULT NOW() ON UPDATE NOW()
);

-- 개발 태스크 테이블
CREATE TABLE IF NOT EXISTS sm_dev_task (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  planning_id      BIGINT       NOT NULL COMMENT 'sm_planning.id',
  title            VARCHAR(200) NOT NULL,
  description      TEXT,
  category         VARCHAR(30)  COMMENT 'frontend/backend/python/db/infra',
  status           VARCHAR(30)  DEFAULT '대기' COMMENT '대기/진행중/완료/보류',
  priority         TINYINT      DEFAULT 3,
  estimated_hours  DECIMAL(5,1),
  actual_hours     DECIMAL(5,1),
  auto_assignable  TINYINT(1)   DEFAULT 0 COMMENT 'CCR 자동 작업 가능',
  due_date         DATE,
  completed_at     DATETIME,
  created_at       DATETIME     DEFAULT NOW(),
  updated_at       DATETIME     DEFAULT NOW() ON UPDATE NOW()
);
