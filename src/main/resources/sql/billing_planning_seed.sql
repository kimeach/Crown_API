-- ============================================================
-- 빌링 + 토큰 + 친구초대 기획/개발 태스크
-- 실행: MySQL crown_db
-- 생성일: 2026-04-09
-- ============================================================

-- ════════════════════════════════════════════════════════════
-- sm_planning (기획 테이블)
-- ════════════════════════════════════════════════════════════

INSERT INTO sm_planning (title, description, category, status, priority, source) VALUES

-- Phase 0: DB 스키마 정비
('Phase B-0: 빌링 DB 스키마 정비',
 'sm_subscription ENUM 9개 확장(plus/max/team_pro/team_max/biz_pro/biz_plus/biz_max), category/member_count/trial_ends_at 컬럼 추가, sm_payment 환불 컬럼, sm_billing_audit 감사 테이블, sm_referral 초대 추적 테이블, sm_referral_reward 보상 설정 테이블, sm_feature_cost 기능별 토큰 비용 테이블, member 테이블 referral_code/referred_by 컬럼',
 '인프라', '확정', 1, 'manual'),

-- Phase 1: 핵심 버그 수정
('Phase B-1: 빌링 핵심 버그 수정',
 'processDueBillings() 토큰 충전 누락 수정, PlanLimits.java 4개→sm_plan_config DB기반 전환, /billing/success 페이지 activateSubscription() 실제 호출, /api/billing/plans SecurityConfig permitAll 추가',
 'UI', '확정', 1, 'manual'),

-- Phase 2: AI 기능 토큰 차감
('Phase B-2: AI 기능 토큰 차감 (16개 기능)',
 'sm_feature_cost 테이블 연동, AI 리라이트(3토큰)/번역(3)/해시태그(1)/SEO(1)/품질분석(1)/TTS(1)/보이스클로닝(5)/자막생성(2~3)/자막번역(2)/PPT생성(5)/PDF내보내기(2)/PPTX내보내기(2)/영상내보내기(3)/블로그톤분석(1)/블로그생성(5) 차감 적용, 실패 시 환불 로직',
 '영상에디터', '확정', 2, 'manual'),

-- Phase 3: 친구 초대
('Phase B-3: 친구 초대 시스템',
 '초대 코드 생성/검증, 가입 시 쌍방 50토큰 보상, 유료 플랜 구독 시 추가 보상(Pro +100~Biz +500), 초대 랜딩 페이지(/invite/[code]), 대시보드 초대 카드(코드 복사/카카오톡 공유), 초대 관리 페이지(이력/통계), FCM 알림',
 'UI', '확정', 2, 'manual'),

-- Phase 4: 플랜 업그레이드/다운그레이드
('Phase B-4: 플랜 업그레이드/다운그레이드',
 '비례 정산 계산 로직(남은 일수 기반 차액), 업그레이드 견적 API + 즉시 결제 + 토큰 조정, 다운그레이드 다음 결제일 예약 방식, 플랜 변경 확인 모달 + 견적 표시 UI, sm_billing_audit 기록',
 'UI', '기획중', 2, 'manual'),

-- Phase 5: 환불
('Phase B-5: 환불 시스템',
 'Toss 환불 API 연동(POST /v1/payments/{paymentKey}/cancel), 7일 이내 환불 자격 검증, 토큰 회수 로직, 환불 엔드포인트(요청/상태조회), 결제 이력에서 환불 요청 UI, sm_payment 환불 컬럼 활용',
 'UI', '기획중', 2, 'manual'),

-- Phase 6: 팀/비즈니스 전환
('Phase B-6: 팀/비즈니스 카테고리 전환',
 '개인→팀 전환(sm_team 자동 생성, 멤버 수 입력, per-member 과금), 팀→비즈니스(min 5명 검증), 팀/비즈→개인(멤버 해산 필수, 공유 프로젝트 해제 경고), 체크아웃 시 멤버 수 슬라이더, sm_subscription.member_count 관리',
 'UI', '기획중', 2, 'manual'),

-- Phase 7: 프론트엔드 빌링 UI 완성
('Phase B-7: 프론트엔드 빌링/토큰 UI 완성',
 'dashboard/usage 토큰 잔액 실제 API 연동(Mock 제거), dashboard/billing 구독 조회/취소/카드 변경 API 연동, settings/subscription 전체 연동, 에디터 헤더 잔여 토큰 뱃지 + 부족 경고 모달, 토큰 사용 이력 페이지 신규',
 'UI', '기획중', 3, 'manual'),

-- Phase 8: 어드민 + 보안
('Phase B-8: 어드민 토큰 관리 + 보안 강화',
 '어드민 토큰 통계(발행/소비/유저별), 수동 보너스 토큰 지급, 토스 웹훅 시크릿 검증, 내부 콜백 X-Worker-Secret 검증, 결제 실패 3회 재시도 + FCM 알림, sm_billing_audit 조회 UI',
 '인프라', '아이디어', 3, 'manual');


-- ════════════════════════════════════════════════════════════
-- sm_dev_task (개발 태스크)
-- planning_id는 서브쿼리로 매핑
-- ════════════════════════════════════════════════════════════

-- ── Phase B-0: DB 스키마 정비 ────────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_subscription ENUM 9개 플랜 확장 + 컬럼 추가',
 'ALTER TABLE sm_subscription MODIFY plan ENUM(9개), ADD category/member_count/trial_ends_at/auto_billing_failed_count',
 'db', '대기', 1, 0.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_payment 환불 컬럼 추가',
 'ALTER TABLE sm_payment ADD refund_amount/refund_reason/refund_requested_at',
 'db', '대기', 1, 0.3),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_billing_audit 감사 로그 테이블 생성',
 'CREATE TABLE sm_billing_audit (member_id, action, old_plan, new_plan, old_amount, new_amount, proration_amount, token_adjustment)',
 'db', '대기', 1, 0.3),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_feature_cost 기능별 토큰 비용 테이블 + 초기 데이터',
 'CREATE TABLE + INSERT 16개 기능별 비용 (render=10, ai_rewrite=3, tts=1, voice_clone=5 등)',
 'db', '대기', 1, 0.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'member 테이블 referral_code/referred_by 컬럼 추가',
 'ALTER TABLE member ADD referral_code VARCHAR(8) UNIQUE, ADD referred_by BIGINT',
 'db', '대기', 2, 0.3),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_referral 초대 추적 테이블 생성',
 'CREATE TABLE sm_referral (inviter_id, invitee_id, referral_code, status, signup_bonus_given, sub_bonus_given, sub_plan, sub_bonus_amount)',
 'db', '대기', 2, 0.3),

((SELECT id FROM sm_planning WHERE title = 'Phase B-0: 빌링 DB 스키마 정비'),
 'sm_referral_reward 보상 설정 테이블 + 초기 데이터',
 'CREATE TABLE + INSERT 9개 보상 행 (signup=쌍방50, subscription=Pro100~Biz500)',
 'db', '대기', 2, 0.3);


-- ── Phase B-1: 핵심 버그 수정 ────────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-1: 빌링 핵심 버그 수정'),
 'processDueBillings() 토큰 충전 누락 수정',
 'BillingService.java:250 — 결제 성공 후 tokenService.grantMonthlyTokens(memberId, plan) 호출 추가',
 'backend', '대기', 1, 0.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-1: 빌링 핵심 버그 수정'),
 'PlanLimits.java → sm_plan_config DB 기반 전환',
 '하드코딩 4개 플랜 → TokenService.getPlanConfig() 동적 조회로 교체, PlanLimits 참조 전부 제거',
 'backend', '대기', 1, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-1: 빌링 핵심 버그 수정'),
 '/billing/success 페이지 API 호출 완성',
 'app/billing/success/page.tsx — TODO 제거, api.activateSubscription() 실제 호출 연동',
 'frontend', '대기', 1, 1.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-1: 빌링 핵심 버그 수정'),
 '/api/billing/plans SecurityConfig permitAll 추가',
 'SecurityConfig.java — /api/billing/plans 공개 엔드포인트 등록',
 'backend', '대기', 2, 0.3);


-- ── Phase B-2: AI 기능 토큰 차감 ─────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 'TokenService에 sm_feature_cost DB 조회 메서드 추가',
 'getFeatureCost(featureKey) → sm_feature_cost에서 token_cost 조회, DAO+Mapper 추가',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 'ShortsServiceImpl AI 기능 토큰 차감 적용 (11개)',
 'rewriteScript/translateScript/generateHashtags/generateSeo/analyzeQuality/getTtsPreview/cloneVoice/generateSubtitle*/translateSubtitle에 useTokens() 호출',
 'backend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 'ShortsServiceImpl 내보내기 토큰 차감 (3개)',
 'generatePptSlides(5토큰)/exportPdf(2토큰)/exportPptx(2토큰)에 useTokens() 호출',
 'backend', '대기', 2, 1.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 'BlogServiceImpl 토큰 차감 (2개)',
 'analyzeTone(1토큰)/createPost(5토큰)에 useTokens() 호출, 블로그 생성 실패 시 환불',
 'backend', '대기', 2, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 'render 하드코딩 10토큰 → sm_feature_cost 동적 조회로 변경',
 'ShortsServiceImpl.startRender() 하드코딩 제거, getFeatureCost("render") 사용',
 'backend', '대기', 3, 0.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-2: AI 기능 토큰 차감 (16개 기능)'),
 '블로그 콜백 에러 시 토큰 환불 로직 추가',
 'BlogRestController 내부 콜백에서 에러 시 refundTokens() 호출',
 'backend', '대기', 2, 1.0);


-- ── Phase B-3: 친구 초대 시스템 ──────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'ReferralDao.java + ReferralMapper.xml 생성',
 'MyBatis 매퍼: insertReferral/getReferralByCode/getByInviterAndInvitee/updateStatus/getRewardConfig/getStatsByInviter/getHistoryByInviter',
 'backend', '대기', 1, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'ReferralService.java 생성',
 'generateCode(memberId)/validateCode/processSignupBonus(쌍방50토큰)/processSubscriptionBonus(플랜별)/getMyStats/getHistory',
 'backend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'ReferralController.java 생성',
 'GET /api/referral/my-code, GET /api/referral/validate/{code}, GET /api/referral/stats, GET /api/referral/history',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'MemberServiceImpl 가입 시 초대 보상 연동',
 'saveOrUpdate()에 referralCode 파라미터 추가, 신규 가입 시 referralService.processSignupBonus() 호출',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'BillingService 구독 시 추가 보상 연동',
 'activateSubscription()에서 sm_referral 확인 → sub_bonus 미지급이면 플랜별 보상 지급',
 'backend', '대기', 2, 1.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'MemberRestController 로그인 시 referral_code 쿠키/파라미터 처리',
 '/api/member/login 에서 referralCode 전달받아 saveOrUpdate에 전달',
 'backend', '대기', 2, 1.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 '초대 랜딩 페이지 (app/invite/[code]/page.tsx)',
 'OO님이 초대했어요! 가입하면 100토큰 지급 메시지, Google 로그인 CTA, referral_code를 쿠키/sessionStorage에 저장',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 '대시보드 초대 카드 (components/dashboard/referral-card.tsx)',
 '내 초대 코드 표시, 복사 버튼, 카카오톡/링크 공유 버튼, 초대 현황 미니 통계(초대수/가입수/획득토큰)',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 '초대 관리 페이지 (app/dashboard/referral/page.tsx)',
 '초대 이력 테이블(친구 닉네임/가입일/구독여부/획득토큰), 통계 카드(총 초대/가입 전환율/총 획득 토큰)',
 'frontend', '대기', 2, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 'lib/api.ts 초대 API 함수 추가',
 'getMyReferralCode()/validateReferralCode()/getReferralStats()/getReferralHistory() 추가',
 'frontend', '대기', 2, 0.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-3: 친구 초대 시스템'),
 '대시보드 페이지에 초대 카드 배치',
 'app/dashboard/page.tsx에 ReferralCard 컴포넌트 추가 (InviteBanner 아래)',
 'frontend', '대기', 3, 0.5);


-- ── Phase B-4: 플랜 업그레이드/다운그레이드 ──────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '비례 정산 계산 유틸 메서드 구현',
 'BillingService에 calculateProration(currentPlan, targetPlan, daysRemaining) — 차액 × (남은일/30) 계산',
 'backend', '대기', 1, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '업그레이드 견적 조회 API',
 'GET /api/billing/upgrade-estimate?targetPlan=plus → 차액/비례정산금액/토큰 변동 미리보기',
 'backend', '대기', 1, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '업그레이드 실행 API',
 'POST /api/billing/subscription/upgrade — Toss billingKey 즉시 결제 → 구독 업데이트 → 토큰 조정 → audit 기록',
 'backend', '대기', 1, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '다운그레이드 견적 조회 API',
 'GET /api/billing/downgrade-estimate?targetPlan=pro → 잔여 크레딧/토큰 변동 미리보기',
 'backend', '대기', 2, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '다운그레이드 예약 API',
 'POST /api/billing/subscription/downgrade — pending_downgrade 상태 저장, 다음 결제일에 자동 전환',
 'backend', '대기', 2, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 'BillingScheduler 다운그레이드 예약 처리',
 'processDueBillings()에서 pending_downgrade 상태 감지 → 플랜 전환 + 토큰 재충전',
 'backend', '대기', 2, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 'TokenService 플랜 변경 시 토큰 조정 메서드',
 'adjustTokensForPlanChange(memberId, oldPlan, newPlan) — 업그레이드: 차액 토큰 추가, 다운그레이드: 잔여 유지',
 'backend', '대기', 2, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 '플랜 변경 확인 모달 UI',
 '현재 플랜 vs 변경 플랜 비교, 비례 정산 금액 표시, 토큰 변동 안내, 결제/확인 버튼',
 'frontend', '대기', 1, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 'pricing 페이지에서 플랜 변경 플로우 연결',
 '현재 플랜 표시, 업그레이드/다운그레이드 버튼 분기, 확인 모달 호출',
 'frontend', '대기', 2, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-4: 플랜 업그레이드/다운그레이드'),
 'lib/api.ts 플랜 변경 API 함수 추가',
 'getUpgradeEstimate()/getDowngradeEstimate()/upgradeSubscription()/downgradeSubscription()',
 'frontend', '대기', 2, 0.5);


-- ── Phase B-5: 환불 시스템 ───────────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 'TossPaymentsClient 환불 API 메서드 구현',
 'refundPayment(paymentKey, reason, amount) — POST /v1/payments/{paymentKey}/cancel (부분/전체 환불 지원)',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 'BillingService 환불 로직 구현',
 'requestRefund(memberId, paymentId) — 7일 이내 검증, Toss 환불 호출, sm_payment 상태 업데이트, 토큰 회수, audit 기록, 플랜 free 전환',
 'backend', '대기', 1, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 '환불 API 엔드포인트',
 'POST /api/billing/payment/{paymentId}/refund + GET /api/billing/payment/{paymentId}/refund-status',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 '환불 시 토큰 회수 로직',
 'TokenService.clawbackTokens(memberId) — 잔여 토큰 전부 0으로 + ledger에 refund_clawback 기록',
 'backend', '대기', 2, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 '결제 이력 페이지에 환불 요청 버튼 추가',
 'dashboard/billing — 7일 이내 결제 건에만 "환불 요청" 버튼, 사유 입력 모달, 환불 상태 표시',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-5: 환불 시스템'),
 'lib/api.ts 환불 API 함수 추가',
 'requestRefund(paymentId, reason)/getRefundStatus(paymentId)',
 'frontend', '대기', 2, 0.5);


-- ── Phase B-6: 팀/비즈니스 카테고리 전환 ─────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '개인→팀 전환 서비스 로직',
 'upgradeToTeamPlan(memberId, teamName, memberCount, planId) — sm_team 생성, 본인 owner 등록, 멤버 수 검증(2~10), per-member 가격 계산, 비례 정산 결제',
 'backend', '대기', 1, 5.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '팀→비즈니스 전환 서비스 로직',
 'upgradeToBusinessPlan(memberId, planId) — min 5명 검증, 기존 팀 유지, per-member 가격 재계산, 비례 정산',
 'backend', '대기', 2, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '팀/비즈→개인 다운그레이드 서비스 로직',
 'downgradeToPersonal(memberId, planId) — 멤버 2명 이상이면 차단, 팀 해산 확인, sm_team_member 제거, sm_team_project 공유 해제',
 'backend', '대기', 2, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '팀 멤버 수 변경 시 과금 조정',
 'updateTeamMemberCount(memberId, newCount) — min/max 검증, per-member 비례 정산, sm_subscription.member_count 업데이트',
 'backend', '대기', 2, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '카테고리 전환 API 엔드포인트',
 'POST /api/billing/subscription/switch-category — 요청 body에 targetPlan+teamName+memberCount',
 'backend', '대기', 2, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '체크아웃 시 팀/비즈 멤버 수 입력 UI',
 'pricing 페이지에서 팀/비즈 플랜 선택 시 멤버 수 슬라이더(min~max), 실시간 가격 계산 표시',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-6: 팀/비즈니스 카테고리 전환'),
 '카테고리 전환 확인 모달 UI',
 '개인→팀: 팀 이름 입력 + 멤버 수 선택, 팀/비즈→개인: 팀 해산 경고 + 확인, 비례 정산 안내',
 'frontend', '대기', 2, 3.0);


-- ── Phase B-7: 프론트엔드 빌링/토큰 UI ──────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-7: 프론트엔드 빌링/토큰 UI 완성'),
 'dashboard/usage 토큰 잔액 실제 API 연동',
 'Mock 데이터 제거, api.getTokenBalance() 호출, 토큰 잔액 게이지 + 이번 달 충전/사용량 표시',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-7: 프론트엔드 빌링/토큰 UI 완성'),
 'dashboard/billing 구독/결제 API 전체 연동',
 'TODO 주석 전부 제거, getSubscription/getPaymentHistory/cancelSubscription 실제 호출',
 'frontend', '대기', 1, 4.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-7: 프론트엔드 빌링/토큰 UI 완성'),
 'settings/subscription 전체 연동',
 'TODO 제거, 구독 정보/자동 갱신일/취소 버튼 실제 API 연동',
 'frontend', '대기', 2, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-7: 프론트엔드 빌링/토큰 UI 완성'),
 '에디터 헤더 잔여 토큰 뱃지 + 부족 경고',
 'editor/[projectId] + editor/doc/[projectId] 헤더에 토큰 잔액 표시, AI 기능 호출 전 checkTokens() 경고 모달',
 'frontend', '대기', 1, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-7: 프론트엔드 빌링/토큰 UI 완성'),
 '토큰 사용 이력 페이지 신규 생성',
 'app/dashboard/tokens/page.tsx — 페이지네이션 테이블, 타입별 필터(충전/사용/환불/만료/보너스), 기간 필터',
 'frontend', '대기', 2, 4.0);


-- ── Phase B-8: 어드민 + 보안 ─────────────────────────────────

INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, estimated_hours) VALUES
((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '어드민 토큰 통계 API + UI',
 'GET /api/admin/token-stats — 전체 발행/소비/잔여 토큰, 기능별 소비 랭킹, 유저별 토큰 잔액 Top 20',
 'backend', '대기', 2, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '어드민 수동 보너스 토큰 지급',
 'POST /api/admin/tokens/grant — 특정 유저에게 보너스 토큰 수동 지급, 사유 기록',
 'backend', '대기', 2, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '토스 웹훅 시크릿 검증 구현',
 'BillingController.handleWebhook() — Toss webhook secret 서명 검증 로직 추가',
 'backend', '대기', 1, 2.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '내부 콜백 X-Worker-Secret 검증',
 'ShortsRestController + BlogRestController 내부 콜백에 workerSecret 헤더 검증 추가',
 'backend', '대기', 1, 1.5),

((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '결제 실패 3회 재시도 + 알림',
 'BillingScheduler에 재시도 카운터(auto_billing_failed_count), 1일/3일/7일 간격 재시도, 실패 시 FCM 알림',
 'backend', '대기', 2, 3.0),

((SELECT id FROM sm_planning WHERE title = 'Phase B-8: 어드민 토큰 관리 + 보안 강화'),
 '구독 취소 시 Toss 빌링키 해지 호출',
 'cancelSubscription()에서 TossPaymentsClient를 통해 빌링키 삭제 API 호출 추가',
 'backend', '대기', 2, 1.0);
