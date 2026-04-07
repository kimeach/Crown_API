-- 색상 테마 템플릿 테이블 (기본 32개 + 사용자 커스텀)
CREATE TABLE IF NOT EXISTS sm_color_theme_template (
  template_id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL COMMENT '템플릿명 (예: Hero - Blue Bold)',
  description TEXT COMMENT '템플릿 설명',

  -- 레이아웃/색상 분류
  layout VARCHAR(50) NOT NULL COMMENT 'hero, magazine, dashboard, card_grid, split, minimal, colorful, photo_bg',
  color_theme VARCHAR(50) NOT NULL COMMENT 'blue_bold, emerald_growth, rose_bold, purple_minimal',

  -- 색상값 (16진수)
  accent VARCHAR(7) NOT NULL COMMENT '주요 강조 색상',
  highlight VARCHAR(7) NOT NULL COMMENT '밝은 하이라이트 색상',
  bg VARCHAR(7) NOT NULL COMMENT '배경색',
  text_color VARCHAR(7) NOT NULL COMMENT '텍스트 색상',
  circle1 VARCHAR(7) NOT NULL COMMENT '배경 원형 1 색상',
  circle2 VARCHAR(7) NOT NULL COMMENT '배경 원형 2 색상',

  -- 폰트 정의
  font_family VARCHAR(255) NOT NULL COMMENT '폰트 패밀리 (예: Pretendard, sans-serif)',
  google_fonts_url VARCHAR(500) COMMENT 'Google Fonts 임포트 URL',

  -- 구분자
  is_system BOOLEAN DEFAULT true COMMENT 'true=기본템플릿, false=사용자커스텀',
  created_by_user_id VARCHAR(255) COMMENT '커스텀 템플릿인 경우 사용자 ID',
  is_public BOOLEAN DEFAULT false COMMENT '다른 사용자도 사용 가능한 공개 템플릿인지',

  -- 메타데이터
  preview_html LONGTEXT COMMENT 'HTML 미리보기 (선택사항)',
  usage_count INT DEFAULT 0 COMMENT '이 템플릿으로 생성된 프로젝트 수',

  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_system (is_system),
  INDEX idx_user (created_by_user_id),
  INDEX idx_layout (layout),
  INDEX idx_color_theme (color_theme)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='템플릿 관리 (기본+사용자커스텀)';
