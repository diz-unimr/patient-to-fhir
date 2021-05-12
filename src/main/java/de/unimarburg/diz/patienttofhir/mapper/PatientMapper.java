package de.unimarburg.diz.patienttofhir.mapper;

import de.unimarburg.diz.patienttofhir.model.PatientModel;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class PatientMapper implements ValueMapper<PatientModel, Bundle> {

    @Override
    public Bundle apply(PatientModel value) {

        return null;
    }
}
