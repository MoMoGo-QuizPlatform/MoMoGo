# Notification 도메인 k6 부하테스트

## 무엇을 증명하는가

대상 API는 `POST /api/spaces/{spaceId}/rooms` (시험방 생성)이지만, 실제로 측정하는 것은
그 안에서 동기적으로 실행되는 `NotificationEventListener.handleRoomCreated`의 알림 생성
반복 처리 비용이다. `@Async`가 없어서 초대 인원수만큼 DB insert를 순서대로 끝내야
관리자에게 응답이 간다. 이 병목을 근거로 Kafka 기반 비동기 알림 처리 도입을 제안한다.

방 생성 API는 room 도메인 소유지만, 느린 원인과 해결책(Kafka)은 notification 도메인
소관이라 이 폴더에서 테스트한다. **Stress/Spike 실행 전 room 도메인 담당자 및 팀에
사전 공지할 것** (배포 서버가 일시적으로 느려지거나 응답 없음 상태가 될 수 있음).

## 실행 전 준비 (시드 데이터)

아래 값이 실제 값으로 채워져야 테스트가 동작한다 (`config.js` 참고):

| 항목 | 상태 | 비고 |
|---|---|---|
| `CATEGORY_ID` | ✅ 확정값 | 배포 DB에 이미 존재하는 '테스트' 카테고리 |
| `ADMIN_USER` | ⏳ 준비 필요 | 회원가입 후 공간 생성 시 자동으로 ADMIN이 됨 |
| `SPACE_ID` | ⏳ 준비 필요 | ADMIN_USER가 생성한 공간의 UUID |
| `STUDENT_USER_IDS` | ⏳ 준비 필요 | 그 공간에 소속된 학생 계정 UUID 200개 |

이 픽스처(공간 + 학생 200명)는 notification 전용이 아니라 **다른 도메인도 재사용 가능한
공용 테스트 자산**으로 만들 예정. 시드 완료 후 팀에 공유 예정.

## 파일 구성

| 파일 | 종류 | 변수 | 목적 |
|---|---|---|---|
| `invite-scale-test.js` | 원인 증명 (표준 4종 아님) | 초대 인원 (1→200), 동시성 없음(VU 1) | 응답시간이 인원수에 비례하는지 확인 |
| `load-test.js` | 표준 Load | 동시 사용자 (VU 15 고정) | 평상시 트래픽에서 SLA 충족 여부 |
| `stress-test.js` | 표준 Stress | 동시 사용자 (VU 20→400 계단식) | 시스템이 무너지는 한계점 탐색 |
| `spike-test.js` | 표준 Spike | 동시 사용자 (5→300→5 급변) | 급증 시 대응 및 회복력 |
| `soak-test.js` | 표준 Soak | 동시 사용자 (VU 10, 25분 유지) | 장시간 안정성 (누수 등) |

표준 4종은 초대 인원을 30명(한 반 규모 가정)으로 **고정**하고 동시 사용자 수만 변화시킨다.
`invite-scale-test.js`만 반대로 동시성을 1로 고정하고 초대 인원을 변화시킨다.

> Soak 테스트는 25분으로 캡되어 있다 — JWT AccessToken 유효시간이 30분이라 그 이상
> 돌리면 토큰 만료로 401이 나며 결과가 오염되기 때문. 실제 soak은 보통 몇 시간이지만
> 발표/검증 목적으로 압축했다.

## 실행 방법

```bash
k6 run k6/notification/invite-scale-test.js   # 먼저 원인 증명부터
k6 run k6/notification/load-test.js
k6 run k6/notification/stress-test.js
k6 run k6/notification/spike-test.js
k6 run k6/notification/soak-test.js
```
