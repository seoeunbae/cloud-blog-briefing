# 🚀 Cloud Blog Briefing V2: GCP Documentation RAG Chatbot

구글 클라우드 공식 블로그 RSS 피드와 GCP 제품 릴리즈 노트를 실시간 크롤링하여, Customer Engineer(CE) 분들을 지원하기 위해 제작된 **대형 언어 모델(LLM) 기반의 하이브리드 RAG 챗봇 및 지식 그래프 시각화 시스템**입니다.

---

## ✨ 핵심 기술 스택 (Tech Stack)

이 솔루션은 전사적 규모의 정합성과 초고속 다차원 검색 성능을 제공하기 위해 엔터프라이즈 프레임워크와 GCP 전용 인프라 제품군을 조화롭게 통합하였습니다.

- **Application Framework**: `Java 21 (LTS)`, `Spring Boot 3.5.x`, `Spring AI 1.0.0-M1` (Gemini, PGVector integration)
- **Unified Database**: **Google Cloud AlloyDB for PostgreSQL** (with `pgvector` indexing)
- **GenAI Foundation**: **Google Vertex AI Gemini 2.5** (`gemini-2.5-flash` / `text-embedding-004`)
- **Ingestion Tools**: `Jsoup` XML/Atom Feed Parser, `TokenTextSplitter`
- **Frontend Presentation**: HTML5, Vanilla CSS, **`vis-network.js` (실시간 토폴로지 지식 맵 시각화)**

---

## 🏛️ 하이브리드 RAG (Vector + Knowledge Graph)

본 시스템은 단순 문서 청크 벡터 탐색(Vector RAG)의 한계를 극복하기 위해 **관계형 지식 그래프(Relational Knowledge Graph)** 구조를 결합했습니다.

```
       [사용자 질문 입력]
              │
              ├───► 1. 벡터 유사도 검색 (AlloyDB pgvector) -> 비구조화 Chunks 추출
              │
              └───► 2. 키워드 매핑 매칭 (gcp_entities / gcp_relationships) -> 구조화 삼조판(Entity-Relation-Entity) 추출
              │
              ▼
    [하이브리드 컨텍스트 결합] ───► [Gemini Chat Model] ───► [신뢰도 높은 인용 포함 응답 반환]
```

---

## 🛠️ 개발 플랜 & 기능 카테고리

좌측 메뉴 또는 아래 링크를 통해 상세 설정을 확인하실 수 있습니다:

1. **[📝 V2 업데이트 로그](v2_update_log.md)**: 레거시 V1(일방향 뉴스레터)에서 V2 RAG 챗봇으로의 마이그레이션 기술 내용.
2. **[🏛️ 서비스 아키텍처](architecture.md)**: 데이터 파이프라인 수집, AlloyDB 스키마 구성, 하이브리드 RAG 런타임 검색 설계.
3. **[🎬 CE 사용 시나리오](scenarios.md)**: 고객 기술 미팅 대응 및 크로스 체크(Cross-checking) 등 실무 활용도 극대화 워크플로우.
4. **[📸 V1 스크린샷 갤러리](v1_screenshots.md)**: 기존 레거시 메일링 시스템 기록.

---

## 🚀 빠른 시작 (Quick Start)

로컬 빌드 및 데이터 적재를 진행하기 위한 표준 커맨드 시퀀스입니다.

### 0. Java 21 설치
본 프로젝트는 **Java 21 (LTS)** 사양을 요구합니다. 시스템 환경에 맞춰 적절한 JDK 21 버전을 설치해 주십시오.

#### Debian 12 / Ubuntu 환경 (Apt 패키지 매니저 활용)
Debian 12 안정 버전(Stable) 저장소는 최신 JDK 21을 직접 포함하지 않을 수 있으므로, 아래와 같이 Eclipse Temurin(Adoptium)의 공식 저장소를 등록하여 손쉽게 설치할 수 있습니다 (현재 디스크 공간은 복구되어 정상 설치가 가능합니다):

```bash
# Eclipse Temurin (Adoptium) JDK 21 설치 예시
sudo apt-get update
sudo apt-get install -y wget apt-transport-https gnupg

sudo mkdir -p /etc/apt/keyrings
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg

echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt-get update
sudo apt-get install -y temurin-21-jdk
```

#### macOS (Homebrew 활용)
```bash
brew install openjdk@21
```

#### 설치 완료 및 버전 확인
설치가 완료된 후 터미널을 재시작하고 버전을 검증합니다:
```bash
java -version
# openjdk version "21..." 형태로 출력되는지 확인합니다.
```

### 1. 데이터베이스 세팅 (AlloyDB)
스토리지 실행 후, `src/main/resources/schema.sql`에 정의된 데이터베이스 테이블과 pgvector 인덱스를 활성화합니다:
```sql
-- pgvector 확장 모듈 활성화
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. 환경 변수 구성
`src/main/resources/application.properties` 파일을 생성하고 아래 주요 변수를 기입합니다:
```properties
spring.application.name=cloudblog-mailing

# GCP Project and API settings
spring.cloud.gcp.project-id=YOUR_GCP_PROJECT_ID
spring.cloud.gcp.credentials.location=file:/absolute/path/to/your/service-account.json

# Spring AI Vertex AI Gemini 설정
spring.ai.vertex.ai.gemini.project-id=${spring.cloud.gcp.project-id}
spring.ai.vertex.ai.gemini.location=us-central1
spring.ai.vertex.ai.gemini.chat.options.model=gemini-2.5-flash

# PostgreSQL / AlloyDB 연결 설정
spring.datasource.url=jdbc:postgresql://YOUR_ALLOYDB_IP:5432/YOUR_DB_NAME
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### 3. 프로젝트 구동
Gradle 빌드 명령어로 부트 애플리케이션을 즉각 실행합니다:
```bash
./gradlew bootRun
```
앱이 정상 시작되면 브라우저를 열고 **`http://localhost:8080/v2/chat`**으로 접속하십시오.
우측 상단의 **`🔄 데이터 수집 시작`** 버튼을 누르시면, 백그라운드 스케줄러가 RSS 피드를 자동 수집하여 벡터 가중치 및 지식 엣지 데이터 분석을 수행합니다.
