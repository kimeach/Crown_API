-- 기능 로드맵 테이블
CREATE TABLE IF NOT EXISTS sm_feature_roadmap (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  proposal_date       DATE         NOT NULL,
  feature_name        VARCHAR(200) NOT NULL,
  reference_service   VARCHAR(100),
  implementation_desc TEXT,
  difficulty          VARCHAR(20)  COMMENT '낮음/중간/높음',
  estimated_time      VARCHAR(50),
  priority            VARCHAR(20),
  auto_developable    TINYINT(1)   DEFAULT 0,
  auto_dev_reason     VARCHAR(500),
  status              VARCHAR(50)  DEFAULT '검토중' COMMENT '검토중/개발중/개발완료/배포완료/보류',
  branch              VARCHAR(200),
  created_at          DATETIME     DEFAULT NOW(),
  updated_at          DATETIME     DEFAULT NOW() ON UPDATE NOW()
);
