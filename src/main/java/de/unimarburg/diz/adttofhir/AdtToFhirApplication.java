package de.unimarburg.diz.adttofhir;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdtToFhirApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
		SpringApplication.run(AdtToFhirApplication.class, args);
	}

}
