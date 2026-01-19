package io.kineticedge.koffset;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public abstract class KafkaContainerTest {

    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    private static Admin admin;

    @BeforeAll
    static void setup() {
        kafka.start();
        kafka.waitingFor(new DockerHealthcheckWaitStrategy());
        admin = Admin.create(
                Map.ofEntries(Map.entry(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
                )
        );
    }

    @AfterAll
    static void teardown() {
        admin.close();
        kafka.stop();
    }

    public static String bootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public static Admin admin() {
        return admin;
    }

    public static int randomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    @Test
//    void testKafkaIsReady() throws Exception {
//        // Using the apache/kafka-native image for fast startup
//        try (var kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"))) {
//            kafka.start();
//
//            Map<String, Object> config = Map.of(
//                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
//            );
//
//            try (Admin admin = Admin.create(config)) {
//                ListTopicsResult topics = admin.listTopics();
//                Set<String> names = topics.names().get(10, TimeUnit.SECONDS);
//
//                System.out.println(names);
//                // Should be empty but successful
//               // assertTrue(names.isEmpty() || names.contains("_schemas"));
//            }
//        }
//    }
}