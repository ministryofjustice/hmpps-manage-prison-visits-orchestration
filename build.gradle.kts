plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.1"
  kotlin("plugin.spring") version "2.3.0"
  id("org.jetbrains.kotlin.plugin.noarg") version "2.3.0"
  id("org.owasp.dependencycheck") version "12.2.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.24.0")

  implementation("org.springframework.data:spring-data-commons:4.0.2")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.jsonwebtoken:jjwt:0.13.0")
}

kotlin {
  jvmToolchain(25)
}

java {
  sourceCompatibility = JavaVersion.VERSION_24
  targetCompatibility = JavaVersion.VERSION_24
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24
  }
}

tasks.test {
  minHeapSize = "512m"
  maxHeapSize = "2g"

  // Optional but useful
  jvmArgs(
    "-XX:MaxMetaspaceSize=1024m",
    "-XX:+HeapDumpOnOutOfMemoryError",
  )
}

dependencyCheck {
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}
