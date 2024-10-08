package de.unimarburg.diz.patienttofhir.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties(prefix = "fhir")
@Validated
public class FhirProperties {

    private final Systems systems = new Systems();
    @NotNull
    private Boolean generateNarrative;

    @NotNull
    private Boolean useLogicalReferences;

    public Boolean getUseLogicalReferences() {
        return useLogicalReferences;
    }

    public void setUseLogicalReferences(Boolean useLogicalReferences) {
        this.useLogicalReferences = useLogicalReferences;
    }

    @NotNull
    private Boolean useConditionalUpdate;

    public Boolean getUseConditionalUpdate() {
        return useConditionalUpdate;
    }

    public void setUseConditionalUpdate(Boolean useConditionalUpdate) {
        this.useConditionalUpdate = useConditionalUpdate;
    }

    @NotNull
    private Boolean useConditionalCreate;

    public Boolean getUseConditionalCreate() {
        return useConditionalCreate;
    }

    public void setUseConditionalCreate(Boolean useConditionalCreate) {
        this.useConditionalCreate = useConditionalCreate;
    }

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
