# Crown DB 스키마 문서

> 데이터베이스: `crown_db` / MySQL 8.x
> 최종 업데이트: 2026-04-08

---

## 1. member — 회원

Google OAuth 로그인 사용자 정보. 오목 게임 점수도 포함.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| member_id | bigint | NO | PK | auto | 회원 PK |
| google_id | varchar(100) | NO | UNI | | Google OAuth UID |
| nickname | varchar(50) | NO | | | 닉네임 |
| email | varchar(100) | NO | | | 이메일 |
| profile_img | varchar(500) | YES | | | 프로필 이미지 URL |
| score | int | NO | | 1000 | 오목 게임 레이팅 |
| win_count | int | NO | | 0 | 승리 횟수 |
| lose_count | int | NO | | 0 | 패배 횟수 |
| draw_count | int | NO | | 0 | 무승부 횟수 |
| fcm_token | varchar(512) | YES | | | FCM 푸시 토큰 |
| created_at | datetime | NO | | NOW | 가입일 |
| updated_at | datetime | NO | | NOW | 수정일 |

---

## 2. sm_project — 영상/문서 프로젝트

사용자가 생성한 Shorts/PDF/PPT 프로젝트.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| project_id | bigint | NO | PK | auto | 프로젝트 PK |
| member_id | bigint | NO | FK | | 소유자 (member.member_id) |
| category | varchar(20) | NO | | stock | 카테고리 (stock/korea/crypto_top/macro_news) |
| template_id | varchar(30) | NO | | dark_blue | 사용된 템플릿 ID |
| options | json | YES | | | 생성 옵션 (output_type, slide_count 등) |
| title | varchar(200) | YES | | | 프로젝트 제목 |
| script | json | YES | | | 대본 (슬라이드별 텍스트) |
| html_url | varchar(500) | YES | | | S3 HTML 파일 URL |
| thumbnail_url | varchar(500) | YES | | | 썸네일 이미지 URL |
| status | varchar(20) | NO | | draft | draft/generating/done/error |
| video_url | varchar(500) | YES | | | 렌더링된 영상 URL |
| created_at | datetime | NO | | NOW | 생성일 |
| updated_at | datetime | NO | | NOW | 수정일 |

---

## 3. sm_job — 렌더링 작업 큐

Python Worker에 전달되는 영상/HTML 생성 작업.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| job_id | bigint | NO | PK | auto | 작업 PK |
| project_id | bigint | NO | FK | | 대상 프로젝트 (sm_project.project_id) |
| status | varchar(20) | NO | | pending | pending/running/done/error |
| error_message | text | YES | | | 실패 시 에러 메시지 |
| started_at | datetime | YES | | | 작업 시작 시각 |
| finished_at | datetime | YES | | | 작업 완료 시각 |

---

## 4. sm_color_theme_template — 색상 테마 템플릿

8 레이아웃 x 4 색상 = 32개 시스템 템플릿 + 사용자 커스텀 템플릿.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| template_id | int | NO | PK | auto | 템플릿 PK |
| name | varchar(255) | NO | | | 템플릿명 (예: Hero - Blue Bold) |
| description | text | YES | | | 템플릿 설명 |
| layout | varchar(50) | NO | IDX | | hero/magazine/dashboard/card_grid/split/minimal/colorful/photo_bg |
| color_theme | varchar(50) | NO | IDX | | blue_bold/emerald_growth/rose_bold/purple_minimal |
| accent | varchar(7) | NO | | | 주요 강조 색상 (#hex) |
| highlight | varchar(7) | NO | | | 밝은 하이라이트 색상 |
| bg | varchar(7) | NO | | | 배경색 |
| text_color | varchar(7) | NO | | | 텍스트 색상 |
| circle1 | varchar(7) | NO | | | 배경 원형 1 색상 |
| circle2 | varchar(7) | NO | | | 배경 원형 2 색상 |
| font_family | varchar(255) | NO | | | 폰트 패밀리 |
| google_fonts_url | varchar(500) | YES | | | Google Fonts 임포트 URL |
| is_system | tinyint(1) | YES | IDX | 1 | true=기본, false=사용자 커스텀 |
| created_by_user_id | varchar(255) | YES | IDX | | 커스텀 시 Firebase UID |
| is_public | tinyint(1) | YES | | 0 | 공개 템플릿 여부 |
| preview_html | longtext | YES | | | HTML 미리보기 |
| usage_count | int | YES | | 0 | 사용 횟수 |
| created_at | timestamp | YES | | NOW | 생성일 |
| updated_at | timestamp | YES | | NOW | 수정일 |

---

## 5. sm_template — 기본 템플릿 (레거시)

video/ppt 타입별 기본 템플릿. config JSON으로 스타일 저장. (sm_color_theme_template으로 이관 중)

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | int | NO | PK | auto | 템플릿 PK |
| type | varchar(20) | NO | | video | video/ppt |
| name | varchar(100) | NO | | | 템플릿명 |
| description | varchar(255) | YES | | | 설명 |
| thumbnail_url | varchar(500) | YES | | | 썸네일 URL |
| config | json | YES | | | 스타일 설정 (색상, 폰트 등) |
| is_active | tinyint(1) | NO | | 1 | 활성화 여부 |
| sort_order | int | NO | | 0 | 정렬 순서 |
| created_at | datetime | NO | | NOW | 생성일 |

---

## 6. sm_question — 프로젝트 생성 설문

카테고리별 생성 옵션 질문 (Step 4 세부 설정).

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| question_id | int | NO | PK | auto | 질문 PK |
| category | varchar(20) | NO | | stock | 카테고리 |
| group_name | varchar(20) | NO | | | 그룹 (intro/content/outro) |
| type | varchar(20) | NO | | | text/single/multi/number |
| key_name | varchar(50) | NO | | | 응답 키 |
| label | varchar(100) | NO | | | 질문 라벨 |
| description | varchar(200) | YES | | | 부가 설명 |
| options_json | json | YES | | | 선택지 [{value, label}] |
| default_val | varchar(200) | YES | | | 기본값 |
| min_val | int | YES | | | 최소값 (number 타입) |
| max_val | int | YES | | | 최대값 (number 타입) |
| sort_order | int | NO | | 0 | 정렬 순서 |
| required | tinyint(1) | NO | | 0 | 필수 여부 |

---

## 7. sm_script_history — 대본 히스토리

프로젝트 대본 수정 이력.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| history_id | bigint | NO | PK | auto | 히스토리 PK |
| project_id | bigint | NO | FK | | 프로젝트 (sm_project.project_id) |
| member_id | bigint | NO | | | 수정한 사용자 |
| script | json | NO | | | 대본 스냅샷 |
| note | varchar(200) | YES | | | 변경 메모 |
| created_at | datetime | NO | | NOW | 저장일 |

---

## 8. sm_schedule — 배치 스케줄

사용자별 자동 생성 스케줄 설정.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | int | NO | PK | auto | 스케줄 PK |
| member_id | int | NO | | | 소유자 |
| name | varchar(100) | NO | | 새 스케줄 | 스케줄명 |
| category | varchar(20) | NO | | stock | 카테고리 |
| topic | varchar(200) | YES | | | 주제 |
| template | varchar(50) | YES | | | 템플릿 ID |
| keywords | json | YES | | | 키워드 목록 |
| frequency | varchar(20) | NO | | daily | daily/weekly/monthly |
| run_time | varchar(5) | NO | | 09:00 | 실행 시각 (HH:mm) |
| run_days | json | YES | | | 실행 요일 (weekly일 때) |
| is_active | tinyint(1) | NO | | 1 | 활성화 여부 |
| last_run_at | datetime | YES | | | 마지막 실행 시각 |
| next_run_at | datetime | YES | | | 다음 실행 예정 시각 |
| created_at | datetime | NO | | NOW | 생성일 |
| updated_at | datetime | NO | | NOW | 수정일 |

---

## 9. sm_sfx — 효과음 라이브러리

S3에 저장된 효과음 파일 메타데이터.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | int | NO | PK | auto | 효과음 PK |
| name | varchar(200) | NO | | | 파일명 |
| tags | varchar(500) | YES | | [] | 태그 (JSON 배열) |
| duration | decimal(7,2) | YES | | 0.00 | 재생 시간 (초) |
| s3_url | varchar(1000) | NO | | | S3 URL |
| source | varchar(100) | YES | | upload | 출처 (upload/crawl) |
| created_at | datetime | YES | | NOW | 등록일 |

---

## 10. sm_bgm — 배경음악 라이브러리

S3에 저장된 배경음악 파일 메타데이터.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | int | NO | PK | auto | BGM PK |
| name | varchar(200) | NO | | | 파일명 |
| tags | varchar(500) | YES | | [] | 태그 (JSON 배열) |
| duration | decimal(7,2) | YES | | 0.00 | 재생 시간 (초) |
| s3_url | varchar(1000) | NO | | | S3 URL |
| source | varchar(100) | YES | | upload | 출처 |
| created_at | datetime | YES | | NOW | 등록일 |

---

## 11. sm_planning — 기획/로드맵

내부 기능 기획 관리 (어드민 전용).

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 기획 PK |
| title | varchar(200) | NO | | | 기능 제목 |
| description | text | YES | | | 상세 설명 |
| category | varchar(50) | YES | | | 영상에디터/배치자동화/CCR/UI/인프라/기타 |
| status | varchar(30) | YES | | 아이디어 | 아이디어/기획중/확정/진행중/완료/보류 |
| priority | tinyint | YES | | 3 | 1=긴급, 2=높음, 3=보통, 4=낮음 |
| target_date | date | YES | | | 목표 일자 |
| source | varchar(30) | YES | | manual | manual/proposal/ccr |
| proposal_id | bigint | YES | | | 연결된 제안 (sm_feature_roadmap.id) |
| created_at | datetime | YES | | NOW | 생성일 |
| updated_at | datetime | YES | | NOW | 수정일 |

---

## 12. sm_dev_task — 개발 태스크

sm_planning 하위의 구체적 개발 작업.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 태스크 PK |
| planning_id | bigint | NO | | | 상위 기획 (sm_planning.id) |
| title | varchar(200) | NO | | | 태스크 제목 |
| description | text | YES | | | 상세 설명 |
| category | varchar(30) | YES | | | frontend/backend/python/db/infra |
| status | varchar(30) | YES | | 대기 | 대기/진행중/완료/보류 |
| priority | tinyint | YES | | 3 | 우선순위 |
| estimated_hours | decimal(5,1) | YES | | | 예상 소요 시간 |
| actual_hours | decimal(5,1) | YES | | | 실제 소요 시간 |
| auto_assignable | tinyint(1) | YES | | 0 | CCR 자동 작업 가능 여부 |
| due_date | date | YES | | | 마감일 |
| completed_at | datetime | YES | | | 완료일 |
| created_at | datetime | YES | | NOW | 생성일 |
| updated_at | datetime | YES | | NOW | 수정일 |

---

## 13. sm_feature_roadmap — 기능 제안

AI 또는 수동으로 제안된 기능 목록.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 제안 PK |
| proposal_date | date | YES | | | 제안일 |
| feature_name | varchar(200) | NO | | | 기능명 |
| implementation_desc | text | YES | | | 구현 설명 |
| difficulty | varchar(20) | YES | | | 쉬움/보통/어려움 |
| auto_developable | varchar(10) | YES | | 불가능 | 가능/불가능 |
| reference_service | varchar(100) | YES | | | 참고 서비스 |
| expected_effect | text | YES | | | 기대 효과 |
| priority | tinyint | YES | | 3 | 우선순위 |
| status | varchar(30) | YES | | 검토중 | 검토중/개발중/개발완료/배포완료/기획이관/보류 |
| branch | varchar(100) | YES | | | Git 브랜치명 |
| created_at | datetime | YES | | NOW | 생성일 |
| updated_at | datetime | YES | | NOW | 수정일 |

---

## 14. sm_usage_log — 사용 로그

회원별 주요 액션 기록.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 로그 PK |
| member_id | bigint | NO | FK | | 회원 (member.member_id) |
| action | varchar(50) | NO | | GENERATE | 액션 (GENERATE/RENDER/EXPORT 등) |
| created_at | datetime | NO | | NOW | 발생일 |

---

## 15. access_log — API 접근 로그

모든 API 요청 기록 (어드민 대시보드용).

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| log_id | bigint | NO | PK | auto | 로그 PK |
| member_id | bigint | YES | FK | | 요청 회원 (비로그인 시 NULL) |
| path | varchar(200) | NO | | | API 경로 |
| method | varchar(10) | NO | | GET | HTTP 메서드 |
| status_code | int | YES | | | 응답 코드 |
| duration_ms | int | YES | | | 처리 시간 (ms) |
| ip_address | varchar(50) | YES | | | 클라이언트 IP |
| created_at | datetime | NO | IDX | NOW | 요청 시각 |

---

## 16. error_log — 에러 로그

서버/워커 에러 기록 (어드민 에러 페이지용).

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| log_id | bigint | NO | PK | auto | 에러 PK |
| source | varchar(20) | NO | | | 발생 위치 (api/worker/frontend) |
| level | varchar(10) | NO | | ERROR | ERROR/WARN |
| path | varchar(200) | YES | | | API 경로 |
| project_id | bigint | YES | | | 관련 프로젝트 |
| member_id | bigint | YES | | | 관련 회원 |
| message | text | NO | | | 에러 메시지 |
| stack_trace | text | YES | | | 스택 트레이스 |
| created_at | datetime | NO | | NOW | 발생일 |

---

## 17. announcement — 공지사항

FCM 푸시로 발송된 공지.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 공지 PK |
| title | varchar(200) | NO | | | 제목 |
| message | text | NO | | | 본문 |
| sent_count | int | YES | | 0 | 발송 건수 |
| created_at | timestamp | YES | | NOW | 발송일 |

---

## 18. inquiry — 고객 문의

사용자 문의 내역.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| inquiry_id | bigint | NO | PK | auto | 문의 PK |
| member_id | bigint | YES | FK | | 문의자 (비로그인 시 NULL) |
| title | varchar(200) | NO | | | 제목 |
| content | text | NO | | | 내용 |
| status | varchar(20) | NO | | pending | pending/answered/closed |
| created_at | datetime | NO | | NOW | 접수일 |

---

## 19. friend — 친구 관계

회원 간 친구 요청/수락.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| id | bigint | NO | PK | auto | 관계 PK |
| requester_id | bigint | NO | FK | | 요청자 (member.member_id) |
| receiver_id | bigint | NO | FK | | 수신자 (member.member_id) |
| status | varchar(20) | NO | | PENDING | PENDING/ACCEPTED/DECLINED |
| created_at | datetime | YES | | NOW | 요청일 |

---

## 20. game_room — 오목 게임방

오목 대전 방 정보.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| room_id | bigint | NO | PK | auto | 방 PK |
| black_member_id | bigint | NO | FK | | 흑돌 플레이어 |
| white_member_id | bigint | NO | FK | | 백돌 플레이어 |
| status | varchar(20) | NO | | IN_PROGRESS | IN_PROGRESS/FINISHED/DRAW |
| winner_id | bigint | YES | FK | | 승자 |
| black_score_change | int | YES | | | 흑돌 레이팅 변동 |
| white_score_change | int | YES | | | 백돌 레이팅 변동 |
| created_at | datetime | NO | | NOW | 시작일 |
| finished_at | datetime | YES | | | 종료일 |

---

## 21. game_move — 오목 수 기록

게임 내 각 수(手) 기록.

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|----|--------|------|
| move_id | bigint | NO | PK | auto | 수 PK |
| room_id | bigint | NO | FK | | 게임방 (game_room.room_id) |
| member_id | bigint | NO | FK | | 착수자 |
| move_x | tinyint | NO | | | X 좌표 |
| move_y | tinyint | NO | | | Y 좌표 |
| move_order | int | NO | | | 몇 번째 수 |
| created_at | datetime | NO | | NOW | 착수 시각 |

---

## 테이블 관계도

```
member ─┬─< sm_project ──< sm_job
        │       └──< sm_script_history
        ├─< sm_schedule
        ├─< sm_usage_log
        ├─< access_log
        ├─< inquiry
        ├─< friend (requester/receiver)
        ├─< game_room (black/white)
        └─< sm_color_theme_template (created_by_user_id)

sm_planning ──< sm_dev_task
sm_feature_roadmap ──> sm_planning (proposal_id)

game_room ──< game_move
```
