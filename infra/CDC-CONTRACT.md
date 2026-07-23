# CDC 메시지 계약

Debezium을 나중에 "수제" CDC(WAL을 직접 읽어 Kafka로 쏘는 프로세스)로 교체할 때,
아래 계약만 그대로 지키면 `voice-service`/`rag-service`의 프로듀서(outbox insert)와
컨슈머(`@KafkaListener`) 코드는 전혀 건드릴 필요가 없다. 교체 대상은 `docker-compose.yml`의
`kafka`/`connect`/`connect-init` 세 서비스뿐이다.

## 공통 outbox 테이블 스키마

`voice` DB, `rag` DB 각각에 존재하는 `outbox_event` 테이블(같은 트랜잭션 안에서 도메인 변경과
함께 insert됨):

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | uuid | 이벤트 고유 ID (Debezium Event Router의 `event.id` 필드) |
| `aggregate_type` | varchar | 라우팅 대상 토픽을 고르는 필드 (`route.by.field`) |
| `aggregate_id` | varchar | Kafka 메시지 키. 상관관계 자연키(예: `transcriptId`, `sourceRequestId`) |
| `event_type` | varchar | Kafka 헤더 `eventType`으로 실림 (예: `REPORT_INITIALIZE_REQUESTED`) |
| `payload` | jsonb/text | Kafka 메시지 값(JSON) |
| `created_at` | timestamp | 생성 시각 |

## 토픽 1: `report-events`

- **생산자**: `voice-service`의 `outbox_event` (`aggregate_type` = `report-events`로 라우팅)
- **소비자**: `rag-service`, consumer group `rag-service-report-consumer`
- **키**: `aggregateId` = `transcriptId`
- **헤더**: `eventType` = `REPORT_INITIALIZE_REQUESTED` | `REPORT_ANALYZE_REQUESTED`
- **payload (JSON)**:
  - `REPORT_INITIALIZE_REQUESTED`
    ```json
    {
      "sourceRequestId": "string (transcriptId 기반)",
      "userIds": ["string", "string"],
      "requestedAt": "ISO-8601"
    }
    ```
  - `REPORT_ANALYZE_REQUESTED`
    ```json
    {
      "sourceRequestId": "string (transcriptId 기반, INITIALIZE와 동일 값)",
      "transcriptId": "string",
      "requestedAt": "ISO-8601"
    }
    ```

## 토픽 2: `report-completed-events`

- **생산자**: `rag-service`의 `outbox_event` (`aggregate_type` = `report-completed-events`로 라우팅)
- **소비자**: `rag-service` 자기 자신, 독립된 두 consumer group
  - `rag-ws-notifier-group` (STOMP WS 알림)
  - `rag-dashboard-projector-group` (CQRS 읽기 모델 `report_dashboard_stats` 갱신)
- **키**: `aggregateId` = `sourceRequestId`
- **헤더**: `eventType` = `REPORT_COMPLETED` | `REPORT_FAILED`
- **payload (JSON)**:
  ```json
  {
    "sourceRequestId": "string",
    "reportId": "long",
    "userIds": ["string", "string"],
    "state": "COMPLETED | FAILED",
    "completedAt": "ISO-8601"
  }
  ```

## 멱등성 규칙

모든 컨슈머는 `sourceRequestId`(= outbox `aggregate_id`)를 자연키로 사용해 **이미 처리된
이벤트인지 먼저 확인**한 뒤 처리한다 (`findBySourceRequestId` 조회 후 이미 원하는 상태면 skip).
Kafka는 at-least-once만 보장하므로 재전송/중복 소비는 항상 발생할 수 있다는 전제로 설계한다.

## 실패 처리

컨슈머 처리 중 예외 발생 시 Spring Kafka `DefaultErrorHandler`가 동일 토픽에서 재시도
(3회, 2초 간격) 후에도 실패하면 `<topic>.DLT`로 이동한다. `REPORT_ANALYZE_REQUESTED` 처리
자체가 비즈니스 로직상 실패(RAG/GPT 분석 실패)한 경우는 예외가 아니라 `REPORT_FAILED`
이벤트 발행으로 처리하며, 이는 DLT 대상이 아니다.

## 교체 시 체크리스트

수제 CDC 구현이 아래를 만족하면 애플리케이션 코드 변경 없이 `connect`/`connect-init`을
교체할 수 있다.

- [ ] `outbox_event` insert를 WAL에서 읽어 위 두 토픽에 그대로 발행
- [ ] 메시지 키 = `aggregate_id`, 헤더 `eventType` = `event_type` 컬럼 값
- [ ] payload = `payload` 컬럼 값(JSON) 그대로 (가공 없이)
- [ ] `aggregate_type` 값에 따라 올바른 토픽으로 라우팅
- [ ] at-least-once 전달 (누락보다 중복이 안전한 방향)
