-- 기본 템플릿 32개 (모든 컬럼 명시)
-- 테이블명: sm_color_theme_template
-- 컬럼: template_id, name, description, layout, color_theme, accent, highlight, bg, text_color, circle1, circle2, font_family, google_fonts_url, is_system, created_by_user_id, is_public, preview_html, usage_count, created_at, updated_at

INSERT INTO crown_db.sm_color_theme_template
(name, description, layout, color_theme, accent, highlight, bg, text_color, circle1, circle2, font_family, google_fonts_url, is_system, created_by_user_id, is_public, usage_count)
VALUES
-- Hero 레이아웃 (template_01 ~ 04)
('Hero - Blue Bold', '큰 제목 + 배경 이미지, 진중하고 신뢰감 있는 네이비 골드', 'hero', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Hero - Emerald Growth', '큰 제목 + 배경 이미지, 안정적이고 성장을 상징하는 그린', 'hero', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Hero - Rose Bold', '큰 제목 + 배경 이미지, 강렬하고 임팩트 있는 레드', 'hero', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Hero - Purple Minimal', '큰 제목 + 배경 이미지, 세련되고 프리미엄한 보라', 'hero', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Magazine 레이아웃 (template_05 ~ 08)
('Magazine - Blue Bold', '3단 그리드 + 텍스트, 진중하고 신뢰감 있는 네이비 골드', 'magazine', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Magazine - Emerald Growth', '3단 그리드 + 텍스트, 안정적이고 성장을 상징하는 그린', 'magazine', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Magazine - Rose Bold', '3단 그리드 + 텍스트, 강렬하고 임팩트 있는 레드', 'magazine', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Magazine - Purple Minimal', '3단 그리드 + 텍스트, 세련되고 프리미엄한 보라', 'magazine', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Data Dashboard 레이아웃 (template_09 ~ 12)
('Dashboard - Blue Bold', '차트/수치 중심, 진중하고 신뢰감 있는 네이비 골드', 'dashboard', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Dashboard - Emerald Growth', '차트/수치 중심, 안정적이고 성장을 상징하는 그린', 'dashboard', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Dashboard - Rose Bold', '차트/수치 중심, 강렬하고 임팩트 있는 레드', 'dashboard', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Dashboard - Purple Minimal', '차트/수치 중심, 세련되고 프리미엄한 보라', 'dashboard', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Card Grid 레이아웃 (template_13 ~ 16)
('Card Grid - Blue Bold', '카드 나열, 진중하고 신뢰감 있는 네이비 골드', 'card_grid', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Card Grid - Emerald Growth', '카드 나열, 안정적이고 성장을 상징하는 그린', 'card_grid', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Card Grid - Rose Bold', '카드 나열, 강렬하고 임팩트 있는 레드', 'card_grid', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Card Grid - Purple Minimal', '카드 나열, 세련되고 프리미엄한 보라', 'card_grid', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Split Layout (template_17 ~ 20)
('Split Layout - Blue Bold', '좌우 분할, 진중하고 신뢰감 있는 네이비 골드', 'split', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Split Layout - Emerald Growth', '좌우 분할, 안정적이고 성장을 상징하는 그린', 'split', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Split Layout - Rose Bold', '좌우 분할, 강렬하고 임팩트 있는 레드', 'split', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Split Layout - Purple Minimal', '좌우 분할, 세련되고 프리미엄한 보라', 'split', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Minimal 레이아웃 (template_21 ~ 24)
('Minimal - Blue Bold', '여백 + 타이포, 진중하고 신뢰감 있는 네이비 골드', 'minimal', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Minimal - Emerald Growth', '여백 + 타이포, 안정적이고 성장을 상징하는 그린', 'minimal', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Minimal - Rose Bold', '여백 + 타이포, 강렬하고 임팩트 있는 레드', 'minimal', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Minimal - Purple Minimal', '여백 + 타이포, 세련되고 프리미엄한 보라', 'minimal', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Colorful Accent 레이아웃 (template_25 ~ 28)
('Colorful Accent - Blue Bold', '컬러 블록, 진중하고 신뢰감 있는 네이비 골드', 'colorful', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Colorful Accent - Emerald Growth', '컬러 블록, 안정적이고 성장을 상징하는 그린', 'colorful', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Colorful Accent - Rose Bold', '컬러 블록, 강렬하고 임팩트 있는 레드', 'colorful', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Colorful Accent - Purple Minimal', '컬러 블록, 세련되고 프리미엄한 보라', 'colorful', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),

-- Photo Background 레이아웃 (template_29 ~ 32)
('Photo Background - Blue Bold', '사진 배경, 진중하고 신뢰감 있는 네이비 골드', 'photo_bg', 'blue_bold', '#3B82F6', '#FFD700', '#020617', '#e5e7eb', '#1e3a8a', '#1e1b4b', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Photo Background - Emerald Growth', '사진 배경, 안정적이고 성장을 상징하는 그린', 'photo_bg', 'emerald_growth', '#10b981', '#34d399', '#0f172a', '#e0e7ff', '#064e3b', '#052e16', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Photo Background - Rose Bold', '사진 배경, 강렬하고 임팩트 있는 레드', 'photo_bg', 'rose_bold', '#ef4444', '#fca5a5', '#1a1a2e', '#f3f4f6', '#7f1d1d', '#450a0a', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0),
('Photo Background - Purple Minimal', '사진 배경, 세련되고 프리미엄한 보라', 'photo_bg', 'purple_minimal', '#a855f7', '#e879f9', '#1e1b4b', '#e9d5ff', '#6b21a8', '#3b0764', "'Pretendard', sans-serif", 'https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900', 1, NULL, 0, 0);
