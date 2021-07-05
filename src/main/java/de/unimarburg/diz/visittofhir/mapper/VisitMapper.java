package de.unimarburg.diz.visittofhir.mapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.unimarburg.diz.visittofhir.configuration.FhirProperties;
import de.unimarburg.diz.visittofhir.model.VisitModel;
import java.time.ZoneId;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeType;
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
public class VisitMapper implements ValueMapper<VisitModel, Bundle> {

    private final FhirProperties fhirProperties;
    private final static Logger log = LoggerFactory.getLogger(VisitMapper.class);
    private final IParser fhirParser;

    public VisitMapper(FhirProperties fhirProperties, FhirContext fhirContext) {
        this.fhirProperties = fhirProperties;
        this.fhirParser = fhirContext.newJsonParser();
    }

    @Override
    public Bundle apply(VisitModel model) {

        try {
            var patient = mapPatient(model);
            return createBundle(patient);
        } catch (Exception e) {
            log.error("Mapping failed for Patient[{}] with id {}", model.getId(),
                model.getPatientId(), e);
            return null;
        }
    }

    private Bundle createBundle(Patient patient) {
        var bundle = new Bundle();
        var resourceId = patient.getIdentifierFirstRep()
            .getValue();

        // set meta information
        bundle.setType(BundleType.TRANSACTION);

        // build request
        var request = new BundleEntryRequestComponent().setMethod(HTTPVerb.PUT);
        if (fhirProperties.getUseConditionalUpdate()) {
            request.setUrl(String.format("Patient?identifier=%s|%s", patient.getIdentifierFirstRep()
                .getSystem(), resourceId));
        } else {
            request.setUrl("Patient/" + resourceId);
        }

        // add patient resource and request
        bundle.addEntry()
            .setResource(patient)
            .setFullUrl("Patient/" + resourceId)
            .setRequest(request);

        log.debug("Mapped successfully to FHIR bundle: {}",
            fhirParser.encodeResourceToString(bundle));
        return bundle;
    }

    private Patient mapPatient(VisitModel model) {
        var patient = new Patient();

        // profile
        patient.setMeta(
            new Meta().addProfile("https://fhir.miracum.org/core/StructureDefinition/PatientIn"));

        // identifier
        patient.addIdentifier(new Identifier().setSystem(fhirProperties.getSystems()
            .getPatientId())
            .setValue(model.getPatientId())
            .setType(new CodeableConcept().addCoding(
                new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                    .setCode("MR")))
            .setUse(Identifier.IdentifierUse.USUAL));

        // name
        patient.addName(new HumanName().addPrefix(model.getTitle())
            .addGiven(StringUtils.capitalize(model.getFirstName()))
            .setFamily(StringUtils.capitalize(model.getLastName())));
        if (StringUtils.isNotBlank(model.getTitle())) {
            patient.getNameFirstRep()
                .addPrefix(StringUtils.capitalize(model.getTitle()));
        }

        // birth date
        // uses application wide timezone
        if (model.getBirthDate() != null) {
            patient.setBirthDate(Date.from(model.getBirthDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        } else {
            patient.getBirthDateElement()
                .addExtension("http://hl7.org/fhir/StructureDefinition/iso21090-nullFlavor",
                    new CodeType("UNK"));
            log.warn(
                "Missing birth date for Patient[{}] with PID: {}. nullFlavor-Extension created instead.",
                model.getId(), model.getPatientId());
        }

        // gender
        patient.setGender(parseGender(model.getSex()));

        // invalidated by
        if (model.getInvalidatedBy() != null) {
            var link = new PatientLinkComponent().setType(LinkType.REPLACEDBY);

            if (fhirProperties.getUseConditionalUpdate()) {
                // use logical reference
                link.setOther(new Reference().setIdentifier(new Identifier().setSystem(
                    fhirProperties.getSystems()
                        .getPatientId())
                    .setValue(model.getInvalidatedBy())
                    .setType(new CodeableConcept().addCoding(
                        new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                            .setCode("MR")))));
            } else {
                link.setOther(new Reference("Patient/" + model.getInvalidatedBy()));
            }
            patient.addLink(link)
                .setActive(false);
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

    public Bundle fixBundleConditional(Bundle bundle) {
        if (fhirProperties.getUseConditionalUpdate()) {
            var patientEntry = bundle.getEntryFirstRep();
            var identifier = ((Patient) patientEntry.getResource()).getIdentifierFirstRep();

            patientEntry.getRequest()
                .setUrl(String.format("Encounter?identifier=%s|%s", identifier.getSystem(),
                    identifier.getValue()));
        }
        return bundle;
    }
}