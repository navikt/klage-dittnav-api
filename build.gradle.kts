import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion = "0.45.0"
val mockkVersion = "1.13.8"
val h2Version = "2.2.224"
val pamGeographyVersion = "2.9"
val tokenValidationVersion = "1.3.0"
val tokenSupportVersion = "3.1.9"
val oidcSupportVersion = "0.2.18"
val logstashVersion = "7.4"
val pdfboxVersion = "3.0.0"
val tikaVersion = "2.9.1"
val resilience4jVersion = "2.1.0"
val problemSpringWebStartVersion = "0.27.0"
val shedlockVersion = "5.10.0"
val springDocVersion = "2.2.0"
val kodeverkVersion = "1.5.5"
val simpleSlackPosterVersion = "0.1.4"
val mockitoInlineVersion = "5.2.0"

val githubUser: String by project
val githubPassword: String by project

ext["okhttp3.version"] = "4.9.0" // For at token support testen kjører

repositories {
    mavenCentral()
    maven ("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

plugins {
    val kotlinVersion = "1.9.21"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.springframework.boot") version "3.2.0"
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

    implementation("org.apache.tomcat.embed:tomcat-embed-core:10.1.16")
    implementation("org.apache.tomcat.embed:tomcat-embed-el:10.1.16")
    implementation("org.apache.tomcat:tomcat-annotations-api:10.1.16")
    implementation("org.apache.tomcat.embed:tomcat-embed-websocket:10.1.16")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("org.flywaydb:flyway-core")
    implementation("com.zaxxer:HikariCP")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql")

    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

    implementation("no.nav.klage:klage-kodeverk:$kodeverkVersion")

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

    implementation("no.nav.slackposter:simple-slack-poster:$simpleSlackPosterVersion")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
    }
    testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java.sourceCompatibility = JavaVersion.VERSION_17

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
