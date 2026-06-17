# PG Payment Strategy Sample

KCP, INICIS처럼 PG사별 결제 요청 포맷과 승인 API가 다른 환경에서, 실제 실무 결제 흐름을 설명할 수 있도록 단순화한 Java 샘플입니다.

이 샘플은 다음 흐름을 코드로 표현합니다.

1. 백엔드에서 주문/사용자/금액 기반으로 결제 세션을 준비한다.
2. 프론트는 PG SDK 또는 결제창을 호출한다.
3. 프론트가 받은 승인용 값(`approval_key`, `authToken` 등)을 백엔드로 전달한다.
4. 백엔드는 PG 승인 API를 다시 호출해 최종 승인 여부를 판단한다.
5. PG webhook이 있다면 서버에서 후속 정합성 처리를 한다.

## 왜 이렇게 나눴는가

- 프론트는 PG SDK 실행과 리다이렉트 처리에 집중
- 백엔드는 PG 승인/검증의 최종 판단자 역할
- PG사별 차이는 `PaymentGateway` 구현체 내부에 격리
- 결제 서비스는 KCP/INICIS/Toss 구현체를 직접 알지 않음

## 구조

```text
src/main/java/com/hyein/payment
  application/        결제 유스케이스
  domain/             결제 도메인 모델
  port/in/            create/approve/webhook 입력 모델
  port/out/           gateway/repository/event 출력 포트
  adapter/pg/         PG별 전략 구현체
  adapter/persistence 인메모리 저장소 샘플
  adapter/event       인메모리 이벤트 발행 샘플
  demo/               실행 예제
```

## 핵심 포인트

```java
public interface PaymentGateway {
    PgCompany company();
    CheckoutSession createCheckoutSession(Payment payment);
    ApprovalResult approve(Payment payment, PgApprovalPayload approvalPayload);
    WebhookResult parseWebhook(WebhookPayload webhookPayload);
    CancelResult cancel(Payment payment, CancelReason reason);
}
```

`PaymentService`는 PG사별 `req`나 HTTP 요청 객체를 직접 받지 않습니다.  
컨트롤러나 API 레이어에서 프론트 요청을 해석한 뒤, 정제된 커맨드와 payload만 서비스로 전달하는 구조를 전제로 둡니다.

## 표현한 실무 포인트

- 결제 사용자명, 연락처, 결제 금액, 결제 수단 같은 공통 필드
- 프론트 성공/실패 리다이렉트 URL
- 백엔드 webhook URL
- PG별로 다른 승인 payload 이름
- 승인 이후 webhook 기반 후속 정합성 처리 포인트

## 실행

외부 라이브러리 없이 Java 17만 있으면 컴파일과 테스트를 확인할 수 있습니다.

```sh
javac -encoding UTF-8 -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.hyein.payment.PaymentStrategyTestRunner
java -cp out com.hyein.payment.demo.PaymentStrategyDemo
```

## 포트폴리오 설명 문장

실무 결제 연동에서 KCP/INICIS PG별 승인 파라미터와 서버 간 검증 흐름이 달랐기 때문에, 결제 준비/클라이언트 승인값 수신/백엔드 승인 API 호출/webhook 정합성 처리 흐름을 전략 패턴 기반의 PG 어댑터 구조로 재구성했습니다. 결제 유스케이스는 공통 서비스에서 처리하고, PG별 요청 포맷과 서명 및 승인 로직은 각 어댑터 내부에 격리해 PG 추가나 교체 시 변경 범위를 최소화했습니다.
