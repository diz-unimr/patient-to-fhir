package de.unimarburg.diz.visittofhir;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = VisitToFhirApplication.class)
public class IntegrationTests extends TestContainerBase {

    @DynamicPropertySource
    private static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrapServers", kafka::getBootstrapServers);
        registry.add("services.pseudonymizer.url",
            () -> "http://" + pseudonymizerContainer.getHost() + ":"
                + pseudonymizerContainer.getFirstMappedPort() + "/fhir");
    }

    @BeforeAll
    public static void setupContainers() throws Exception {
        setup();
    }

    @Test
    public void bundlesArePseudonymized() {
        var messages = KafkaHelper.getAtLeast(
            KafkaHelper.createFhirTopicConsumer(kafka.getBootstrapServers()), "visit-fhir", 10);
        var resources = messages.stream()
            .map(Bundle.class::cast)
            .flatMap(x -> x.getEntry()
                .stream()
                .map(BundleEntryComponent::getResource))
            .collect(Collectors.toList());

        var pseudedCoding = new Coding("http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
            "PSEUDED", "part of the resource is pseudonymized");
        var redactedCoding = new Coding("http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
            "REDACTED", "part of the resource is removed");

        assertThat(resources).extracting(r -> r.getMeta()
            .getSecurity())
            .allSatisfy(c -> assertThat(c).usingRecursiveComparison()
                .isEqualTo(List.of(redactedCoding, pseudedCoding)));
    }

}
