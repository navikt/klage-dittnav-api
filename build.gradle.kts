import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.3.0"
val jacksonVersion = "2.10.1"
val prometheusVersion = "0.7.0"
val logbackVersion = "1.2.3"
val logstashVersion = "5.1"
val konfigVersion = "1.6.10.0"
val kotlinLoggerVersion = "1.7.6"
//val tjenestespesifikasjonerVersion = "1.2019.12.18-12.22-ce897c4eb2c1"
val cxfVersion = "3.3.1"
val coroutinesVersion = "1.3.3"
val wireMockVersion = "2.19.0"
val mockkVersion = "1.9"
val junitJupiterVersion = "5.4.0"
val assertkVersion = "0.21"
val restAssuredVersion = "4.2.0"
val resilience4jVersion = "1.2.0"
val threetenVersion = "1.5.0"

val mainClass = "no.nav.klage.ApplicationKt"

//fun tjenestespesifikasjon(name: String) = "no.nav.tjenestespesifikasjoner:$name:$tjenestespesifikasjonerVersion"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/tjenestespesifikasjoner")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}



dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:latest.release")
    implementation("io.micrometer:micrometer-registry-influx:latest.release")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("com.natpryce:konfig:$konfigVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggerVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.threeten:threeten-extra:$threetenVersion")

    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("javax.activation:activation:1.1.1")
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.4")
    implementation("com.sun.xml.ws:jaxws-tools:2.3.1") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
//    implementation(tjenestespesifikasjon("person-v3-tjenestespesifikasjon"))
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")

}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6.3"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
    transform(ServiceFileTransformer::class.java) {
        setPath("META-INF/cxf")
        include("bus-extensions.txt")
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")

sourceSets["main"].resources.srcDirs("src/resources")
sourceSets["test"].resources.srcDirs("test/testresources")
