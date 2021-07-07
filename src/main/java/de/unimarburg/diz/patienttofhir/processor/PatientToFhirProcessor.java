package de.unimarburg.diz.patienttofhir.processor;

import de.unimarburg.diz.FhirPseudonymizer;
import de.unimarburg.diz.patienttofhir.mapper.PatientMapper;
import de.unimarburg.diz.patienttofhir.model.PatientModel;
import java.util.function.Function;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class PatientToFhirProcessor {

    private final PatientMapper patientMapper;
    private final FhirPseudonymizer fhirPseudonymizer;

    @Autowired
    public PatientToFhirProcessor(PatientMapper patientMapper,
        FhirPseudonymizer fhirPseudonymizer) {
        this.patientMapper = patientMapper;
        this.fhirPseudonymizer = fhirPseudonymizer;
    }

    @Bean
    public Function<KTable<String, PatientModel>, KStream<String, Bundle>> process() {

        return patient -> patient.
            mapValues(patientMapper)
            .toStream()
            .filter((k, v) -> v != null)
            .mapValues(fhirPseudonymizer::process)
            .mapValues(patientMapper::fixBundleConditional);
    }

}
