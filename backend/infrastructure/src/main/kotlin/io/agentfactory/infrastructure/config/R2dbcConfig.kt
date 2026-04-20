package io.agentfactory.infrastructure.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional

@Configuration
@EnableR2dbcRepositories(
    basePackages = ["io.agentfactory.infrastructure.persistence.repository.r2dbc"],
)
@EnableR2dbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class R2dbcConfig {

    @Bean
    fun r2dbcTransactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager =
        R2dbcTransactionManager(connectionFactory)

    @Bean
    fun auditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { Mono.just("system") }

    @Bean
    fun auditingDateTimeProvider(): DateTimeProvider =
        DateTimeProvider { Optional.of(OffsetDateTime.now(ZoneOffset.UTC)) }
}
