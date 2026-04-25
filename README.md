# PG Payment Strategy Sample

KCP, INICIS처럼 PG사별 인증/승인/취소 방식이 다른 결제 연동을 전략 패턴으로 재구성한 포트폴리오용 Java 샘플입니다.

기존 구조의 핵심 문제는 도메인 서비스가 `KCPPayment`, `InicisPayment` 구현체를 직접 알고 `switch`로 분기한다는 점이었습니다. 이 프로젝트는 PG 선택 책임을 `PaymentGatewayRegistry`로 분리하고, 결제 유스케이스는 `PaymentGateway` 계약만 바라보도록 구성했습니다.

## 목표

- PG사별 요청 포맷, 서명, 승인 URL, 취소 URL 차이를 어댑터 내부로 격리
- 결제 도메인 로직에서 KCP/INICIS 구현체 의존 제거
- PG 추가 시 기존 유스케이스 수정 없이 새 `PaymentGateway` 구현체 등록
- 승인 후 내부 처리 실패 시 PG 취소로 보상 처리
- 결제 생성/승인/취소 이벤트를 outbox 형태로 남길 수 있는 구조 제시

## 구조

```text
src/main/java/com/hyein/payment
  application/        결제 유스케이스
  domain/             결제 도메인 모델
  port/in/            입력 포트
  port/out/           출력 포트
  adapter/pg/         PG 전략 구현체와 Registry
  adapter/persistence 인메모리 저장소 샘플
  adapter/event       인메모리 outbox 샘플
  demo/               실행 예제
```

## 전략 패턴 적용 지점

```java
public interface PaymentGateway {
    PgCompany company();
    CheckoutSession createCheckoutSession(Payment payment);
    ApprovalResult approve(Payment payment, PgAuthResult authResult);
    CancelResult cancel(Payment payment, CancelReason reason);
}
```

`PaymentService`는 KCP, INICIS 구현체를 직접 주입받지 않습니다.

```java
PaymentGateway gateway = gatewayRegistry.get(payment.pgCompany());
ApprovalResult result = gateway.approve(payment, command.authResult());
```

따라서 TossPayments, NaverPay, KakaoPay 같은 PG가 추가되어도 결제 유스케이스는 변경하지 않고 새 어댑터만 등록하면 됩니다.

## 실행

외부 라이브러리 없이 Java 17만 있으면 컴파일과 테스트를 확인할 수 있습니다.

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src/main/java,src/test/java -Filter *.java).FullName
java -cp out com.hyein.payment.PaymentStrategyTestRunner
java -cp out com.hyein.payment.demo.PaymentStrategyDemo
```

## 포트폴리오 설명 문장

감정 서비스 결제 연동에서 KCP/INICIS 구현체를 직접 분기하던 구조를 전략 패턴 기반의 PG 어댑터 구조로 재설계했습니다. 결제 유스케이스는 `PaymentGateway` 인터페이스와 `PaymentGatewayRegistry`만 의존하도록 분리했고, PG별 서명/요청 포맷/승인/취소 로직은 각 어댑터 내부에 격리했습니다. 이를 통해 PG 전환 및 추가 시 변경 범위를 신규 어댑터 등록 수준으로 줄이고, 승인 후 내부 처리 실패 시 보상 취소까지 일관된 흐름으로 처리할 수 있도록 구성했습니다.

