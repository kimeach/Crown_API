# Crown API - 프로젝트 가이드

## 프로젝트 개요
**CrownGames** 플랫폼의 백엔드 API 서버.
오목 등 여러 게임 앱의 공통 백엔드로 사용. 구글 플레이스토어 출시 목표.

## 기술 스택
- **Java 17**
- **Spring Boot 3.2.4**
- **Gradle**
- **MyBatis**
- **MySQL**
- **Spring Security + Firebase Authentication**
- **WebSocket (STOMP)**
- **RestTemplate** (Python 워커 HTTP 통신)

## 패키지 구조
```
com.crown/
├── common/
│   ├── config/
│   │   ├── AppConfig.java            # RestTemplate, ObjectMapper 빈 등록
│   │   ├── FirebaseConfig.java       # Firebase 앱 2개 초기화
│   │   ├── MybatisConfig.java        # @MapperScan("com.crown.*.mapper")
│   │   ├── SecurityConfig.java       # Firebase JWT 검증, CORS 설정
│   │   └── WebSocketConfig.java      # STOMP /ws, /ws-native 엔드포인트
│   ├── dto/
│   │   └── ApiResponse.java          # 공통 응답 래퍼 {success, message, data}
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   └── typehandler/
│       ├── JsonMapTypeHandler.java    # JSON ↔ Map<String,String>
│       └── JsonObjectTypeHandler.java # JSON ↔ Map<String,Object>
├── member/
│   ├── restcontroller/
│   │   ├── MemberRestController.java  # POST /api/member/login, GET /api/member/me
│   │   └── RankingRestController.java # GET /api/ranking
│   ├── service/MemberService.java
│   ├── serviceimpl/MemberServiceImpl.java
│   ├── dao/MemberDao.java
│   ├── mapper/MemberMapper.java
│   └── dto/
│       ├── MemberDto.java
│       └── FirebaseAttributes.java
├── omok/
│   ├── restcontroller/
│   │   ├── OmokRestController.java        # GET /api/omok/history, /api/omok/room/{id}
│   │   └── OmokWebSocketController.java   # STOMP 핸들러 전체
│   ├── service/
│   │   ├── OmokService.java
│   │   ├── MatchingService.java
│   │   └── EloService.java
│   ├── serviceimpl/
│   │   ├── OmokServiceImpl.java
│   │   ├── MatchingServiceImpl.java       # 인메모리 대기열 (ArrayDeque)
│   │   └── EloServiceImpl.java            # K=32 ELO 점수제
│   ├── dao/OmokDao.java
│   ├── mapper/OmokMapper.java
│   └── dto/
│       ├── GameRoomDto.java
│       ├── GameMoveDto.java
│       ├── ChallengeDto.java              # 친구 대전 신청용
│       └── MatchMessage.java              # type: MATCHED|MOVE|WIN|DRAW|SURRENDER|UNDO_*|FRIEND_CHALLENGE*|REMATCH_*|ERROR
├── friend/
│   ├── restcontroller/FriendRestController.java  # /api/friend/** 엔드포인트
│   ├── service/FriendService.java
│   ├── serviceimpl/FriendServiceImpl.java
│   ├── dao/FriendDao.java
│   ├── mapper/FriendMapper.java
│   └── dto/FriendDto.java
└── shorts/
    ├── restcontroller/ShortsRestController.java   # /api/shorts/** 엔드포인트
    ├── service/ShortsService.java
    ├── serviceimpl/ShortsServiceImpl.java         # Python 워커 HTTP 호출
    ├── dao/ShortsDao.java
    ├── mapper/ShortsMapper.java
    └── dto/
        ├── ProjectDto.java   # templateId, options, script(JSON) 포함
        ├── JobDto.java
        └── QuestionDto.java  # 카테고리별 설문 질문
```

## 레이어 구조 규칙
새 기능 추가 시 반드시 아래 구조를 따를 것:
```
RestController → Service (interface) → ServiceImpl → Dao (interface) → Mapper → Mapper.xml
```
- Service는 항상 인터페이스 / 구현체 분리
- Dao는 Mapper를 주입받아 사용
- Mapper XML은 `src/main/resources/mapper/{패키지명}/` 에 위치

## DB 스키마
`src/main/resources/schema.sql` 참고

| 테이블 | 설명 |
|--------|------|
| `member` | 회원 (ELO score 기본값 1000, win/lose/draw_count) |
| `game_room` | 게임방 (status: IN_PROGRESS / FINISHED / ABANDONED) |
| `game_move` | 착수 기록 (리플레이용, move_x/y/order) |
| `friend` | 친구 관계 (status: PENDING / ACCEPTED) — **schema.sql 미포함, 별도 실행 필요** |
| `sm_project` | ShortsMaker 프로젝트 (template_id, options 컬럼 추가됨 — **schema.sql과 불일치**) |
| `sm_job` | ShortsMaker 영상 생성 잡 |
| `sm_question` | 카테고리별 설문 질문 — **schema.sql 미포함, 별도 실행 필요** |

### schema.sql에 누락된 테이블 (직접 실행 필요)
```sql
-- 친구 관계
CREATE TABLE IF NOT EXISTS friend (
    friend_id    BIGINT   AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT   NOT NULL,
    receiver_id  BIGINT   NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACCEPTED
    created_at   DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uq_friend_pair (requester_id, receiver_id),
    FOREIGN KEY (requester_id) REFERENCES member(member_id),
    FOREIGN KEY (receiver_id)  REFERENCES member(member_id)
);

-- ShortsMaker 설문 질문
CREATE TABLE IF NOT EXISTS sm_question (
    question_id  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    category     VARCHAR(20) NOT NULL,
    group_name   VARCHAR(50),
    type         VARCHAR(20) NOT NULL,  -- text, select, range, toggle
    key_name     VARCHAR(50) NOT NULL,
    label        VARCHAR(100) NOT NULL,
    description  VARCHAR(200),
    options_json JSON,
    default_val  VARCHAR(100),
    min_val      INT,
    max_val      INT,
    sort_order   INT NOT NULL DEFAULT 0,
    required     TINYINT(1) NOT NULL DEFAULT 0
);

-- sm_project 컬럼 추가 (기존 테이블에 없는 경우)
ALTER TABLE sm_project
    ADD COLUMN template_id VARCHAR(50) DEFAULT 'dark_blue' AFTER category,
    ADD COLUMN options     JSON                             AFTER template_id;
```

## WebSocket 흐름 (STOMP)
```
연결:    /ws (SockJS), /ws-native (순수 WebSocket)
발행:    /app/omok/match              → 랜덤 매칭 요청 (memberId 전송)
         /app/omok/match/cancel       → 매칭 취소
         /app/omok/move               → 착수 (GameMoveDto)
         /app/omok/win                → 승리 선언
         /app/omok/surrender          → 기권
         /app/omok/undo-request       → 무르기 요청
         /app/omok/undo-response      → 무르기 응답 (moveX=1:수락, 0:거절)
         /app/omok/challenge          → 친구 대전 신청 (ChallengeDto)
         /app/omok/challenge/accept   → 친구 대전 수락
         /app/omok/challenge/reject   → 친구 대전 거절
         /app/omok/rematch-request    → 재도전 요청
         /app/omok/rematch-response   → 재도전 응답 (moveX=1:수락, 0:거절) — 수락 시 흑백 교체

구독:    /topic/omok/room/{roomId}    → 게임 진행 중 메시지
         /topic/omok/user/{memberId}  → 개인 알림 (매칭 완료, 친구 대전 신청 등)

메시지 타입: MATCHED | MOVE | WIN | DRAW | SURRENDER | ERROR
             UNDO_REQUEST | UNDO_ACCEPT | UNDO_REJECT
             FRIEND_CHALLENGE | FRIEND_CHALLENGE_REJECT
             REMATCH_REQUEST | REMATCH_MATCHED | REMATCH_REJECTED
```

## REST API

### Member
| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/member/login` | Firebase 로그인 후 회원 생성/갱신 | 필요 |
| GET | `/api/member/me` | 내 정보 조회 | 필요 |
| GET | `/api/ranking?limit=100` | 랭킹 조회 | 불필요 |

### Omok
| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/omok/history` | 내 대전 기록 | 필요 |
| GET | `/api/omok/room/{roomId}` | 방 정보 조회 | 필요 |

### Friend
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/friend/list` | 내 친구 목록 |
| GET | `/api/friend/requests` | 받은 친구 요청 목록 |
| POST | `/api/friend/request/{targetId}` | 친구 요청 전송 |
| POST | `/api/friend/accept/{requesterId}` | 친구 요청 수락 |
| DELETE | `/api/friend/{friendId}` | 친구 삭제 / 요청 거절 |

### ShortsMaker
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/shorts/questions?category=stock` | 카테고리별 설문 질문 |
| POST | `/api/shorts/projects/generate` | 데이터 수집 + 대본 + HTML 생성 요청 |
| POST | `/api/shorts/projects/blank` | 빈 프로젝트 생성 |
| GET | `/api/shorts/projects` | 내 프로젝트 목록 |
| GET | `/api/shorts/projects/{id}` | 프로젝트 상세 |
| PUT | `/api/shorts/projects/{id}/script` | 대본 수정 저장 |
| PUT | `/api/shorts/projects/{id}/html` | HTML 저장 (워커 경유) |
| PATCH | `/api/shorts/projects/{id}/title` | 제목 변경 |
| DELETE | `/api/shorts/projects/{id}` | 프로젝트 삭제 (S3 파일 포함) |
| POST | `/api/shorts/projects/{id}/render` | 영상 생성 시작 |
| GET | `/api/shorts/jobs/{id}/status` | 잡 상태 폴링 |
| POST | `/api/shorts/tts-preview` | TTS 미리 듣기 (audio/mpeg 스트리밍) |
| POST | `/api/shorts/projects/{id}/ai/rewrite` | AI 대본 재작성 |
| POST | `/api/shorts/projects/{id}/ai/translate` | AI 대본 번역 |
| POST | `/api/shorts/projects/{id}/subtitle/script` | 대본 → SRT 자막 생성 |
| POST | `/api/shorts/projects/{id}/subtitle/video` | 영상 → Whisper 자막 생성 |
| POST | `/api/shorts/voice/clone` | 목소리 복제 생성 (multipart) |
| GET | `/api/shorts/voice/list` | 사용 가능한 목소리 목록 |
| DELETE | `/api/shorts/voice/{voiceId}` | 목소리 복제 삭제 |
| POST | `/api/shorts/projects/{id}/upload-asset` | 클립 미디어(이미지/영상) S3 업로드 |
| PUT | `/api/shorts/internal/generate-callback/{projectId}` | Python 워커 → 생성 완료 콜백 (인증 불필요) |
| PUT | `/api/shorts/internal/render-callback/{jobId}` | Python 워커 → 렌더 완료 콜백 (인증 불필요) |

## 인증
- **Firebase Authentication** 사용 (Google 로그인)
- Firebase 프로젝트 2개 동시 운영:
  - `CrownGames` → `firebase-service-account.json` (게임 앱용)
  - `velona-ai` → `firebase-service-account2.json` (ShortsMaker용)
- Flutter/Next.js에서 Firebase ID 토큰 발급 → `Authorization: Bearer <idToken>` 헤더로 전송
- `FirebaseTokenFilter`가 토큰 검증 → `FirebaseToken`을 SecurityContext에 저장
- 인증 불필요 경로: `/ws/**`, `/ws-native/**`, `/api/ranking`, `/api/shorts/internal/**`

## CORS 설정
- 허용 Origin: `http://localhost:3000` (Next.js 개발 서버)
- 운영 배포 시 `SecurityConfig.java`의 `allowedOrigins`를 실제 도메인으로 변경 필요

## ELO 점수제
- 기본 점수: 1000
- K factor: 32
- 승리 시 점수 증가 / 패배 시 점수 감소 / 무승부 시 소폭 조정
- `EloServiceImpl.java` 참고

## MyBatis TypeHandler
| TypeHandler | 변환 | 용도 |
|-------------|------|------|
| `JsonMapTypeHandler` | JSON ↔ `Map<String,String>` | `sm_project.script` |
| `JsonObjectTypeHandler` | JSON ↔ `Map<String,Object>` | `sm_project.options` |

- `application.yml`에 `type-handlers-package: com.crown.common.typehandler` 등록됨
- Mapper XML resultMap에 반드시 `typeHandler` 명시할 것

## Python 워커 연동 (ShortsMaker)
- 워커 주소: `http://localhost:8003` (`application.yml` 기준, `worker.url`)
- 인증: `X-Worker-Secret` 헤더 (`application.yml`의 `worker.secret`)
- 콜백 베이스: `app.base-url` (`http://localhost:8080`)

### 워커 API 엔드포인트 (Crown API → 워커)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/generate/stock` | 데이터 수집 + 대본 + HTML 생성 |
| POST | `/render` | 영상 렌더링 |
| POST | `/html/save` | 수정된 HTML 저장 |
| POST | `/tts/preview` | TTS 미리 듣기 |
| POST | `/ai/rewrite` | AI 대본 재작성 |
| POST | `/ai/translate` | AI 대본 번역 |
| POST | `/subtitle/from-script` | 대본 → SRT 자막 |
| POST | `/subtitle/from-video` | 영상 → Whisper 자막 |
| POST | `/voice/clone` | 목소리 복제 생성 |
| GET | `/voice/list` | 목소리 목록 조회 |
| DELETE | `/voice/{voiceId}` | 목소리 복제 삭제 |
| POST | `/storage/upload` | 클립 미디어 S3 업로드 |
| DELETE | `/storage/project/{projectId}` | 프로젝트 S3 파일 전체 삭제 |

## 게임 추가 방법
새 게임 추가 시 `com.crown.{게임명}` 패키지 생성 후 동일한 레이어 구조로 구성.
Mapper XML은 `src/main/resources/mapper/{게임명}/` 에 추가.

## 연관 프로젝트
| 프로젝트 | 경로 | 설명 |
|---------|------|------|
| Crown API (백엔드) | `C:\work\Crown_API` | 현재 프로젝트 |
| omok_flutter (앱) | `C:\work\omok_flutter` | 오목 Flutter 앱 |
| Velona AI (Next.js) | `C:\work\Velonaai` | ShortsMaker 프론트엔드 |
| Python Worker | `C:\work\python-worker` | 영상 생성 워커 (port 8003) |

## 시작 전 필요한 설정
1. MySQL에 `crown_db` 스키마 생성 (`schema.sql` 실행)
2. `schema.sql`에 없는 테이블 별도 실행 (위 "schema.sql에 누락된 테이블" 참고)
3. Firebase 서비스 계정 JSON 2개 위치:
   - `src/main/resources/firebase-service-account.json` (CrownGames)
   - `src/main/resources/firebase-service-account2.json` (velona-ai)
4. `application.yml` DB 비밀번호 및 `worker.secret` 확인

---

## 구현 현황 및 TODO

### 완료된 작업
- [x] Member: Firebase 인증, 로그인/회원정보/랭킹 API
- [x] Omok: 랜덤 매칭, 착수/승리/기권, ELO 점수 계산
- [x] Omok: 무르기 요청/수락/거절 (UNDO_REQUEST / UNDO_ACCEPT / UNDO_REJECT)
- [x] Omok: 친구 대전 신청/수락/거절 (FRIEND_CHALLENGE / FRIEND_CHALLENGE_REJECT)
- [x] Omok: 재도전 요청/수락/거절, 흑백 교체 (REMATCH_*)
- [x] Friend: 친구 목록, 요청 전송/수락/거절/삭제
- [x] ShortsMaker: 프로젝트 CRUD (생성/조회/수정/삭제)
- [x] ShortsMaker: Python 워커 연동 (generate/render 콜백 분리)
- [x] ShortsMaker: TTS 미리 듣기 (오디오 스트리밍)
- [x] ShortsMaker: AI 대본 재작성 / 번역
- [x] ShortsMaker: 자막 생성 (대본→SRT, 영상→Whisper)
- [x] ShortsMaker: 목소리 복제 (생성/목록/삭제)
- [x] ShortsMaker: 클립 미디어 S3 업로드
- [x] ShortsMaker: 설문 질문 조회 (sm_question 테이블)
- [x] 공통 JsonMapTypeHandler / JsonObjectTypeHandler
- [x] CORS 설정 (localhost:3000 허용)
- [x] 내부 콜백 경로 인증 제외 (`/api/shorts/internal/**`)

### 미완료 / TODO
- [ ] **schema.sql 동기화**: `friend`, `sm_question` 테이블 및 `sm_project` 컬럼 추가 내용 반영
- [ ] **WebSocket 인증**: 현재 `/ws/**` 전체 인증 없이 허용 — STOMP CONNECT 헤더에서 토큰 검증 필요
- [ ] **OmokController.java 제거**: `com.crown.omok.controller.OmokController`가 빈 클래스로 존재, 삭제 필요
- [ ] **친구 온라인 상태 표시**: 친구가 접속 중인지 WebSocket 세션 기반으로 확인하는 기능
- [ ] **친구 대전 중 상태 표시**: 친구가 게임 중인지 여부 표시
- [ ] **무르기 실제 처리**: 현재 무르기 요청/응답 메시지만 릴레이, 실제 마지막 착수 DB 삭제 로직 없음
- [ ] **운영 CORS 설정**: `localhost:3000` → 실제 도메인으로 변경
- [ ] **운영 worker.secret 보안**: 환경변수로 분리 (현재 `application.yml`에 평문)
- [ ] **rate limiting**: API 남용 방지 (특히 AI 재작성, TTS 등 비용 발생 엔드포인트)
- [ ] **ShortsMaker 카테고리 확장**: 현재 `stock` 카테고리 중심, `beauty`/`tech` 워커 연동 확인 필요
- [ ] **Push 알림**: 친구 요청, 친구 대전 신청 시 FCM 푸시 알림 (앱 백그라운드 상태 대응)
- [ ] **대전 기록 상세**: 리플레이용 game_move 조회 API 미구현
- [ ] **회원 탈퇴**: 회원 삭제 API 없음
- [ ] **sm_project thumbnail**: 영상 썸네일 URL 저장 컬럼 고려
