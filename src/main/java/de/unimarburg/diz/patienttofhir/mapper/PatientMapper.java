package de.unimarburg.diz.patienttofhir.mapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.unimarburg.diz.patienttofhir.configuration.FhirProperties;
import de.unimarburg.diz.patienttofhir.model.PatientModel;
import java.time.ZoneId;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PatientMapper implements ValueMapper<PatientModel, Bundle> {

    private final FhirProperties fhirProperties;
    private final static Logger log = LoggerFactory.getLogger(PatientMapper.class);
    private final IParser fhirParser;

    public PatientMapper(FhirProperties fhirProperties, FhirContext fhirContext) {
        this.fhirProperties = fhirProperties;
        this.fhirParser = fhirContext.newJsonParser();
    }

    @Override
    public Bundle apply(PatientModel model) {

        try {
            var patient = mapPatient(model);
            return createBundle(patient);
        } catch (Exception e) {
            log.error("Mapping failed for Patient[{}] with id {}",
                model.getId(), model.getPatientId(), e);
            return null;
        }
    }

    private Bundle createBundle(Patient patient) {
        var bundle = new Bundle();
        var resourceId = patient.getIdentifierFirstRep().getValue();

        // set meta information
        bundle.setType(BundleType.TRANSACTION);

        // add patient resource
        bundle.addEntry().setResource(patient).setFullUrl("Patient/" + resourceId)
            .getRequest()
            .setMethod(HTTPVerb.PUT)
            .setUrl("Patient/" + resourceId);
        // conditional update
//            .setUrl(String
//                .format("Patient?identifier=%s|%s", patient.getIdentifierFirstRep().getSystem(),
//                    resourceId));

        log.debug("Mapped successfully to FHIR bundle: {}",
            fhirParser.encodeResourceToString(bundle));
        return bundle;
    }

    private Patient mapPatient(PatientModel model) {
        var patient = new Patient();

        // profile
        patient.setMeta(new Meta().addProfile(
            "https://fhir.miracum.org/core/StructureDefinition/PatientIn"));

        // identifier
        patient.addIdentifier(new Identifier().setSystem(fhirProperties.getSystems().getPatientId())
            .setValue(model.getPatientId())
            .setType(new CodeableConcept().addCoding(
                new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                    .setCode("MR"))));

        // name
        patient.addName(new HumanName()
            .addPrefix(model.getTitle())
            .addGiven(StringUtils.capitalize(model.getFirstName()))
            .setFamily(StringUtils.capitalize(model.getLastName())));
        if (StringUtils.isNotBlank(model.getTitle())) {
            patient.getNameFirstRep().addPrefix(StringUtils.capitalize(model.getTitle()));
        }

        // birth date
        // uses application wide timezone
        if (model.getBirthDate() != null) {
            patient.setBirthDate(Date.from(model.getBirthDate().atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        } else {
            log.warn(
                "Missing birth date for Patient[{}] with PID: {}. This resource will not validate against the patient profile.",
                model.getId(), model.getPatientId());
        }

        // gender
        patient.setGender(parseGender(model.getSex()));

        // invalidated by
        if (model.getInvalidatedBy() != null) {
            patient
                .addLink(new PatientLinkComponent()
                    .setOther(new Reference("Patient/" + model.getInvalidatedBy()))
                    // logical reference
//                    .setOther(new Reference().setIdentifier(
//                        new Identifier().setSystem(fhirProperties.getSystems().getPatientId())
//                            .setValue(model.getInvalidatedBy())
//                            .setType(new CodeableConcept().addCoding(
//                                new Coding()
//                                    .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
//                                    .setCode("MR")))))
                    .setType(LinkType.REPLACEDBY));
            patient.setActive(false);
        }

        return patient;
    }

    private AdministrativeGender parseGender(char sex) {
        switch (sex) {
            case 'O':
                return AdministrativeGender.OTHER;
            case 'F':
                return AdministrativeGender.FEMALE;
            case 'M':
                return AdministrativeGender.MALE;
            default:
                return AdministrativeGender.UNKNOWN;
        }
    }
}
