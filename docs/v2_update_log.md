# 📝 V2 신규 업데이트 로그 (Update Log)

V1의 **단방향 뉴스레터 발송 서비스**에서 한 걸음 나아가, 구글 클라우드 공식 블로그 및 제품 릴리즈 노트를 통합하여 Customer Engineer(CE)를 지원하는 **대화형 RAG 챗봇 및 실시간 지식 그래프 대시보드**로 전면 고도화하였습니다.

---

## 🚀 주요 개선 사항

### 1. Spring AI 공식 마이그레이션
- 저수준의 Google GenAI SDK를 이용한 정적 API 요청 코드를 폐기하고, 공식 **Spring AI** 라이브러리(`1.0.0-M1` BOM)로 전면 마이그레이션하였습니다.
- `spring-ai-vertex-ai-gemini-spring-boot-starter`를 이용해 구글 Vertex AI의 최신 **Gemini 2.5** 계열 모델과의 통합 환경을 구성하였습니다.

### 2. GCP AlloyDB 통합 벡터 및 그래프 저장소 전환
- Neo4j와 같은 특화 그래프 데이터베이스를 별도로 구축하는 추가 운영 부담을 배제하고, GCP의 완전관리형 PostgreSQL 호환 데이터베이스인 **AlloyDB** 하나로 스토리지를 일원화하였습니다.
- **벡터 유사도 검색**: AlloyDB AI 및 `pgvector` 인덱스를 활용하여 구글 `text-embedding-004` 임베딩 모델(768차원) 기반 문서 청크를 고성능 시맨틱 검색하도록 설정하였습니다.
- **관계형 지식 그래프(Relational KG)**: 엔티티(`gcp_entities`) 및 이들 간의 상관관계(`gcp_relationships`) 데이터를 관계형 스키마 상에 테이블로 매핑해, 일관성 높은 트랜잭션 도메인 내에서 지식 연결 관계를 완벽하게 표현하였습니다.

### 3. 이중 소스(Dual Sources) 크롤링 & 그래프 추출 파이프라인
- 단순 블로그 수집을 넘어 **Google Cloud Blog RSS** 및 **GCP Release Notes xml feed**를 동시에 파싱하는 스케줄러 수집 모듈을 구축하였습니다.
- 각 피드를 파싱한 텍스트에서 Vertex AI Gemini 모델을 실행해, 새로운 개체(Service, Concept, Feature)와 이들 간의 상관관계 트리플을 지식 그래프 상에 자동으로 추출 및 등록합니다.
- 데이터 정합성을 유지하기 위해 코어 GCP 아키텍처 연관관계 구조를 기본 Seeding하는 방식을 병행 설계하여 첫 구동부터 최상의 품질을 보장합니다.

### 4. 실시간 지식 그래프 하이라이팅 대시보드 UI
- HTML5와 Vanilla CSS를 기반으로 한 프리미엄 다크 글래스모피즘(Glassmorphism) 테마를 구현하였습니다.
- `vis-network.js`를 결합하여 데이터베이스 상의 지식 구조를 웹상에 3D 스타일 토폴로지 맵으로 시각화합니다.
- 사용자가 질문을 던지면 RAG 엔진이 참고한 특정 노드들과 연관 엣지선들이 실시간으로 반짝이는 골드 컬러로 변하는 **동적 하이라이팅 인터랙션**을 개발하였습니다.

### 5. Java 21 LTS 업그레이드 및 최신 라이브러리 마이그레이션
- 컴파일러 툴체인을 최신 LTS 버전인 **Java 21**로 전환하여 우수한 JVM 성능 및 가상 스레드 친화적 인프라 환경을 확보하였습니다.
- 기존의 레거시 Java EE 기반 `javax.mail` 패키지 의존성을 완전 정리하고, 현대화된 Jakarta EE 10 사양 표준인 **`org.springframework.boot:spring-boot-starter-mail` (Jakarta Mail)** 로 통합 업그레이드하였습니다.
- 소스 코드 상의 모든 메일링 패키지 임포트 구조를 `javax.mail.*`에서 `jakarta.mail.*`로 전환하여 최신 라이브러리와의 완벽한 런타임 호환성을 완료하였습니다.
- 구 버전의 `mysql:mysql-connector-java` JDBC 드라이버를 최신 모던 배포 버전 패키지인 `com.mysql:mysql-connector-j`로 교체 완료하였습니다.
