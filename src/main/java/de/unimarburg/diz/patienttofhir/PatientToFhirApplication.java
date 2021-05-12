package de.unimarburg.diz.patienttofhir;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatientToFhirApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
		SpringApplication.run(PatientToFhirApplication.class, args);
	}

}
