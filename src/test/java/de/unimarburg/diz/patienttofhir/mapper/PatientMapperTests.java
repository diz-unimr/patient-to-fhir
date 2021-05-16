package de.unimarburg.diz.patienttofhir.mapper;

import de.unimarburg.diz.patienttofhir.configuration.FhirConfiguration;
import java.io.IOException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {PatientMapper.class, FhirConfiguration.class})
public class PatientMapperTests {

    // TODO @Test
    public void mapperCreatesBundle() {
    }

    // TODO @Test
    public void mapperCreatesPatient() {
    }

    // TODO @Test
    public void resourcesAreValid() throws IOException {
//        var validator = FhirProfileValidator.create(fhirContext);
//
//        var report = getTestReport(testReport, testObservations);
//
//        var bundle = mapper.apply(report).getValue();
//
//        var validations = bundle.getEntry()
//            .stream()
//            .map(x -> validator.validateWithResult(x.getResource()))
//            .collect(Collectors.toList());
//
//        validations.forEach(FhirProfileValidator::prettyPrint);
//
//        assertThat(validations).allMatch(ValidationResult::isSuccessful);
    }


}
