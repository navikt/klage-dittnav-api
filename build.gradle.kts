import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val h2Version = "2.3.232"
val pamGeographyVersion = "2.23"
val tokenSupportVersion = "5.0.30"
val oidcSupportVersion = "0.2.18"
val logstashVersion = "8.1"
val pdfboxVersion = "3.0.5"
val tikaVersion = "3.2.2"
val resilience4jVersion = "2.3.0"
val shedlockVersion = "6.10.0"
val springDocVersion = "2.8.13"
val kodeverkVersion = "1.11.1"
val simpleSlackPosterVersion = "1.0.0"
val testContainersVersion = "1.21.3"
val mockkVersion = "1.14.5"
val springMockkVersion = "4.0.2"
val otelVersion = "1.54.0"

ext["okhttp3.version"] = "4.9.0" // For at token support testen kjører

repositories {
    mavenCentral()
    maven ("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

plugins {
    val kotlinVersion = "2.2.10"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.springframework.boot") version "3.5.5"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    idea
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    modules {
        module("org.springframework.boot:spring-boot-starter-tomcat") {
            replacedBy("org.springframework.boot:spring-boot-starter-jetty")
        }
    }
    implementation("org.eclipse.jetty.http2:jetty-http2-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("io.opentelemetry:opentelemetry-api:$otelVersion")

    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("org.flywaydb:flyway-core")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")

    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

    implementation("no.nav.klage:klage-kodeverk:$kodeverkVersion")

    implementation("ch.qos.logback:logback-classic")
    implementation("org.codehaus.janino:janino")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("no.nav.pam.geography:pam-geography:$pamGeographyVersion")
    implementation("org.apache.pdfbox:pdfbox:$pdfboxVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("io.projectreactor.kafka:reactor-kafka")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:oidc-spring-support:$oidcSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")

    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

    implementation("no.nav.slackposter:simple-slack-poster:$simpleSlackPosterVersion")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")

    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
        exclude(group = "org.mockito")
    }

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")

    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java.sourceCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}
