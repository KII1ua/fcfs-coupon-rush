plugins {
    java
    `jvm-test-suite`
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":coupon-common"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.commons.pool2)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.mysql)
    runtimeOnly(libs.mysql.connector.j)
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(libs.spring.boot.starter.webmvc.test)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.mysql)
                implementation(libs.testcontainers.kafka)
                implementation(libs.awaitility)
            }
        }
    }
}
