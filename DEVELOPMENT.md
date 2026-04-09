저는 Velona AI의 수석 아키텍트 엔진(Architect)입니다. 최근의 코드 변경 내역(제공되지 않음)과 현재 `DEVELOPMENT.md` 내용을 기반으로, Velona AI 프로젝트의 코딩 표준과 아키텍처 지침을 업데이트합니다. 특히 Java 백엔드 스택에 초점을 맞춰 클린 코드 원칙, 네이밍 컨벤션, 그리고 권장되는 설계 패턴을 명시합니다.

---

# 🏗️ Development Guide (DEVELOPMENT.md)

## 🚨 무결점 배포 및 동등성 유지 원칙 (Strict Parity & Zero Regression)
모든 코드 수정은 다음의 **'완벽 검증 절차'**를 통과해야만 하며, 결과값이 이전과 0.0001%라도 다를 경우 배포를 즉시 중단합니다.

1.  **기능적 동등성(Behavioral Parity) 보장**:
    *   수정 후의 결과값(Output)은 수정 전과 **완벽히 일치**해야 합니다.
    *   리팩토링이나 코드 개선이라는 명목으로 기존 로직의 결과 데이터를 변경하는 행위를 엄격히 금합니다.

2.  **전수 테스트(All Cases Testing)**:
    *   정상 케이스뿐만 아니라 모든 엣지 케이스(Null, 에러, 극한의 입력값 등)를 상정하여 테스트하십시오.
    *   기존에 존재하는 모든 유닛 테스트와 통합 테스트를 100% 통과해야 합니다.

3.  **영향도 교차 검증**:
    *   수정된 함수를 참조하는 모든 상위 모듈의 동작을 확인하십시오.
    *   하나의 수리가 다른 곳의 고장(Side Effect)으로 이어지지 않음을 증명해야 합니다.

4.  **무결점 푸시(Gated Push)**:
    *   위 모든 검증과 프로젝트 전체 빌드(`npm run build` / `./gradlew build`)가 **단 하나의 에러나 경고 없이 성공**했을 때만 `main` 브랜치에 푸시할 수 있습니다.
    *   검증되지 않은 코드를 '일단 푸시'하는 행위는 엔진의 자격을 박탈하는 중대한 위반입니다.

## 🗄️ 데이터베이스 원칙 (Database Policy)
1.  **스키마 고정**: 모든 데이터베이스 작업은 반드시 **`crown_db`** 스키마에서만 이루어져야 합니다. 다른 스키마를 생성하거나 임의로 변경하는 행위를 금합니다.
2.  **연결 설정**: 환경 설정 파일(`application.yml` 등) 및 쿼리 작성 시 항상 `crown_db`를 명시적으로 타겟팅하십시오.
3.  **무결성 유지**: 테이블 구조 변경(DDL) 시에는 반드시 백업 및 롤백 시나리오를 먼저 수립해야 합니다.

---

## 📝 클린 코드 원칙 (Clean Code Principles)
Velona AI 백엔드 코드는 가독성, 유지보수성, 확장성을 최우선으로 고려합니다. 다음 클린 코드 원칙을 준수하여 견고한 시스템을 구축합니다.

*   **의미 있는 이름 (Meaningful Names)**:
    *   변수, 함수, 클래스명은 그 의도를 명확히 드러내야 합니다.
    *   약어 사용을 지양하고, 발음하기 쉽고 검색 가능한 이름을 사용합니다.
    *   예: `totalAmount`, `getUserDetails`, `OrderService` (O) / `totAmt`, `getUsr`, `OrdSvc` (X)

*   **단일 책임 원칙 (SRP - Single Responsibility Principle)**:
    *   클래스와 함수는 단 하나의 책임만을 가져야 합니다. 함수는 한 가지 일만 수행하고, 클래스는 한 가지 변경 이유만 가져야 합니다.
    *   이는 코드의 응집도를 높이고 결합도를 낮춥니다.

*   **함수 분해 (Small & Focused Functions)**:
    *   함수는 작고 간결하게 유지합니다. 한 함수 내의 라인 수는 최대한 줄이며, 들여쓰기 수준은 1~2단계 이내로 제한합니다.
    *   추상화 수준을 일관되게 유지하여 코드를 쉽게 이해할 수 있도록 합니다.

*   **반복 피하기 (DRY - Don't Repeat Yourself)**:
    *   동일한 로직이나 코드 블록이 여러 곳에 반복되지 않도록 유틸리티 클래스, 추상화, 또는 적절한 디자인 패턴을 활용하여 재사용 가능한 형태로 구현합니다.
    *   코드 변경 시 한 곳만 수정하면 되도록 합니다.

*   **의존성 주입 (Dependency Injection)**:
    *   객체 간의 의존성을 직접 생성하지 않고 외부(예: Spring IoC 컨테이너)에서 주입받아 사용합니다.
    *   이는 객체 간의 결합도를 낮추고, 테스트 용이성을 크게 높입니다. Spring의 `@Autowired` 또는 생성자 주입을 적극 활용합니다.

*   **오류 처리 (Error Handling)**:
    *   예상 가능한 오류는 명확하게 처리하고, 예외(Exception)를 비즈니스 로직으로 흐르게 하지 않습니다.
    *   실패 시 빨리 종료(Fail-fast)하는 전략을 고려하며, 사용자에게 의미 있는 오류 메시지를 제공합니다.

*   **주석 최소화 (Minimal Comments)**:
    *   코드가 스스로 설명하도록 작성하는 것을 최우선으로 합니다.
    *   주석은 '왜' 이렇게 했는지에 대한 이유나 복잡한 알고리즘 설명 등 불가피한 경우에만 사용합니다. '무엇을' 하는지는 코드만으로 파악할 수 있어야 합니다.

## 🏷️ 네이밍 컨벤션 (Naming Conventions)
Velona AI 프로젝트는 Java 표준 네이밍 컨벤션을 따르며, 코드의 일관성과 가독성을 높입니다.

*   **패키지 (Package)**: `all.lowercase.with.dots`
    *   예: `com.velona.service`, `com.velona.repository`, `com.velona.controller`

*   **클래스 및 인터페이스 (Class & Interface)**: `PascalCase` (첫 글자 대문자, 합성어 첫 글자 대문자)
    *   예: `UserService`, `ProductRepository`, `UserMapper`, `OrderCreateRequest`

*   **메서드 및 변수 (Method & Variable)**: `camelCase` (첫 글자 소문자, 합성어 첫 글자 대문자)
    *   예: `getUserById`, `calculatePrice`, `userName`, `isValidEmail`

*   **상수 (Constant)**: `SCREAMING_SNAKE_CASE` (모든 글자 대문자, 단어 구분은 언더스코어 `_`)
    *   예: `MAX_RETRIES`, `DEFAULT_PAGE_SIZE`, `API_VERSION`

*   **Enum**: Enum 타입 자체는 `PascalCase`, Enum 값은 `SCREAMING_SNAKE_CASE`
    *   예:
        ```java
        public enum OrderStatus {
            PENDING,
            SHIPPED,
            DELIVERED
        }
        ```

## 🛠️ 아키텍처 및 설계 가이드라인 (Architecture & Design Guidelines)

Velona AI는 안정적이고 확장 가능한 백엔드 시스템 구축을 목표로 합니다. 다음 지침을 준수하여 중복을 줄이고 견고한 설계를 지향합니다.

1.  **계층형 아키텍처 (Layered Architecture)**
    각 계층은 고유한 책임과 역할에 집중하여 결합도를 낮추고 모듈성을 높입니다.
    *   **Controller Layer**:
        *   클라이언트 요청 처리 및 응답, DTO 변환 등 외부 인터페이스 역할.
        *   **주의**: 비즈니스 로직은 포함하지 않습니다. Service Layer를 호출하는 역할에 집중합니다.
    *   **Service Layer**:
        *   핵심 비즈니스 로직 구현. 여러 리포지토리 또는 외부 서비스와의 상호작용을 오케스트레이션 합니다.
        *   트랜잭션 관리 책임을 가질 수 있습니다.
    *   **Repository (or DAO) Layer**:
        *   데이터베이스 접근 및 영속성 관리.
        *   데이터 저장/조회/수정/삭제 등 기본적인 CRUD 작업에 집중합니다. 도메인 객체를 반환합니다.
    *   **Domain Layer**:
        *   핵심 비즈니스 도메인 객체 (Entity, Value Object) 및 관련 로직.
        *   비즈니스 규칙과 상태를 포함하며, 다른 계층에 의해 사용됩니다.

2.  **중복 로직 제거 (DRY - Don't Repeat Yourself)**
    코드 중복은 버그의 온상이므로, 다음 방법을 적극적으로 활용하여 중복을 제거합니다.
    *   **유틸리티 클래스**: 범용적으로 사용될 수 있는 헬퍼 메서드들은 별도의 유틸리티 클래스(예: `DateUtil`, `ValidationUtil`, `CipherUtil`)로 분리하여 관리하고 재활용합니다.
    *   **추상화 및 인터페이스**: 공통된 행위나 속성을 가진 객체들은 추상 클래스나 인터페이스를 통해 추상화하여 중복 코드를 줄이고 유연성을 확보합니다.
    *   **AOP (Aspect-Oriented Programming)**: 로깅, 보안, 트랜잭션 관리, 성능 측정 등 여러 모듈에 걸쳐 반복되는 횡단 관심사(Cross-cutting Concerns)는 Spring AOP를 활용하여 분리하고 핵심 비즈니스 로직과의 결합도를 낮춥니다.

3.  **권장 디자인 패턴 (Recommended Design Patterns)**
    특정 문제 해결 및 코드 구조화에 도움이 되는 검증된 디자인 패턴을 적극 활용합니다.
    *   **의존성 주입 (Dependency Injection)**: Spring Framework의 강력한 DI 기능을 적극 활용하여 객체 간의 결합도를 낮추고 테스트 용이성을 극대화합니다. `final` 필드와 생성자 주입을 우선적으로 고려합니다.
    *   **DTO (Data Transfer Object)**: 계층 간 데이터 전송 시에는 도메인 모델(Entity)을 직접 노출하지 않고 DTO를 사용하여 필요한 데이터만 전달하고 민감한 정보를 보호하며, 특정 계층의 변화가 다른 계층에 미치는 영향을 최소화합니다.
    *   **Repository Pattern**: 데이터 접근 로직을 추상화하여 특정 데이터 저장 기술(DB)에 대한 의존성을 줄이고, 테스트 용이성을 높입니다. JPA를 사용하는 경우 Spring Data JPA가 이 패턴을 잘 구현하고 있습니다.
    *   **Strategy Pattern**: 동일한 문제를 해결하는 여러 알고리즘이 존재할 때, 이들을 캡슐화하고 런타임에 동적으로 교체할 수 있도록 합니다. (예: 결제 방식, 할인 정책)
    *   **Builder Pattern**: 복잡한 객체 생성 시, 가독성을 높이고 선택적 인자를 유연하게 처리할 수 있도록 돕습니다. 특히 불변(immutable) 객체 생성에 유용합니다.
    *   **Factory Pattern**: 객체 생성 로직을 캡슐화하여 클라이언트 코드가 특정 구현체에 의존하지 않도록 돕습니다. (예: 특정 타입의 오브젝트를 생성하는 팩토리 클래스)

---