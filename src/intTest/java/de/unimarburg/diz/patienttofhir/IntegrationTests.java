package de.unimarburg.diz.patienttofhir;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = PatientToFhirApplication.class)
public class IntegrationTests extends TestContainerBase {

    private static final int FETCH_COUNT = 10;

    @DynamicPropertySource
    private static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrapServers",
            kafka::getBootstrapServers);
    }

    @BeforeAll
    public static void setupContainers() throws Exception {
        setup();
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Test
    public void bundlesAreMapped() {
        var messages = KafkaHelper.getAtLeast(
            KafkaHelper.createFhirTopicConsumer(kafka.getBootstrapServers()),
            "patient-fhir", FETCH_COUNT);
        var resources = messages
            .stream()
            .map(Bundle.class::cast)
            .flatMap(x -> x
                .getEntry()
                .stream()
                .map(BundleEntryComponent::getResource))
            .collect(Collectors.toList());

        assertThat(resources)
            .flatExtracting(r -> r
                .getMeta()
                .getProfile())
            .extracting(PrimitiveType::getValue)
            .containsOnly(
                "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient");
    }

}
