package io.agentfactory.infrastructure.config

import io.agentfactory.domain.agent.persistence.IAgentExecutionPort
import io.agentfactory.domain.agent.service.AgentExecutionService
import io.agentfactory.domain.artifact.persistence.IArtifactPort
import io.agentfactory.domain.artifact.service.ArtifactService
import io.agentfactory.domain.job.persistence.IJobPort
import io.agentfactory.domain.job.service.JobService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfig {

    @Bean
    fun jobService(jobPort: IJobPort): JobService =
        JobService(jobPort)

    @Bean
    fun artifactService(artifactPort: IArtifactPort): ArtifactService =
        ArtifactService(artifactPort)

    @Bean
    fun agentExecutionService(agentExecutionPort: IAgentExecutionPort): AgentExecutionService =
        AgentExecutionService(agentExecutionPort)
}
