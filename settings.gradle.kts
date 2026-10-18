rootProject.name = "fcfs-coupon"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "coupon-common",     // 두 서버가 함께 쓰는 엔티티 기반 클래스와 Kafka 메시지 계약
    "coupon-api",        // 발급 서버: 회원/정책/회차 관리, Redis 판정, Kafka 발행
    "coupon-consumer",   // 처리 서버: Kafka 소비, 발급 기록 저장
)
