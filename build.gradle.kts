// 모든 모듈에 공통인 설정만 둔다. 모듈마다 다른 의존성은 각 모듈의 build.gradle.kts 에 있다.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jvm-test-suite")
    apply(plugin = "io.spring.dependency-management")

    group = "com.coupon.rush"
    version = "0.0.1-SNAPSHOT"

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    // 부트 플러그인을 쓰지 않는 coupon-common 도 같은 버전을 쓰도록 BOM 을 직접 가져온다
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)
    }

    configure<TestingExtension> {
        suites {
            val unitTest = register<JvmTestSuite>("unitTest") {
                dependencies {
                    implementation(project())
                    implementation(rootProject.libs.spring.boot.starter.test)
                    compileOnly(rootProject.libs.lombok)
                    annotationProcessor(rootProject.libs.lombok)
                }
            }
            register<JvmTestSuite>("integrationTest") {
                dependencies {
                    implementation(project())
                    implementation(rootProject.libs.spring.boot.starter.test)
                    implementation(rootProject.libs.spring.boot.testcontainers)        // @ServiceConnection
                    implementation(rootProject.libs.testcontainers.junit.jupiter)
                    compileOnly(rootProject.libs.lombok)
                    annotationProcessor(rootProject.libs.lombok)
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
        configurations.named("${suite}Implementation") { extendsFrom(configurations.getByName("implementation")) }
        configurations.named("${suite}RuntimeOnly") { extendsFrom(configurations.getByName("runtimeOnly")) }
    }

    val suites = the<TestingExtension>().suites
    tasks.named("check") {
        dependsOn(suites.named("unitTest"), suites.named("integrationTest"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("-Xshare:off", "-XX:+EnableDynamicAgentLoading")
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
