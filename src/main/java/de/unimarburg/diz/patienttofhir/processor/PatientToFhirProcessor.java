package de.unimarburg.diz.patienttofhir.processor;

import de.unimarburg.diz.patienttofhir.mapper.PatientMapper;
import de.unimarburg.diz.patienttofhir.model.PatientModel;
import org.apache.kafka.streams.kstream.KStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class PatientToFhirProcessor {

    private final PatientMapper patientMapper;

    @Autowired
    public PatientToFhirProcessor(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @Bean
    public Function<KStream<String, PatientModel>,
            KStream<String, IBaseResource>> process() {

        return patient -> patient.mapValues(patientMapper)
                .filter((k, v) -> v != null)
                .mapValues(patientMapper::fixBundleConditional);
    }

}
