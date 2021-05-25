package de.unimarburg.diz.patienttofhir;

import static org.assertj.core.api.Fail.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class TestContainerBase {

    protected static KafkaContainer kafka;
    protected static GenericContainer pseudonymizerContainer;

    protected static void setup() throws Exception {

        var network = Network.newNetwork();

        // setup & start aim db
        var aimDb = createAimDbContainer(network);
        aimDb.start();

        // pseudonymization
        pseudonymizerContainer = createPseudonymizerContainer(network);
        pseudonymizerContainer.start();

        // setup & start kafka
        kafka = createKafkaContainer(network);
        kafka.start();

        // setup & start kafka connect
        createKafkaConnectContainer(kafka);

        // kafdrop
        createKafdropContainer(network);
    }

    private static PostgreSQLContainer createAimDbContainer(Network network) {
        return new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:11-alpine")).withDatabaseName("aim")
            .withUsername("aim")
            .withPassword("test")
            .withNetwork(network)
            .withNetworkAliases("aim-db")
            .withClasspathResourceMapping("initdb", "/docker-entrypoint-initdb.d",
                BindMode.READ_ONLY);
    }

    public static KafkaContainer createKafkaContainer(Network network) {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.0")).withNetwork(
            network)
            .withEnv(Map.of("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1",
                "KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1"));
    }

    private static GenericContainer createKafkaConnectContainer(KafkaContainer kafka)
        throws Exception {
        Integer restPort = 8083;
        var connectProps = new HashMap<String, String>();
        connectProps.put("CONNECT_BOOTSTRAP_SERVERS", kafka.getNetworkAliases()
            .get(0) + ":9092");
        connectProps.put("CONNECT_REST_PORT", String.valueOf(restPort));
        connectProps.put("CONNECT_GROUP_ID", "connect-group");
        connectProps.put("CONNECT_CONFIG_STORAGE_TOPIC", "docker-connect-configs");
        connectProps.put("CONNECT_OFFSET_STORAGE_TOPIC", "docker-connect-offsets");
        connectProps.put("CONNECT_STATUS_STORAGE_TOPIC", "docker-connect-status");
        connectProps.put("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter");
        connectProps.put("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter");
        connectProps.put("CONNECT_INTERNAL_KEY_CONVERTER",
            "org.apache.kafka.connect.json.JsonConverter");
        connectProps.put("CONNECT_INTERNAL_VALUE_CONVERTER",
            "org.apache.kafka.connect.json.JsonConverter");
        connectProps.put("CONNECT_REST_ADVERTISED_HOST_NAME", "kafka-connect");
        connectProps.put("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1");
        connectProps.put("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1");
        connectProps.put("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1");
        connectProps.put("CONNECT_PLUGIN_PATH", "/usr/share/java");

        var connect = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-kafka-connect:5.5.0")).withEnv(connectProps)
            .waitingFor(Wait.forHttp("/connectors"))
            .withNetwork(kafka.getNetwork())
            .withExposedPorts(restPort);

        // create patient-db connector
        connect.start();
        addConnector(connect, "connect/connect-patient.json");

        return connect;
    }

    private static void addConnector(GenericContainer connect, String configFilename)
        throws Exception {
        // post connect configuration to REST endpoint
        var host =
            "http://" + connect.getHost() + ":" + connect.getFirstMappedPort() + "/connectors";
        var request = HttpRequest.newBuilder()
            .uri(new URI(host))
            .headers("Content-Type", "application/json", "Accept", "application/json")
            .POST(BodyPublishers.ofFile(Path.of(ClassLoader.getSystemResource(configFilename)
                .toURI())))
            .build();

        var response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            fail("Error setting up aim-db connector: " + response);
        }
    }

    private static GenericContainer createPseudonymizerContainer(Network network) {
        var gpasContainer = createGpasContainer(network);

        return new GenericContainer<>(DockerImageName.parse(
            "registry.diz.uni-marburg.de/docker/fhir-pseudonymizer:1.6.0")).withEnv(
            Collections.singletonMap("GPAS__URL", "http://gpas:8080/gpas/gpasService"))
            .withClasspathResourceMapping("anonymization.yaml", "/etc/anonymization.yaml",
                BindMode.READ_ONLY)
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/fhir/metadata"))
            .withExposedPorts(8080);
    }


    private static GenericContainer createGpasContainer(Network network) {
        var container = new GenericContainer<>(
            DockerImageName.parse("tmfev/gpas:1.9.1")).withNetworkAliases("gpas")
            .withNetwork(network)
            .withExposedPorts(8080)
            .waitingFor(Wait.forListeningPort());

        container.start();
        try {
            initGpas(
                String.format("http://%s:%d", container.getHost(), container.getFirstMappedPort()));
        } catch (Exception e) {
            fail("Error setting up GPas: ");
        }

        return container;
    }

    private static void createKafdropContainer(Network network) {
        var container = new GenericContainer<>(
            DockerImageName.parse("obsidiandynamics/kafdrop:3.27.0")).withNetworkAliases("kafdrop")
            .withNetwork(network)
            .withExposedPorts(9000)
            .withEnv(Map.of("KAFKA_BROKERCONNECT", kafka.getNetworkAliases()
                .get(0) + ":9092", "SERVER_SERVLET_CONTEXTPATH", "/"))
            .waitingFor(Wait.forListeningPort());

        container.start();
    }

    private static HttpResponse<String> createDomain(String host, String file) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(new URI(host + "/gpas/DomainService"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofFile(Path.of(ClassLoader.getSystemResource(file)
                    .toURI())))
                .build();

            return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            fail("Error create GPas Domain.", e.getMessage());
            return null;
        }
    }

    private static void initGpas(String host) {
        // patient domain
        var response = createDomain(host, "gpas/createPatientDomain.xml");
        if (response.statusCode() != 200) {
            fail("Error setting up gpas patient domain: " + response);
        }
    }
}


