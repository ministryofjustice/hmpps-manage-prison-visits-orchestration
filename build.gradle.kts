plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.1"
  kotlin("plugin.spring") version "1.8.0"
  kotlin("plugin.jpa") version "1.8.0"
  id("org.jetbrains.kotlin.plugin.noarg") version "1.8.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
// https://mvnrepository.com/artifact/org.springframework.data/spring-data-commons
  implementation("org.springframework.data:spring-data-commons:3.0.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.14")
  implementation("org.springdoc:springdoc-openapi-ui:1.6.14")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.14")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.14")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}
repositories {
  mavenCentral()
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
