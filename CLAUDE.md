# agentFactory

한 줄 아이디어를 입력하면 관리자형(admin) 웹앱 MVP를 자동 생성하는 멀티 에이전트 서비스.

## 기술 스택

- **Frontend**: TypeScript (사용자가 아이디어를 입력하고 생성 과정/결과를 확인하는 대시보드)
- **Backend**: Kotlin (아이디어 수신 → 에이전트 오케스트레이션 → MVP 산출물 관리)
- **MVP 생성 엔진**: Claude (Claude API / Agent SDK를 호출해 실제 코드·스키마·UI를 생성)

## 구조 (계획)

현재 프로젝트는 초기 상태. 아래는 앞으로 맞춰나갈 레이아웃 가이드.

- `frontend/` — TypeScript 기반 UI (프레임워크 미정; Next.js 또는 Vite+React 후보)
- `backend/` — Kotlin 기반 API 서버 (프레임워크 미정; Spring Boot 또는 Ktor 후보)
- `agents/` — MVP 생성을 담당하는 에이전트 정의 및 프롬프트
- `generated/` — 에이전트가 생성한 MVP 산출물이 저장될 디렉터리 (런타임)

실제 디렉터리가 만들어지면 이 섹션을 업데이트할 것.

## .claude 구조

Claude가 이 프로젝트에서 일관되게 동작하도록 `.claude/` 를 역할별로 분리한다.

- `.claude/agents/` — 서브에이전트 정의 (`*.md`, Claude Code가 자동 로드)
- `.claude/skills/` — 사용자 호출형 스킬 (`<skill-name>/SKILL.md`)
- `.claude/rules/` — 역할·에이전트·스킬이 공통으로 따르는 규칙 (아래에서 import)
- `.claude/hooks/` — Claude Code 훅에서 실행할 스크립트. 배선은 `.claude/settings.json` 에서.

각 디렉터리의 `README.md` 가 세부 컨벤션을 설명한다.

## 규칙 (rules/)

아래 규칙은 이 파일을 통해 항상 컨텍스트에 로드된다. 에이전트/스킬 프롬프트에서도 필요한 항목을 인용할 것.

@.claude/rules/general.md
@.claude/rules/stack.md
@.claude/rules/mvp-generation.md

## 명령어

프로젝트가 초기화되면 이 섹션에 `build`, `test`, `lint`, `dev` 실행 방법을 기록한다.
