plugins {
    java
    `jvm-test-suite`
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.coupon.rush"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.mysql)
    runtimeOnly(libs.mysql.connector.j)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

testing {
    suites {
        val unitTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(libs.spring.boot.starter.test)
                compileOnly(libs.lombok)
                annotationProcessor(libs.lombok)
            }
        }

        val integrationTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(libs.spring.boot.starter.test)
                implementation(libs.spring.boot.starter.webmvc.test)
                implementation(libs.spring.boot.testcontainers)        // @ServiceConnection
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.mysql)
                compileOnly(libs.lombok)
                annotationProcessor(libs.lombok)
            }
            targets.all {
                testTask.configure {
                    shouldRunAfter(unitTest)
                }
            }
        }
    }
}

listOf("unitTest", "integrationTest").forEach { suite ->
    configurations.named("${suite}Implementation") { extendsFrom(configurations.implementation.get()) }
    configurations.named("${suite}RuntimeOnly") { extendsFrom(configurations.runtimeOnly.get()) }
}

tasks.named("check") {
    dependsOn(testing.suites.named("unitTest"), testing.suites.named("integrationTest"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
