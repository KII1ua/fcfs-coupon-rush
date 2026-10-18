plugins {
    java
    `jvm-test-suite`
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":coupon-common"))
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.spring.boot.starter.jackson)
    runtimeOnly(libs.mysql.connector.j)
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(libs.testcontainers.mysql)
                implementation(libs.testcontainers.kafka)
                implementation(libs.awaitility)
            }
        }
    }
}
