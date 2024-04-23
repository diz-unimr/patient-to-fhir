# patient-to-fhir

[![MegaLinter](https://github.com/diz-unimr/patient-to-fhir/actions/workflows/mega-linter.yml/badge.svg?branch=master)](https://github.com/diz-unimr/patient-to-fhir/actions/workflows/mega-linter.yml?query=branch%3Amaster) ![java](https://github.com/diz-unimr/patient-to-fhir/actions/workflows/build.yml/badge.svg) ![docker](https://github.com/diz-unimr/patient-to-fhir/actions/workflows/release.yml/badge.svg) [![codecov](https://codecov.io/gh/diz-unimr/patient-to-fhir/graph/badge.svg?token=pjEHHwY2Q9)](https://codecov.io/gh/diz-unimr/patient-to-fhir)

> Kafka Stream Processor, transforming patient data to FHIR

The processor reads patient data from the input topic and maps it to FHIR resource bundles
according to the MII patient module and profile specification.