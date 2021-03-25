# Template Kafka Streams processor
> This is a template for a Kafka Streams processor including boilerplate configuration.
> It can be used as a starting point when implementing a processor application.

# How to use this

Clone this repository and replace the template's placeholder name:  

1. Rename the package `de.unimarburg.diz.adttofhir` by replacing the placeholder name (**kafkastreamstemplate**) according to your desired application name.
2. Replace the placeholder names in `build.gradle` and `settings.gradle`, as well.
3. Create a processor class as a service component:
    ```java
    @Service
    public class LufuToFhirProcessor {
       // ...
    }
    ``` 

4. Change `Application.class` to something meaningful.
5. Customize the `application.yml`.
   1. Set the new package name for logging purposes.
   2. Uncomment ``cloud:`` section and change values to match your processor's method name and parameters, as well as the *Kafka Binder* settings. 
6. Replace the values for the `org.opencontainers.image` labels in the `Dockerfile`.
7. Move `.gitlab-ci-example.yml` to `.gitlab-ci.yml` (replacing the template's GitLab CI file).
8. Replace this 📄`README.md` 😊