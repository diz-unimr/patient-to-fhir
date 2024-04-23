package de.unimarburg.diz.patienttofhir.configuration;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.miracum.kafka.serializers.KafkaFhirSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    private static final Logger LOG =
        LoggerFactory.getLogger(KafkaConfiguration.class);

    @Bean
    public StreamsBuilderFactoryBeanConfigurer streamsBuilderCustomizer() {
        return factoryBean -> factoryBean.setKafkaStreamsCustomizer(
            kafkaStreams -> kafkaStreams.setUncaughtExceptionHandler(
                e -> {
                    LOG.error("Uncaught exception occurred.", e);
                    // default handler response
                    return StreamsUncaughtExceptionHandler.
                        StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
                }));
    }

    @Bean
    public Serde<IBaseResource> fhirSerde() {
        return new KafkaFhirSerde();
    }
}
