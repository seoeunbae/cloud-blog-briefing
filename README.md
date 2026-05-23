# 🚀 Cloud Blog Briefing V2: GCP Documentation RAG Chatbot

[![GitHub Pages](https://img.shields.io/badge/Docs-GitHub_Pages-amber?style=flat-square&logo=github)](https://seoeunbae.github.io/cloud-blog-briefing/)
[![Java 21](https://img.shields.io/badge/Java-21_LTS-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0--M1-blue?style=flat-square)](https://spring.io/projects/spring-ai)
[![AlloyDB AI](https://img.shields.io/badge/GCP_AlloyDB-Unified_Vector_%26_Graph-blue?style=flat-square&logo=googlecloud)](https://cloud.google.com/alloydb)
[![Gemini 2.5](https://img.shields.io/badge/Model-Gemini_2.5_Flash-orange?style=flat-square&logo=google)](https://deepmind.google/technological-breakthroughs/gemini/)

구글 클라우드 공식 블로그 RSS 피드와 GCP 릴리즈 노트를 통합하여, Customer Engineer(CE)들이 직면하는 아키텍처 및 기술 명세서 질문에 답변하도록 고안된 **하이브리드 RAG 챗봇 및 지식 그래프 시뮬레이터(V2)**입니다.

리드미의 분량이 방대해짐에 따라 사용성의 편의를 위해 **카테고리별로 문서를 전면 분리하고 깃허브 호스팅(GitHub Pages)을 지원**하도록 개편하였습니다.

---

## 🌐 깃허브 호스팅 실시간 문서 사이트 (Live Documentation)

> [!TIP]
> 아래 링크를 방문하시면 **실시간 전체 검색, 사이드바 카테고리 이동, 코드 복사 및 이미지 돋보기 줌인** 기능이 포함된 최상급 대시보드 문서 사이트를 즉시 이용하실 수 있습니다:
> 
> 👉 **[https://seoeunbae.github.io/cloud-blog-briefing/](https://seoeunbae.github.io/cloud-blog-briefing/)**

---

## 📂 카테고리별 분리 문서 링크 (Github Markdown Links)

깃허브 리포지토리 브라우저 상에서 바로 문서를 열람하고 싶으신 경우, 아래의 카테고리 단추를 통해 독립된 개별 Markdown 문서를 클릭하여 편리하게 확인하실 수 있습니다:

| 카테고리 | 문서 상세 설명 | 링크 |
| :--- | :--- | :---: |
| **🏠 프로젝트 소개** | 핵심 기능 요약, 기술 스택, 세부 구조 및 로컬 구동 가이드 | [바로가기 (Home)](docs/README.md) |
| **📝 업데이트 로그** | 레거시 V1(일방향 뉴스레터)에서 V2 RAG 챗봇으로의 마이그레이션 변경 로그 | [바로가기 (Update Log)](docs/v2_update_log.md) |
| **🏛️ 서비스 아키텍처** | 이중 소스 수집기, AlloyDB 저장소 설계 및 하이브리드 RAG 검색 다이어그램 | [바로가기 (Architecture)](docs/architecture.md) |
| **🎬 사용 시나리오** | CE 기술 영업 미팅 대응 및 출처 교차 검증(Cross-checking) 시나리오 | [바로가기 (Scenarios)](docs/scenarios.md) |
| **📸 서비스 갤러리** | V1 뉴스레터/메일링 서비스 UI 캡처 스크린샷 모음 | [바로가기 (Screenshots)](docs/v1_screenshots.md) |

---

## ✨ 핵심 기능 핵심 요약 (Key Features)

1. **Spring AI 마이그레이션**: 레거시 SDK를 완전 폐기하고 최첨단 `Spring AI` 프레임워크 기반의 유연한 Gemini 상호작용 설계 구축.
2. **GCP AlloyDB 단일 통합 저장소**: 이기종 DB를 따로 배치하는 운영 부담을 제로화하고, AlloyDB의 `pgvector`와 RDBMS 조인 구조를 이용한 초정밀 **하이브리드 RAG(Vector RAG + 지식 그래프)** 실현.
3. **듀얼 소스 파이프라인**: 공식 마케팅 블로그 RSS 피드 및 릴리즈 노트 피드를 동시 자동 긁어모아 텍스트 청킹 및 지식 노드 관계를 자동 추출/확장.
4. **골드 인터랙티브 시각화**: 웹 대시보드 상에서 `vis-network.js`를 장착해, 질문 참조와 관련된 GCP 서비스들의 노드/엣지 연결망을 실시간 빛나는 골드 라이트로 추적/시각화.
