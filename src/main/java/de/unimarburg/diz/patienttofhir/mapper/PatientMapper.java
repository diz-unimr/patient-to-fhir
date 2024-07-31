package de.unimarburg.diz.patienttofhir.mapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.unimarburg.diz.patienttofhir.configuration.FhirProperties;
import de.unimarburg.diz.patienttofhir.model.PatientModel;
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
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;

@Service
public class PatientMapper implements ValueMapper<PatientModel, Bundle> {

    private final FhirProperties fhirProperties;
    private static final Logger LOG = LoggerFactory.getLogger(
        PatientMapper.class);
    private final IParser fhirParser;

    public PatientMapper(FhirProperties fhirProperties,
                         FhirContext fhirContext) {
        this.fhirProperties = fhirProperties;
        this.fhirParser = fhirContext.newJsonParser();
    }

    @Override
    public Bundle apply(PatientModel model) {

        try {
            var patient = mapPatient(model);
            return createBundle(patient);
        } catch (Exception e) {
            LOG.error("Mapping failed for Patient[{}] with id {}",
                model.getId(), model.getPatientId(), e);
            return null;
        }
    }

    private Bundle createBundle(Patient patient) {
        var bundle = new Bundle();

        // set meta information
        bundle.setType(BundleType.TRANSACTION);

        var identifier = patient
            .getIdentifierFirstRep();

        // build request
        var request = new BundleEntryRequestComponent();

        if (fhirProperties.getUseConditionalUpdate()) {
            request.setMethod(HTTPVerb.PUT);
            request.setUrl(String.format("Patient?identifier=%s|%s", identifier
                .getSystem(), identifier.getValue()));
        } else if (fhirProperties.getUseConditionalCreate()) {
            request.setMethod(HTTPVerb.POST)
                .setUrl("Patient")
                .setIfNoneExist(
                    "identifier=%s|%s".formatted(identifier.getSystem(),
                        identifier.getValue()));
        } else {
            request.setUrl("Patient/" + identifier.getValue());
        }
        // add patient resource and request
        bundle
            .addEntry()
            .setResource(patient)
            .setFullUrl("Patient/" + identifier.getValue())
            .setRequest(request);

        LOG.debug("Mapped successfully to FHIR bundle: {}",
            fhirParser.encodeResourceToString(bundle));
        return bundle;
    }

    @SuppressWarnings("checkstyle:LineLength")
    private Patient mapPatient(PatientModel model) {
        var patient = new Patient();
        // profile
        patient.setMeta(new Meta().addProfile(
                "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient")
            .setSource("#aim"));

        // last modified
        patient
            .getMeta()
            .setLastUpdated(Date.from(model.getModified()));

        // identifier
        patient.addIdentifier(new Identifier()
            .setSystem(fhirProperties
                .getSystems()
                .getPatientId())
            .setValue(model.getPatientId())
            .setType(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                .setCode("MR")))
            .setUse(Identifier.IdentifierUse.USUAL));

        // name
        patient.addName(new HumanName()
            .addGiven(StringUtils.capitalize(model.getFirstName()))
            .setFamily(StringUtils.capitalize(model.getLastName()))
            .setUse(NameUse.OFFICIAL));
        if (StringUtils.isNotBlank(model.getTitle())) {
            patient
                .getNameFirstRep()
                .addPrefix(StringUtils.capitalize(model.getTitle()));
        }

        // birthdate
        // uses application wide timezone
        if (model.getBirthDate() != null) {
            patient.setBirthDate(Date.from(model
                .getBirthDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        } else {
            patient
                .getBirthDateElement()
                .addExtension(
                    "http://hl7.org/fhir/StructureDefinition/iso21090-nullFlavor",
                    new CodeType("UNK"));
            LOG.warn(
                "Missing birth date for Patient[{}] with PID: {}. nullFlavor-Extension created instead.",
                model.getId(), model.getPatientId());
        }

        // address is missing
        patient
            .addAddress()
            .addExtension(
                "http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                new CodeType("unknown"));

        // gender
        patient.setGender(parseGender(model.getSex()));

        // invalidated by
        if (model.getInvalidatedBy() != null) {
            var link = new PatientLinkComponent().setType(LinkType.REPLACEDBY);

            if (fhirProperties.getUseLogicalReferences()) {
                // use logical reference
                link.setOther(new Reference().setIdentifier(new Identifier()
                    .setSystem(fhirProperties
                        .getSystems()
                        .getPatientId())
                    .setValue(model.getInvalidatedBy())
                    .setType(new CodeableConcept().addCoding(new Coding()
                        .setSystem(
                            "http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("MR")))));
            } else {
                link.setOther(
                    new Reference("Patient/" + model.getInvalidatedBy()));
            }
            patient
                .addLink(link)
                .setActive(false);
        }

        return patient;
    }

    private AdministrativeGender parseGender(char sex) {
        return switch (sex) {
            case 'O' -> AdministrativeGender.OTHER;
            case 'F' -> AdministrativeGender.FEMALE;
            case 'M' -> AdministrativeGender.MALE;
            default -> AdministrativeGender.UNKNOWN;
        };
    }

    public Bundle fixBundleConditional(Bundle bundle) {
        if (fhirProperties.getUseConditionalUpdate()) {
            var patientEntry = bundle.getEntryFirstRep();
            var identifier =
                ((Patient) patientEntry.getResource()).getIdentifierFirstRep();

            patientEntry
                .getRequest()
                .setUrl(String.format("Patient?identifier=%s|%s",
                    identifier.getSystem(), identifier.getValue()));
        }
        return bundle;
    }
}
