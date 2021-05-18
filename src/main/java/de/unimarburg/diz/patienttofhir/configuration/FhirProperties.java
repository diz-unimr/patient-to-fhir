package de.unimarburg.diz.patienttofhir.configuration;

import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties(prefix = "fhir")
@Validated
public class FhirProperties {

    private final Systems systems = new Systems();
    @NotNull
    private Boolean generateNarrative;

    public Systems getSystems() {
        return systems;
    }

    public Boolean getGenerateNarrative() {
        return generateNarrative;
    }

    public void setGenerateNarrative(Boolean generateNarrative) {
        this.generateNarrative = generateNarrative;
    }

    public static class Systems {

        @NotNull
        private String patientId;

        public String getPatientId() {
            return patientId;
        }

        public void setPatientId(String patientId) {
            this.patientId = patientId;
        }
    }
}
