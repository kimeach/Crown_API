# ☕ Velona AI - Crown API Guide (CLAUDE.md)

> **⚠️ 작업 전 필독 도서 (중요도 순):**
> 1. `./DEPLOYMENT.md`: 배포 및 장애 대응 절차
> 2. `./SECURITY.md`: 보안 준수 사항 및 금기 코드 패턴
> 3. `./DEVELOPMENT.md`: 코딩 컨벤션 및 권장 아키텍처

이 파일은 `Crown_API` (Java/Spring Boot) 모듈 내에서 작업할 때 준수해야 할 핵심 규칙입니다.

## 🚨 최우선 금기 사항 (Critical)
1. **아키텍처 레이어 엄수**: `RestController → Service → ServiceImpl → Dao → Mapper → XML` 계층 구조를 반드시 준수하십시오. 레이어를 건너뛰는 직접 호출을 금합니다.
2. **SQL 무결성**: `SELECT *` 사용을 금지하며, 명확한 테이블명과 컬럼명을 명시하십시오.
3. **Admin API 일관성**: 어드민 대시보드 통계 추가 시 `AdminController`의 `JdbcTemplate` 기반 통계 쿼리 최적화를 고려하십시오.

## 🛠 주요 명령어
- **서버 실행**: `./gradlew bootRun`
- **빌드 및 테스트**: `./gradlew build`
- **테스트 실행**: `./gradlew test`
- **클린 빌드**: `./gradlew clean build`

## 📜 개발 규칙
- **Naming**: 클래스명은 PascalCase, 변수 및 메서드명은 camelCase를 따르십시오.
- **Exception**: `GlobalExceptionHandler`를 통해 공통된 `ApiResponse` 형식으로 에러를 반환하십시오.
- **DTO**: API 입출력 데이터는 반드시 DTO 클래스를 정의하여 관리하십시오.
