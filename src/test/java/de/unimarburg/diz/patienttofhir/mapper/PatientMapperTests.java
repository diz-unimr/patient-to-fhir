package de.unimarburg.diz.patienttofhir.mapper;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import de.unimarburg.diz.patienttofhir.configuration.FhirConfiguration;
import de.unimarburg.diz.patienttofhir.model.PatientModel;
import de.unimarburg.diz.patienttofhir.validator.FhirProfileValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {PatientMapper.class, FhirConfiguration.class})
public class PatientMapperTests {

    @SuppressWarnings("checkstyle:VisibilityModifier")
    @Autowired
    PatientMapper mapper;

    private static final Logger LOG =
        LoggerFactory.getLogger(PatientMapperTests.class);
    private static FhirValidator validator;
    private static IParser jsonParser;

    @BeforeAll
    static void init() {
        var fhirContext = new FhirConfiguration().fhirContext();
        validator = FhirProfileValidator.create(fhirContext);
        jsonParser = fhirContext.newJsonParser();
    }

    @Test
    public void mapperCreatesBundleWithPatientResource() {
        var model = createTestModel();
        var bundle = mapper.apply(model);

        assertThat(bundle).isInstanceOf(Bundle.class)
            .extracting(x -> x.getEntryFirstRep()
                .getResource())
            .isInstanceOf(Patient.class);
    }

    @Test
    public void resourcesAreValid() {
        // arrange
        var model = createTestModel();
        var bundle = mapper.apply(model);

        // act
        var validation = validator.validateWithResult(bundle);
        FhirProfileValidator.prettyPrint(validation);
        LOG.info(jsonParser.encodeResourceToString(bundle));

        // assert
        assertThat(validation.isSuccessful()).isTrue();
    }

    @Test
    public void mapperUsesConditionalCreate() {
        var model = createTestModel();

        var bundle = mapper.apply(model);

        assertThat(bundle).isInstanceOf(Bundle.class)
            .extracting(x -> x.getEntryFirstRep().getRequest().hasIfNoneExist())
            .isEqualTo(true);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private PatientModel createTestModel() {
        var model = new PatientModel();
        model.setId(1);
        model.setBirthDate(LocalDate.now()
            .minusYears(42));
        model.setInvalidatedBy("000043");
        model.setFirstName("PETER");
        model.setLastName("LUSTIG");
        model.setPatientId("000042");
        model.setInserted(LocalDateTime.now()
            .minusDays(2)
            .atZone(ZoneId.systemDefault())
            .toInstant());
        model.setModified(LocalDateTime.now()
            .minusMinutes(5)
            .atZone(ZoneId.systemDefault())
            .toInstant());
        model.setTitle("DR.");
        model.setSex('M');

        return model;
    }

}
