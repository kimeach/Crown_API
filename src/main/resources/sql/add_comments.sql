-- ============================================================
-- crown_db 전체 테이블/컬럼 코멘트 추가
-- 실행: mysql -u root -p crown_db < add_comments.sql
-- ============================================================

-- ── 1. member ──────────────────────────────────────────────
ALTER TABLE member COMMENT = '회원 (Google OAuth 로그인)';
ALTER TABLE member
  MODIFY member_id bigint NOT NULL AUTO_INCREMENT COMMENT '회원 PK',
  MODIFY google_id varchar(100) NOT NULL COMMENT 'Google OAuth UID (Firebase)',
  MODIFY nickname varchar(50) NOT NULL COMMENT '닉네임',
  MODIFY email varchar(100) NOT NULL COMMENT '이메일',
  MODIFY profile_img varchar(500) DEFAULT NULL COMMENT '프로필 이미지 URL',
  MODIFY score int NOT NULL DEFAULT 1000 COMMENT '오목 게임 레이팅',
  MODIFY win_count int NOT NULL DEFAULT 0 COMMENT '승리 횟수',
  MODIFY lose_count int NOT NULL DEFAULT 0 COMMENT '패배 횟수',
  MODIFY draw_count int NOT NULL DEFAULT 0 COMMENT '무승부 횟수',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
  MODIFY updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  MODIFY fcm_token varchar(512) DEFAULT NULL COMMENT 'FCM 푸시 토큰';

-- ── 2. sm_project ──────────────────────────────────────────
ALTER TABLE sm_project COMMENT = '영상/문서 프로젝트 (Shorts, PDF, PPT)';
ALTER TABLE sm_project
  MODIFY project_id bigint NOT NULL AUTO_INCREMENT COMMENT '프로젝트 PK',
  MODIFY member_id bigint NOT NULL COMMENT '소유자 (member.member_id)',
  MODIFY category varchar(20) NOT NULL DEFAULT 'stock' COMMENT '카테고리 (stock/korea/crypto_top/macro_news)',
  MODIFY template_id varchar(30) NOT NULL DEFAULT 'dark_blue' COMMENT '사용된 템플릿 ID',
  MODIFY options json DEFAULT NULL COMMENT '생성 옵션 JSON (output_type, slide_count 등)',
  MODIFY title varchar(200) DEFAULT NULL COMMENT '프로젝트 제목',
  MODIFY script json DEFAULT NULL COMMENT '대본 (슬라이드별 텍스트 JSON)',
  MODIFY html_url varchar(500) DEFAULT NULL COMMENT 'S3 HTML 파일 URL',
  MODIFY thumbnail_url varchar(500) DEFAULT NULL COMMENT '썸네일 이미지 URL',
  MODIFY status varchar(20) NOT NULL DEFAULT 'draft' COMMENT '상태 (draft/generating/done/error)',
  MODIFY video_url varchar(500) DEFAULT NULL COMMENT '렌더링된 영상 URL',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  MODIFY updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일';

-- ── 3. sm_job ──────────────────────────────────────────────
ALTER TABLE sm_job COMMENT = '렌더링 작업 큐 (Python Worker 처리)';
ALTER TABLE sm_job
  MODIFY job_id bigint NOT NULL AUTO_INCREMENT COMMENT '작업 PK',
  MODIFY project_id bigint NOT NULL COMMENT '대상 프로젝트 (sm_project.project_id)',
  MODIFY status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '상태 (pending/running/done/error)',
  MODIFY error_message text DEFAULT NULL COMMENT '실패 시 에러 메시지',
  MODIFY started_at datetime DEFAULT NULL COMMENT '작업 시작 시각',
  MODIFY finished_at datetime DEFAULT NULL COMMENT '작업 완료 시각';

-- ── 4. sm_color_theme_template (이미 일부 존재) ────────────
ALTER TABLE sm_color_theme_template COMMENT = '색상 테마 템플릿 (시스템 32개 + 사용자 커스텀)';
ALTER TABLE sm_color_theme_template
  MODIFY template_id int NOT NULL AUTO_INCREMENT COMMENT '템플릿 PK';

-- ── 5. sm_template ─────────────────────────────────────────
ALTER TABLE sm_template COMMENT = '기본 템플릿 (레거시, sm_color_theme_template으로 이관 중)';
ALTER TABLE sm_template
  MODIFY id int NOT NULL AUTO_INCREMENT COMMENT '템플릿 PK',
  MODIFY type varchar(20) NOT NULL DEFAULT 'video' COMMENT '타입 (video/ppt)',
  MODIFY name varchar(100) NOT NULL COMMENT '템플릿명',
  MODIFY description varchar(255) DEFAULT NULL COMMENT '설명',
  MODIFY thumbnail_url varchar(500) DEFAULT NULL COMMENT '썸네일 URL',
  MODIFY config json DEFAULT NULL COMMENT '스타일 설정 JSON (색상, 폰트 등)',
  MODIFY is_active tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
  MODIFY sort_order int NOT NULL DEFAULT 0 COMMENT '정렬 순서',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일';

-- ── 6. sm_question ─────────────────────────────────────────
ALTER TABLE sm_question COMMENT = '프로젝트 생성 설문 (카테고리별 옵션 질문)';
ALTER TABLE sm_question
  MODIFY question_id int NOT NULL AUTO_INCREMENT COMMENT '질문 PK',
  MODIFY category varchar(20) NOT NULL DEFAULT 'stock' COMMENT '카테고리',
  MODIFY group_name varchar(20) NOT NULL COMMENT '그룹 (intro/content/outro)',
  MODIFY type varchar(20) NOT NULL COMMENT '입력 타입 (text/single/multi/number)',
  MODIFY key_name varchar(50) NOT NULL COMMENT '응답 키 (options JSON에 매핑)',
  MODIFY label varchar(100) NOT NULL COMMENT '질문 라벨 (UI 표시)',
  MODIFY description varchar(200) DEFAULT NULL COMMENT '부가 설명',
  MODIFY options_json json DEFAULT NULL COMMENT '선택지 [{value, label}]',
  MODIFY default_val varchar(200) DEFAULT NULL COMMENT '기본값',
  MODIFY min_val int DEFAULT NULL COMMENT '최소값 (number 타입)',
  MODIFY max_val int DEFAULT NULL COMMENT '최대값 (number 타입)',
  MODIFY sort_order int NOT NULL DEFAULT 0 COMMENT '정렬 순서',
  MODIFY required tinyint(1) NOT NULL DEFAULT 0 COMMENT '필수 여부';

-- ── 7. sm_script_history ───────────────────────────────────
ALTER TABLE sm_script_history COMMENT = '대본 수정 히스토리';
ALTER TABLE sm_script_history
  MODIFY history_id bigint NOT NULL AUTO_INCREMENT COMMENT '히스토리 PK',
  MODIFY project_id bigint NOT NULL COMMENT '프로젝트 (sm_project.project_id)',
  MODIFY member_id bigint NOT NULL COMMENT '수정한 사용자 (member.member_id)',
  MODIFY script json NOT NULL COMMENT '대본 스냅샷 JSON',
  MODIFY note varchar(200) DEFAULT NULL COMMENT '변경 메모',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '저장일';

-- ── 8. sm_schedule ─────────────────────────────────────────
ALTER TABLE sm_schedule COMMENT = '자동 생성 배치 스케줄';
ALTER TABLE sm_schedule
  MODIFY id int NOT NULL AUTO_INCREMENT COMMENT '스케줄 PK',
  MODIFY member_id int NOT NULL COMMENT '소유자 (member.member_id)',
  MODIFY name varchar(100) NOT NULL DEFAULT '새 스케줄' COMMENT '스케줄명',
  MODIFY category varchar(20) NOT NULL DEFAULT 'stock' COMMENT '카테고리',
  MODIFY topic varchar(200) DEFAULT NULL COMMENT '주제',
  MODIFY template varchar(50) DEFAULT NULL COMMENT '템플릿 ID',
  MODIFY keywords json DEFAULT NULL COMMENT '키워드 목록 JSON',
  MODIFY frequency varchar(20) NOT NULL DEFAULT 'daily' COMMENT '주기 (daily/weekly/monthly)',
  MODIFY run_time varchar(5) NOT NULL DEFAULT '09:00' COMMENT '실행 시각 (HH:mm)',
  MODIFY run_days json DEFAULT NULL COMMENT '실행 요일 JSON (weekly일 때)',
  MODIFY is_active tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
  MODIFY last_run_at datetime DEFAULT NULL COMMENT '마지막 실행 시각',
  MODIFY next_run_at datetime DEFAULT NULL COMMENT '다음 실행 예정 시각',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  MODIFY updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일';

-- ── 9. sm_sfx ──────────────────────────────────────────────
ALTER TABLE sm_sfx COMMENT = '효과음 라이브러리 (S3)';
ALTER TABLE sm_sfx
  MODIFY id int NOT NULL AUTO_INCREMENT COMMENT '효과음 PK',
  MODIFY name varchar(200) NOT NULL COMMENT '파일명',
  MODIFY tags varchar(500) DEFAULT '[]' COMMENT '태그 (JSON 배열)',
  MODIFY duration decimal(7,2) DEFAULT 0.00 COMMENT '재생 시간 (초)',
  MODIFY s3_url varchar(1000) NOT NULL COMMENT 'S3 URL',
  MODIFY source varchar(100) DEFAULT 'upload' COMMENT '출처 (upload/crawl)',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '등록일';

-- ── 10. sm_bgm ─────────────────────────────────────────────
ALTER TABLE sm_bgm COMMENT = '배경음악 라이브러리 (S3)';
ALTER TABLE sm_bgm
  MODIFY id int NOT NULL AUTO_INCREMENT COMMENT 'BGM PK',
  MODIFY name varchar(200) NOT NULL COMMENT '파일명',
  MODIFY tags varchar(500) DEFAULT '[]' COMMENT '태그 (JSON 배열)',
  MODIFY duration decimal(7,2) DEFAULT 0.00 COMMENT '재생 시간 (초)',
  MODIFY s3_url varchar(1000) NOT NULL COMMENT 'S3 URL',
  MODIFY source varchar(100) DEFAULT 'upload' COMMENT '출처 (upload/crawl)',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '등록일';

-- ── 11. sm_planning ────────────────────────────────────────
ALTER TABLE sm_planning COMMENT = '기획/로드맵 관리 (어드민)';
ALTER TABLE sm_planning
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '기획 PK',
  MODIFY title varchar(200) NOT NULL COMMENT '기능 제목',
  MODIFY description text DEFAULT NULL COMMENT '상세 설명',
  MODIFY target_date date DEFAULT NULL COMMENT '목표 일자',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  MODIFY updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일';

-- ── 12. sm_dev_task ────────────────────────────────────────
ALTER TABLE sm_dev_task COMMENT = '개발 태스크 (sm_planning 하위)';
ALTER TABLE sm_dev_task
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '태스크 PK',
  MODIFY title varchar(200) NOT NULL COMMENT '태스크 제목',
  MODIFY description text DEFAULT NULL COMMENT '상세 설명',
  MODIFY priority tinyint DEFAULT 3 COMMENT '우선순위 (1=긴급 ~ 4=낮음)',
  MODIFY estimated_hours decimal(5,1) DEFAULT NULL COMMENT '예상 소요 시간',
  MODIFY actual_hours decimal(5,1) DEFAULT NULL COMMENT '실제 소요 시간',
  MODIFY due_date date DEFAULT NULL COMMENT '마감일',
  MODIFY completed_at datetime DEFAULT NULL COMMENT '완료일',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  MODIFY updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일';

-- ── 13. sm_feature_roadmap ─────────────────────────────────
ALTER TABLE sm_feature_roadmap COMMENT = '기능 제안 (AI/수동)';
ALTER TABLE sm_feature_roadmap
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '제안 PK',
  MODIFY proposal_date date DEFAULT NULL COMMENT '제안일',
  MODIFY feature_name varchar(200) NOT NULL COMMENT '기능명',
  MODIFY implementation_desc text DEFAULT NULL COMMENT '구현 설명',
  MODIFY reference_service varchar(100) DEFAULT NULL COMMENT '참고 서비스',
  MODIFY expected_effect text DEFAULT NULL COMMENT '기대 효과',
  MODIFY priority tinyint DEFAULT 3 COMMENT '우선순위 (1=긴급 ~ 4=낮음)',
  MODIFY branch varchar(100) DEFAULT NULL COMMENT 'Git 브랜치명',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  MODIFY updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일';

-- ── 14. sm_usage_log ───────────────────────────────────────
ALTER TABLE sm_usage_log COMMENT = '회원 사용 로그';
ALTER TABLE sm_usage_log
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
  MODIFY member_id bigint NOT NULL COMMENT '회원 (member.member_id)',
  MODIFY action varchar(50) NOT NULL DEFAULT 'GENERATE' COMMENT '액션 (GENERATE/RENDER/EXPORT 등)',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생일';

-- ── 15. access_log ─────────────────────────────────────────
ALTER TABLE access_log COMMENT = 'API 접근 로그 (어드민 대시보드)';
ALTER TABLE access_log
  MODIFY log_id bigint NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
  MODIFY member_id bigint DEFAULT NULL COMMENT '요청 회원 (비로그인 시 NULL)',
  MODIFY path varchar(200) NOT NULL COMMENT 'API 경로',
  MODIFY method varchar(10) NOT NULL DEFAULT 'GET' COMMENT 'HTTP 메서드',
  MODIFY status_code int DEFAULT NULL COMMENT 'HTTP 응답 코드',
  MODIFY duration_ms int DEFAULT NULL COMMENT '처리 시간 (ms)',
  MODIFY ip_address varchar(50) DEFAULT NULL COMMENT '클라이언트 IP',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '요청 시각';

-- ── 16. error_log ──────────────────────────────────────────
ALTER TABLE error_log COMMENT = '에러 로그 (어드민 에러 페이지)';
ALTER TABLE error_log
  MODIFY log_id bigint NOT NULL AUTO_INCREMENT COMMENT '에러 PK',
  MODIFY source varchar(20) NOT NULL COMMENT '발생 위치 (api/worker/frontend)',
  MODIFY level varchar(10) NOT NULL DEFAULT 'ERROR' COMMENT '레벨 (ERROR/WARN)',
  MODIFY path varchar(200) DEFAULT NULL COMMENT 'API 경로',
  MODIFY project_id bigint DEFAULT NULL COMMENT '관련 프로젝트 (sm_project.project_id)',
  MODIFY member_id bigint DEFAULT NULL COMMENT '관련 회원 (member.member_id)',
  MODIFY message text NOT NULL COMMENT '에러 메시지',
  MODIFY stack_trace text DEFAULT NULL COMMENT '스택 트레이스',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생일';

-- ── 17. announcement ───────────────────────────────────────
ALTER TABLE announcement COMMENT = '공지사항 (FCM 푸시 발송)';
ALTER TABLE announcement
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '공지 PK',
  MODIFY title varchar(200) NOT NULL COMMENT '제목',
  MODIFY message text NOT NULL COMMENT '본문',
  MODIFY sent_count int DEFAULT 0 COMMENT '발송 건수',
  MODIFY created_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '발송일';

-- ── 18. inquiry ────────────────────────────────────────────
ALTER TABLE inquiry COMMENT = '고객 문의';
ALTER TABLE inquiry
  MODIFY inquiry_id bigint NOT NULL AUTO_INCREMENT COMMENT '문의 PK',
  MODIFY member_id bigint DEFAULT NULL COMMENT '문의자 (비로그인 시 NULL)',
  MODIFY title varchar(200) NOT NULL COMMENT '제목',
  MODIFY content text NOT NULL COMMENT '내용',
  MODIFY status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '상태 (pending/answered/closed)',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '접수일';

-- ── 19. friend ─────────────────────────────────────────────
ALTER TABLE friend COMMENT = '친구 관계 (요청/수락)';
ALTER TABLE friend
  MODIFY id bigint NOT NULL AUTO_INCREMENT COMMENT '관계 PK',
  MODIFY requester_id bigint NOT NULL COMMENT '요청자 (member.member_id)',
  MODIFY receiver_id bigint NOT NULL COMMENT '수신자 (member.member_id)',
  MODIFY status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '상태 (PENDING/ACCEPTED/DECLINED)',
  MODIFY created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '요청일';

-- ── 20. game_room ──────────────────────────────────────────
ALTER TABLE game_room COMMENT = '오목 게임방';
ALTER TABLE game_room
  MODIFY room_id bigint NOT NULL AUTO_INCREMENT COMMENT '방 PK',
  MODIFY black_member_id bigint NOT NULL COMMENT '흑돌 플레이어 (member.member_id)',
  MODIFY white_member_id bigint NOT NULL COMMENT '백돌 플레이어 (member.member_id)',
  MODIFY status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '상태 (IN_PROGRESS/FINISHED/DRAW)',
  MODIFY winner_id bigint DEFAULT NULL COMMENT '승자 (member.member_id)',
  MODIFY black_score_change int DEFAULT NULL COMMENT '흑돌 레이팅 변동',
  MODIFY white_score_change int DEFAULT NULL COMMENT '백돌 레이팅 변동',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작일',
  MODIFY finished_at datetime DEFAULT NULL COMMENT '종료일';

-- ── 21. game_move ──────────────────────────────────────────
ALTER TABLE game_move COMMENT = '오목 수(手) 기록';
ALTER TABLE game_move
  MODIFY move_id bigint NOT NULL AUTO_INCREMENT COMMENT '수 PK',
  MODIFY room_id bigint NOT NULL COMMENT '게임방 (game_room.room_id)',
  MODIFY member_id bigint NOT NULL COMMENT '착수자 (member.member_id)',
  MODIFY move_x tinyint NOT NULL COMMENT 'X 좌표',
  MODIFY move_y tinyint NOT NULL COMMENT 'Y 좌표',
  MODIFY move_order int NOT NULL COMMENT '몇 번째 수',
  MODIFY created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '착수 시각';
