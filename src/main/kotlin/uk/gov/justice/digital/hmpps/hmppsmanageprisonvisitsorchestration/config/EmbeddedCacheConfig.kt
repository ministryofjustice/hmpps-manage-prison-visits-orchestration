package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class EmbeddedCacheConfig {
  @Bean
  fun config(): Config {
    val config = Config()
    config.mapConfigs["UserFullName"] = MapConfig().setTimeToLiveSeconds(60)
    return config
  }
}
