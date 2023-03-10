package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Configuration
class SwaggerConfig {
  init {
    val schema = io.swagger.v3.oas.models.media.Schema<LocalTime>()
    schema.example(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))).type("string").format("HH:mm").example("13:45")
    SpringDocUtils.getConfig().replaceWithSchema(LocalTime::class.java, schema)
  }
}

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val buildName: String = buildProperties.name
  private val buildVersion: String = buildProperties.version

  @Value("\${info.app.description}")
  private val description: String = "VSIP Orchestration service used by VSIP frontend to make external API calls and collate responses."

  @Value("\${info.app.contact.name}")
  private val contactName: String = "Prison Visits Booking Project"

  @Value("\${info.app.contact.email}")
  private val contactEmail: String = "prisonvisitsbooking@digital.justice.gov.uk"

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://hmpps-manage-prison-visits-orchestration.prison.service.justice.gov.uk").description("Prod"),
        Server().url("https://hmpps-manage-prison-visits-orchestration-preprod.prison.service.justice.gov.uk").description("PreProd"),
        Server().url("https://hmpps-manage-prison-visits-orchestration-staging.prison.service.justice.gov.uk").description("Staging"),
        Server().url("https://hmpps-manage-prison-visits-orchestration-dev.prison.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title(buildName)
        .version(buildVersion)
        .description(description)
        .contact(Contact().name(contactName).email(contactEmail)),
    )
}
