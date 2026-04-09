# 🚀 Velona AI - Integrated Deployment Guide (DEPLOYMENT.md)

이 파일은 프로젝트 전체의 배포 원칙과 장애 대응 절차를 정의한 **최상위 지침서**입니다. 모든 에이전트는 배포 전 이 파일을 반드시 숙지해야 합니다.

---

## 🚨 장애 발생 시 대응 및 기록 절차 (Incident Protocol)
서버가 중단되거나 API 응답이 없을 경우, **재시작 전** 반드시 다음 절차를 수행하십시오.

1.  **원인 조사**: `pm2 logs`, `auto_dev_master.log`, 또는 Java 콘솔 로그를 통해 마지막 에러 스택을 수집합니다.
2.  **리포트 작성**: 각 프로젝트의 `docs/error/` 폴더에 `YYYYMMDD-HHMM-[서비스]-failure.md` 형식으로 분석 내용을 기록합니다.
3.  **기록 필수 항목**: 장애 증상, 수집된 로그, 추정 원인, 조치 내용, 재발 방지책.
4.  **선 분석 후 조치**: 리포트 작성이 완료된 후에만 서버를 재시작하거나 패치를 적용합니다.

---

## ♻️ 서버 재시작 및 배포 절차 (Server Restart & Deployment Procedure)
장애 복구 또는 신규 배포 시, 시스템 안정성을 위해 다음 절차를 **반드시** 준수하십시오. Cafe24 클라우드(RAM 2GB) 환경에서는 불필요한 프로세스가 메모리 고갈과 시스템 불안정을 초래할 수 있으므로, 특히 엄격한 관리가 필요합니다.

1.  **모든 관련 프로세스 강제 종료 (Clean Slate 확보)**
    *   **목표**: 모든 구버전 또는 좀비 프로세스를 제거하여 깨끗한 환경을 조성하고 메모리 누수를 방지합니다.
    *   `python-worker` 관련: `taskkill /F /IM python.exe /T` 또는 `taskkill /F /IM py.exe /T` (실행 방식에 따라)
    *   `Crown_API` (Java) 관련: `taskkill /F /IM java.exe /T`
    *   **확인**: `tasklist | findstr "python.exe"` 또는 `tasklist | findstr "java.exe"` 명령으로 관련 프로세스가 모두 종료되었는지 육안으로 확인합니다.
    *   **경고**: 이 단계는 가장 중요합니다. 잔류 프로세스는 `NameError`, 포트 점유, 메모리 경합 등 예측 불가능한 문제를 야기합니다. Cafe24 환경에서 RAM 고갈은 치명적입니다.

2.  **최신 코드 반영 및 환경 설정 확인**
    *   Git 저장소에서 최신 버전을 `git pull`로 반영합니다.
    *   `requirements.txt` 또는 `build.gradle` 기반으로 필요한 의존성 파일들이 최신 상태인지 확인하고, 필요한 경우 재설치/재빌드합니다.
    *   환경 변수(`.env`) 파일에 변경 사항이 없는지, 또는 새로 추가된 환경 변수가 올바르게 설정되었는지 다시 한번 검토합니다.

3.  **서비스 재시작 및 초기 동작 확인**
    *   각 프로젝트의 시작 스크립트(예: `python auto_dev_master.py`, `java -jar ...`)를 사용하여 서비스를 재시작합니다.
    *   재시작 직후, 서비스가 정상적으로 시작되었음을 나타내는 초기 로그 메시지를 확인합니다. (예: "Velona AI Master Engine Started", "Spring Boot Application Started")
    *   **주의**: 백그라운드 프로세스 매니저(예: `pm2`)를 사용하는 경우, `pm2 restart [app_name]` 명령이 모든 기존 프로세스를 확실히 종료하고 새로운 프로세스를 올리는지 검증해야 합니다. 필요한 경우 `pm2 stop [app_name]` 후 `pm2 delete [app_name]` 후 `pm2 start [script]`로 완전 재시작을 고려합니다.

4.  **기능 검증 및 로그 모니터링**
    *   가장 최근에 변경된 기능 또는 핵심 기능에 대해 간단한 테스트를 수행하여 예상대로 동작하는지 확인합니다.
    *   `auto_dev_master.log` 및 Java 서버 로그를 실시간으로 모니터링하여 `NameError`나 `Port already in use`와 같은 즉각적인 에러가 발생하지 않는지 확인합니다.

---

## 🛠 프로젝트별 배포 전 체크리스트

### [Common]
- 모든 분석 리포트는 DB(`sm_dev_task.result_content`)에 먼저 저장되었는가?
- 환경 변수(`.env`)나 모델명(`2.5-flash`)이 임의로 변경되지 않았는가?
- **(필수) 배포 전 `♻️ 서버 재시작 및 배포 절차`를 완벽히 숙지하고 이행할 준비가 되었는가?**

### [velonaAi-react]
- `npm run build`를 실행하여 컴파일 에러(태그 중복, 타입 오류)가 없는지 확인했는가?
- 하드코딩된 색상 대신 CSS 변수(`var(--ve-*)`)를 사용했는가?

### [Crown_API]
- `./gradlew build`로 테스트 통과 여부를 확인했는가?
- SQL 쿼리에 `SELECT *`가 포함되지 않았는가?
- **(확인) 배포 전 `java.exe` 프로세스가 기존에 1개 이상 실행되고 있지는 않은가? 발견 시 `♻️ 서버 재시작 및 배포 절차`의 1번 항목을 우선 수행했는가?**

### [python-worker]
- 마스터 엔진(`auto_dev_master.py`)의 루프 내에 `try-except`가 안전하게 걸려있는가?
- 신규 스크립트 추가 시 실행 권한 및 경로 설정이 올바른가?
- **(필수) 코드 변경 후, 특히 새로운 함수(`nightly_perf_cycle` 등)를 추가하거나 기존 함수를 수정한 경우, 해당 함수가 현재 배포될 코드에 명확히 정의되어 있으며, 이 코드가 실제로 실행될 것임을 재확인했는가? (구버전 코드 실행으로 인한 `NameError` 방지)**
- **(필수) `auto_dev_master.py` 배포 시, 프로세스 매니저(예: `pm2`)의 `restart` 명령에만 의존하지 않고, 모든 `py.exe` 또는 `python.exe` 프로세스를 강제 종료 후 새로운 인스턴스를 시작하는 방식으로 배포를 진행했는가?**