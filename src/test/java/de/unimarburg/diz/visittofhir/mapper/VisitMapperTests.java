package de.unimarburg.diz.visittofhir.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import de.unimarburg.diz.visittofhir.configuration.FhirConfiguration;
import de.unimarburg.diz.visittofhir.model.VisitModel;
import de.unimarburg.diz.visittofhir.validator.FhirProfileValidator;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {VisitMapper.class, FhirConfiguration.class})
public class VisitMapperTests {

    @Autowired
    VisitMapper mapper;

    @Value("classpath:example-pseuded-patient.json")
    Resource exampleResult;
    @Value("classpath:example-null-flavor.json")
    Resource exampleNullFlavor;

    private final static Logger log = LoggerFactory.getLogger(VisitMapperTests.class);
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
        log.info(jsonParser.encodeResourceToString(bundle));

        // assert
        assertThat(validation.isSuccessful()).isTrue();
    }

    @Test
    public void exampleResultIsValid() throws IOException {
        // arrange
        var bundle = jsonParser.parseResource(Bundle.class, exampleNullFlavor.getInputStream());

        // act
        var validation = validator.validateWithResult(bundle);
        FhirProfileValidator.prettyPrint(validation);
        log.info(jsonParser.encodeResourceToString(bundle));

        // assert
        assertThat(validation.isSuccessful()).isTrue();
    }

    private VisitModel createTestModel() {
        var model = new VisitModel();
        model.setId(1);
        model.setBirthDate(LocalDate.now()
            .minusYears(42));
        model.setInvalidatedBy("000042");
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
