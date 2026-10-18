plugins {
    `java-library`
}

dependencies {
    // BaseTimeEntity 가 JPA 매핑과 감사(@CreatedDate) 애노테이션을 쓴다. 두 서버 모두 JPA 를 쓰므로 그대로 노출한다
    api(libs.spring.boot.starter.data.jpa)
}
