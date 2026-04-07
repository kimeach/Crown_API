-- ============================================================
-- sm_planning 초기 데이터 (기획 항목)
-- Phase 10 미완료 + 알려진 버그/개선 항목
-- ============================================================

-- 완료된 Phase 기록 (히스토리)
INSERT INTO sm_planning (title, description, category, status, priority, source, created_at) VALUES
('Phase 1~4: DB + Python 워커 + Crown API + 파이프라인', 'DB 설계, FastAPI 워커, Java Crown API, 영상 파이프라인 기초 구축', '인프라', '완료', 3, 'manual', '2025-01-01'),
('Phase 5: Next.js 프론트엔드 기본', '대시보드, 에디터 기본 UI, Firebase 인증 연동', 'UI', '완료', 3, 'manual', '2025-02-01'),
('Phase 6~8: VideoEditor 고도화 + 삽입/서식 탭', 'TTS 프리뷰, 오버레이, 컬러팔레트, 서식 탭 전체 구현', '영상에디터', '완료', 3, 'manual', '2025-04-01'),
('Phase 9: PDF/PPT 에디터 완성', 'Playwright export, python-pptx, AI PPT 생성 파이프라인', '영상에디터', '완료', 3, 'manual', '2025-06-01'),
('Phase 10: 리팩토링 + 기획 기능 확장', 'verticalAlign 버그 수정, SSR localStorage, as any 제거, 해시태그/SEO/품질 AI 기능', '영상에디터', '완료', 3, 'manual', '2025-09-01');

-- 현재 진행 중 / 예정 항목
INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES
('기능 로드맵 어드민 페이지', '기획제안/기획/개발 3탭 관리, 도넛차트, 대시보드 위젯 — 현재 구현 완료, DB 마이그레이션 필요', '배치자동화', '완료', 2, 'manual'),
('CCR 자동화 시스템', 'claude -p 비대화형 자동 실행, 슬랙 알림, #velona-ideas 채널 명령 처리', '배치자동화', '진행중', 2, 'manual'),
('Cafe24 운영 서버 배포', '1GB RAM Cafe24 서버에 Python 워커 + Crown API + Next.js 배포 완성', '인프라', '아이디어', 1, 'manual');

-- Phase 10 미완료 기능 (외부 API 불필요)
INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES
('썸네일 자동 생성', 'Playwright로 첫 슬라이드 캡처 → S3 저장 → DB thumbnail_url 업데이트 (파이프라인 자동 처리)', '영상에디터', '완료', 3, 'manual'),
('트렌딩 토픽 제안', 'yfinance + RSS 크롤링으로 실시간 인기 주제 제안, 정적 폴백 포함 /trending/topics', '배치자동화', '완료', 3, 'manual'),
('프로젝트 복제', '대시보드에서 복제 버튼 — HTML+대본 복사, 영상 미포함', 'UI', '완료', 4, 'manual'),
('버전 히스토리 (대본 이력)', 'sm_script_history 테이블, 대본 저장 시 자동 스냅샷, 복원 API', 'UI', '완료', 4, 'manual'),
('AI 자동 해시태그 생성', 'Gemini 호출 → YouTube 해시태그 15개 자동 생성 /ai/hashtags', '영상에디터', '완료', 4, 'manual'),
('AI SEO 최적화', 'Gemini 호출 → 제목/설명/태그 최적화 /ai/seo', '영상에디터', '완료', 4, 'manual'),
('AI 영상 품질 피드백', 'Gemini 호출 → 0-100점 + 강점/개선점 /ai/quality', '영상에디터', '완료', 4, 'manual');

-- 미완료 영상 편집기 기능 (에디터 내 incomplete 항목)
INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES
('타임라인 편집기 구현', '에디터 하단 타임라인 패널 — 클립 드래그 순서 변경, 구간 편집 (현재 버튼만 있고 패널 없음)', '영상에디터', '기획중', 2, 'manual'),
('목소리 목록 동적 로딩', '에디터 효과 탭에서 /api/admin/voices 실제 데이터 로딩 (현재 하드코딩 3종)', '영상에디터', '아이디어', 3, 'manual'),
('SRT 자막 에디터', '에디터 내 자막 직접 편집 패널 — 현재 버튼만 있고 실제 편집기 없음', '영상에디터', '기획중', 2, 'manual'),
('목소리 녹음 기능', 'MediaRecorder API — 마이크 녹음 후 클립 오디오로 삽입 (삽입탭에 UI 있으나 저장 연동 미완)', '영상에디터', '아이디어', 4, 'manual');

-- 외부 API 필요 (보류)
INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES
('YouTube 직접 업로드', 'YouTube Data API v3 연동으로 에디터에서 바로 업로드', '영상에디터', '보류', 4, 'manual'),
('무료 이미지 검색 삽입', 'Unsplash / Pexels API 연동 — 삽입탭 이미지 검색', '영상에디터', '보류', 4, 'manual'),
('AI 이미지 자동 삽입', 'Gemini Imagen API — 대본 기반 이미지 자동 생성', '영상에디터', '보류', 3, 'manual'),
('결제/구독 플랜', '토스페이먼츠 또는 아임포트 연동, 플랜별 기능 제한', 'UI', '보류', 2, 'manual'),
('팀 협업 기초 구조', 'DB 팀/멤버 테이블 + 초대 링크, 공동 편집', 'UI', '보류', 3, 'manual');

-- 보안 이슈
INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES
('DB 자격증명 하드코딩 제거', 'error_logger.py, admin.py에 DB 비밀번호 하드코딩 → 환경변수로 교체 필요 (보안)', '인프라', '확정', 1, 'manual');

-- ============================================================
-- sm_dev_task 초기 데이터 (주요 계획별 태스크)
-- planning_id는 위 INSERT 순서 기준 — 실제 ID 확인 후 조정 필요
-- ============================================================

-- CCR 자동화 시스템 태스크 (planning_id는 실제 ID 조회 후 입력)
-- INSERT INTO sm_dev_task (planning_id, title, category, status, priority, estimated_hours, auto_assignable) VALUES
-- (8, 'ccr-run.sh 슬랙 알림 통합', 'infra', '완료', 2, 1.0, 0),
-- (8, '#velona-ideas 채널 명령 처리 (ccr-ideas-task.md)', 'infra', '진행중', 2, 2.0, 0),
-- (8, 'session.active 락 파일 충돌 방지', 'infra', '완료', 1, 0.5, 0);
