package de.unimarburg.diz.visittofhir.processor;

import de.unimarburg.diz.FhirPseudonymizer;
import de.unimarburg.diz.visittofhir.mapper.VisitMapper;
import de.unimarburg.diz.visittofhir.model.VisitModel;
import java.util.function.Function;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class VisitToFhirProcessor {

    private final VisitMapper visitMapper;
    private final FhirPseudonymizer fhirPseudonymizer;

    @Autowired
    public VisitToFhirProcessor(VisitMapper visitMapper, FhirPseudonymizer fhirPseudonymizer) {
        this.visitMapper = visitMapper;
        this.fhirPseudonymizer = fhirPseudonymizer;
    }

    @Bean
    public Function<KTable<String, VisitModel>, KStream<String, Bundle>> process() {

        return visit -> visit.
            mapValues(visitMapper)
            .toStream()
            .filter((k, v) -> v != null)
            .mapValues(fhirPseudonymizer::process)
            .mapValues(visitMapper::fixBundleConditional);
    }

}
