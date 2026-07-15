<div align="center">

<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td><img src="./logo.png" width="100" height="100" alt="SSAIREN Logo"/></td>
    <td>&nbsp;&nbsp;&nbsp;</td>
    <td align="left">
      <h1>싸이렌 (SSAIREN)</h1>
      <h3>실시간 보이스피싱 탐지 AI 앱</h3>
    </td>
  </tr>
</table>

> **"사용자가 버튼을 누르기 전에, AI가 먼저 움직입니다"**

<br>

<img src="https://img.shields.io/badge/상태-현재%20개발%20중-FF6B35?style=flat-square&labelColor=0F172A"/>
&nbsp;
<img src="https://img.shields.io/badge/SSAFY-15기-1E3A5F?style=flat-square&labelColor=0F172A"/>
&nbsp;
<img src="https://img.shields.io/badge/Team-7인팀-1E3A5F?style=flat-square&labelColor=0F172A"/>

<br><br>

<img src="./image.png" width="700" alt="SSAIREN App Mockup"/>

</div>

<br>

## 프로젝트 소개

- **SSAIREN(싸이렌)** 은 통화 중 발생하는 보이스피싱을 실시간으로 탐지하는 AI 기반 Android 앱입니다.
- 사용자가 의심 통화를 인지하기 전, AI가 먼저 음성을 분석해 경고를 발송합니다.
- 보호자 연동 기능을 통해 고령자·취약계층을 가족이 원격으로 보호할 수 있습니다.
- 납치협박 시나리오 발생 시 보호자의 실시간 위치를 피해자에게 전달해 안전을 확인합니다.

<br>

## 팀 구성

<div align="center">

| **spspd** | **s2vn9wxx** | **kyeongminn** | **oz115** |
| :------: | :------: | :------: | :------: |
| [<img src="https://avatars.githubusercontent.com/spspd?v=4" height=120 width=120> <br/> @spspd](https://github.com/spspd) | [<img src="https://avatars.githubusercontent.com/s2vn9wxx?v=4" height=120 width=120> <br/> @s2vn9wxx](https://github.com/s2vn9wxx) | [<img src="https://avatars.githubusercontent.com/kyeongminn?v=4" height=120 width=120> <br/> @kyeongminn](https://github.com/kyeongminn) | [<img src="https://avatars.githubusercontent.com/oz115?v=4" height=120 width=120> <br/> @oz115](https://github.com/oz115) |

| **minyeongg** | **insongK** | **moonjs1011** |
| :------: | :------: | :------: |
| [<img src="https://avatars.githubusercontent.com/minyeongg?v=4" height=120 width=120> <br/> @minyeongg](https://github.com/minyeongg) | [<img src="https://avatars.githubusercontent.com/insongK?v=4" height=120 width=120> <br/> @insongK](https://github.com/insongK) | [<img src="https://avatars.githubusercontent.com/moonjs1011?v=4" height=120 width=120> <br/> @moonjs1011](https://github.com/moonjs1011) |

</div>

<br>

## 개발 환경

- **Frontend** : Flutter (Android), Dart
- **Backend** : Spring Boot, FastAPI, WebSocket
- **AI/ML** : OpenAI Whisper API, GPT-4o-mini, LangChain
- **Infra** : AWS EC2, Firebase FCM, KakaoMap API
- **버전 및 이슈관리** : Github, Github Issues, Github Project
- **협업 툴** : Discord, Notion, Github Wiki
- [커밋 컨벤션](https://github.com/SSAIREN/.github/wiki)
- [코드 컨벤션](https://github.com/SSAIREN/.github/wiki)

<br>

## 기술 스택 선택 이유

### Flutter (Android)

- 단일 코드베이스로 피해자 앱과 보호자 앱을 하나의 프로젝트에서 관리해 팀 리소스를 효율화했습니다.
- Hot Reload 기능으로 UI 반복 개선 사이클을 단축했습니다.
- Dart의 `async/await` 패턴이 오디오 버퍼 스레드와 전송 스레드를 분리 운영하는 구조에 적합합니다.

### Spring Boot

- 사용자 인증, 세션 관리, WebSocket 연결 등 클라이언트 facing 로직을 담당합니다.
- AI 추론이 필요한 요청은 내부 REST API를 통해 FastAPI 서버로 위임합니다.

### FastAPI + LangChain

- Python 네이티브 AI 생태계를 그대로 활용하기 위해 FastAPI를 선택했습니다.
- LangChain의 체이닝 구조로 Whisper STT → GPT-4o-mini 위험도 판단 파이프라인을 명확하게 구성했습니다.
- Spring Boot가 처리하기 어려운 AI 추론 로직을 별도 서버로 분리해 관심사를 분리했습니다.

### REST API + WebSocket 혼합

- 평상시 오디오 분석은 5초 단위 청크를 REST API(POST /analyze)로 전송합니다. 이전 요청이 완료된 후 다음 요청을 보내는 루프 구조로 요청 중복을 방지합니다.
- WebSocket은 트리거 발동(위험도 0.8 이상) 이후 툴 실행 결과를 클라이언트에 실시간으로 푸시하는 구간에만 사용합니다.
- 경찰 대시보드는 항상 WebSocket 연결을 유지해 새로운 피해 케이스를 실시간으로 수신합니다.

### Firebase FCM

- 보호자 기기로의 푸시 알림 인프라를 빠르게 구성하기 위해 선택했습니다.
- 앱이 백그라운드 또는 종료 상태일 때도 안정적으로 알림이 전달됩니다.
- 납치협박 시나리오에서 보호자 앱이 종료된 상태에서도 GPS 위치 요청을 트리거하는 데 활용됩니다.

### 브랜치 전략

- Git-flow 전략 기반으로 `main`, `develop`, `feat` 브랜치를 운영합니다.
  - **main** : 배포 전용 브랜치
  - **develop** : 개발 통합 브랜치
  - **feat/기능명** : 기능 단위 독립 개발 후 develop에 merge, 이후 삭제

<br>

## 핵심 기능

### [실시간 보이스피싱 탐지]
- 통화 중 음성을 끊김 없이 마이크로 수음하며, 5초 단위로 분리된 오디오 청크를 REST API로 AI 서버에 전송합니다.
- Whisper API로 STT 변환 후 GPT-4o-mini가 보이스피싱 패턴 여부와 위험도(0~1)를 판단합니다.
- 위험도가 임계값(0.8)을 넘으면 즉시 앱 내 경고 팝업과 진동 알림을 발생시킵니다.
- 중복 트리거 방지를 위해 한 번 트리거된 세션은 통화 종료 전까지 재트리거되지 않습니다.

<br>

### [자동 대응 시스템]
- 위험도 임계값 초과 시 사용자 확인 후 LangChain 툴이 병렬로 자동 실행됩니다.
  - 보호자에게 FCM 푸시 알림 발송
  - 납치협박 유형 감지 시 보호자의 GPS 위치 조회 및 KakaoMap 표시
  - 경찰 관제 대시보드에 케이스 실시간 등록
- 툴 실행 결과는 WebSocket을 통해 피해자 앱에 단계별로 실시간 표시됩니다.

<br>

### [보호자 연동]
- 피해자-보호자 계정을 하나의 앱에서 역할 선택으로 연결합니다.
- 위험 알림 발생 시 보호자는 FCM 푸시를 수신하고 앱에서 안심 메시지를 피해자에게 전송할 수 있습니다.
- 납치협박 시나리오에서 보호자(아들)의 실시간 GPS 위치를 피해자(부모)에게 KakaoMap으로 표시해 납치 여부를 즉시 확인할 수 있습니다.

<br>

### [경찰 관제 대시보드]
- 웹 기반 실시간 모니터링 화면으로, WebSocket 연결을 유지해 새 피해 케이스를 실시간으로 수신합니다.
- 케이스별 위험도, 피해 유형, 위치 정보, AI 통화 분석 요약을 제공합니다.
- 대응 프로세스(위치 파악 → 가족 알림 → 경찰 통보) 진행 상황을 단계별로 표시합니다.

<br>

## 시스템 구조

```
사용자 (Android Flutter 앱)
       │
       │ POST /analyze (5초 단위 오디오 청크, REST API)
       │ POST /trigger (위험도 0.8 이상, 사용자 확인 후)
       │ WS /ws/trigger (트리거 이후 툴 실행 결과 수신)
       ▼
  Spring Boot (API Gateway · EC2)
  ├── 사용자 인증 / 세션 관리
  ├── WebSocket 연결 관리 (피해자 앱 · 보호자 앱 · 경찰 대시보드)
  ├── 보호자 연동 관리
  └── 탐지 결과 저장
       │ REST API (내부 호출)
       ▼
  FastAPI + LangChain (AI 분석 서버 · EC2)
  ├── Whisper API → STT 변환
  ├── GPT-4o-mini → 보이스피싱 위험도 + 유형 판단
  └── LangChain Tools (병렬 실행)
       ├── SMS / FCM → 보호자 기기 푸시 알림
       ├── GPS → 보호자 위치 조회 (납치협박 시나리오)
       └── 경찰 대시보드 → WS /ws/dashboard 케이스 등록

  경찰 관제 대시보드 (Web)
  └── WS /ws/dashboard 연결 유지 → 케이스 실시간 수신
```

<br>

## 개발 기간 및 작업 관리

### 개발 기간

- 전체 개발 기간 : 2026-05-14 ~ 2026-06-14
- 기획 및 설계 : 2026-05-14 ~ 2026-05-31
- 기능 구현 : 2026-06-13 ~ 2026-06-14

<br>

### 작업 관리

- GitHub Projects와 Issues를 사용하여 진행 상황을 공유했습니다.
- 주간 회의를 진행하며 작업 방향성에 대한 고민을 나누고 Notion에 회의 내용을 기록했습니다.

<br>

---

<div align="center">
<sub>© 2025 SSAIREN Team · SSAFY 15기</sub>
</div>
