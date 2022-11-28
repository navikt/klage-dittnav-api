import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion = "0.41.1"
val mockkVersion = "1.13.2"
val h2Version = "2.1.214"
val pamGeographyVersion = "2.9"
val tokenValidationVersion = "1.3.0"
val tokenSupportVersion = "2.1.9"
val oidcSupportVersion = "0.2.18"
val logstashVersion = "7.2"
val pdfboxVersion = "2.0.27"
val tikaVersion = "2.6.0"
val springSleuthVersion = "3.1.5"
val resilience4jVersion = "1.7.1"
val problemSpringWebStartVersion = "0.27.0"
val shedlockVersion = "4.42.0"
val springDocVersion = "1.6.12"

val githubUser: String by project
val githubPassword: String by project

ext["okhttp3.version"] = "4.9.0" // For at token support testen kjører

repositories {
    mavenCentral()
    maven ("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven("https://jitpack.io")
}

plugins {
    val kotlinVersion = "1.7.21"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.springframework.boot") version "2.7.5"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    idea
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth:$springSleuthVersion")

    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("org.flywaydb:flyway-core")
    implementation("com.zaxxer:HikariCP")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql")

    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

    implementation("com.github.navikt:klage-kodeverk:1.1.5")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("ch.qos.logback:logback-classic")
    implementation("ch.qos.logback:logback-access")
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

    implementation("com.github.navikt:simple-slack-poster:0.0.3")
    implementation("org.zalando:problem-spring-web-starter:$problemSpringWebStartVersion")
    implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
    }
    testImplementation("org.mockito:mockito-inline:4.9.0")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions{
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
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
