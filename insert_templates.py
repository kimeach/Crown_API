#!/usr/bin/env python3
"""
나머지 26개 템플릿 DB에 추가하는 스크립트
테이블: sm_template (crown_db)
"""

import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "crown_db")

# 32개 템플릿 정의 (기본 6개는 이미 존재)
TEMPLATES = [
    # Hero (template_01 ~ 04)
    {"name": "Hero - Blue Bold", "layout": "hero", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Hero - Emerald Growth", "layout": "hero", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Hero - Rose Bold", "layout": "hero", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Hero - Purple Minimal", "layout": "hero", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Magazine (template_05 ~ 08)
    {"name": "Magazine - Blue Bold", "layout": "magazine", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Magazine - Emerald Growth", "layout": "magazine", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Magazine - Rose Bold", "layout": "magazine", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Magazine - Purple Minimal", "layout": "magazine", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Dashboard (template_09 ~ 12)
    {"name": "Dashboard - Blue Bold", "layout": "dashboard", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Dashboard - Emerald Growth", "layout": "dashboard", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Dashboard - Rose Bold", "layout": "dashboard", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Dashboard - Purple Minimal", "layout": "dashboard", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Card Grid (template_13 ~ 16)
    {"name": "Card Grid - Blue Bold", "layout": "card_grid", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Card Grid - Emerald Growth", "layout": "card_grid", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Card Grid - Rose Bold", "layout": "card_grid", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Card Grid - Purple Minimal", "layout": "card_grid", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Split Layout (template_17 ~ 20)
    {"name": "Split Layout - Blue Bold", "layout": "split", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Split Layout - Emerald Growth", "layout": "split", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Split Layout - Rose Bold", "layout": "split", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Split Layout - Purple Minimal", "layout": "split", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Minimal (template_21 ~ 24)
    {"name": "Minimal - Blue Bold", "layout": "minimal", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Minimal - Emerald Growth", "layout": "minimal", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Minimal - Rose Bold", "layout": "minimal", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Minimal - Purple Minimal", "layout": "minimal", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Colorful (template_25 ~ 28)
    {"name": "Colorful Accent - Blue Bold", "layout": "colorful", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Colorful Accent - Emerald Growth", "layout": "colorful", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Colorful Accent - Rose Bold", "layout": "colorful", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Colorful Accent - Purple Minimal", "layout": "colorful", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},

    # Photo Background (template_29 ~ 32)
    {"name": "Photo Background - Blue Bold", "layout": "photo_bg", "color": "blue_bold", "accent": "#3B82F6", "highlight": "#FFD700", "bg": "#020617", "text": "#e5e7eb", "circle1": "#1e3a8a", "circle2": "#1e1b4b"},
    {"name": "Photo Background - Emerald Growth", "layout": "photo_bg", "color": "emerald_growth", "accent": "#10b981", "highlight": "#34d399", "bg": "#0f172a", "text": "#e0e7ff", "circle1": "#064e3b", "circle2": "#052e16"},
    {"name": "Photo Background - Rose Bold", "layout": "photo_bg", "color": "rose_bold", "accent": "#ef4444", "highlight": "#fca5a5", "bg": "#1a1a2e", "text": "#f3f4f6", "circle1": "#7f1d1d", "circle2": "#450a0a"},
    {"name": "Photo Background - Purple Minimal", "layout": "photo_bg", "color": "purple_minimal", "accent": "#a855f7", "highlight": "#e879f9", "bg": "#1e1b4b", "text": "#e9d5ff", "circle1": "#6b21a8", "circle2": "#3b0764"},
]

def insert_templates():
    try:
        conn = pymysql.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME,
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        FONT_FAMILY = "'Pretendard', sans-serif"
        GOOGLE_FONTS = "https://fonts.googleapis.com/css2?family=Pretendard:wght@400;700;900&display=swap"

        # 각 템플릿 INSERT
        for tpl in TEMPLATES:
            sql = """
            INSERT INTO sm_template
            (name, layout, color_theme, accent, highlight, bg, text_color, circle1, circle2, font_family, google_fonts_url, is_system, usage_count)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 1, 0)
            """
            values = (
                tpl["name"],
                tpl["layout"],
                tpl["color"],
                tpl["accent"],
                tpl["highlight"],
                tpl["bg"],
                tpl["text"],
                tpl["circle1"],
                tpl["circle2"],
                FONT_FAMILY,
                GOOGLE_FONTS
            )
            cursor.execute(sql, values)
            print(f"✅ {tpl['name']} 삽입")

        conn.commit()
        cursor.close()
        conn.close()

        # 최종 확인
        conn = pymysql.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME,
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM sm_template")
        count = cursor.fetchone()[0]
        print(f"\n🎉 최종 템플릿 개수: {count}개")
        cursor.close()
        conn.close()

    except Exception as e:
        print(f"❌ 오류: {e}")

if __name__ == "__main__":
    insert_templates()
