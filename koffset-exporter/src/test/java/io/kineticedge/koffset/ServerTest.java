package io.kineticedge.koffset;

import io.kineticedge.koffset.config.ServerConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclOperation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    private static int randomPort() {
        try (ServerSocket socket = new ServerSocket()) {
            // Reuse address to avoid "Address already in use" if the test runs in rapid succession
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", 0));
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find an available port", e);
        }
    }

    @Test
    void x() {
        LagAnalyzer lagAnalyzer = Mockito.mock(LagAnalyzer.class);
        AutoAdjuster autoAdjuster = Mockito.mock(AutoAdjuster.class);

        ServerConfig config = new ServerConfig(randomPort());
        Server server = new Server(config, lagAnalyzer, autoAdjuster);

        final long now = System.currentTimeMillis();

        Mockito.when(lagAnalyzer.getRefreshedAt())
                .thenReturn(-1L)
                .thenReturn(now);

        lagAnalyzer.groupMetadata();
        lagAnalyzer.lag();

        //

        // --- MOCK DATA SETUP ---
        String group = "test-group";
        String topic = "test-topic";
        TopicPartition tp = new TopicPartition(topic, 0);


        // 1. Mock Group Metadata
        //ConsumerGroupDescription metadata = new ConsumerGroupDescription(group, "STABLE", Set.of(tp));
        //Mockito.when(lagAnalyzer.groupMetadata()).thenReturn(Map.of(group, metadata));

        ConsumerGroupDescription metadata = new ConsumerGroupDescription(
                "GROUP_ID",
                true,
                List.of(),
                "PA",
                GroupType.CLASSIC,
                GroupState.STABLE,
                new Node(1, "host", 9999),
                Set.<AclOperation>of(),
                Optional.<Integer>empty(),
                Optional.<Integer>empty()
        );
        Mockito.when(lagAnalyzer.groupMetadata()).thenReturn(Map.of(group, metadata));

        // 2. Mock LagDetail (matching the fields in your LagAnalyzerTest)
        LagAnalyzer.LagDetail lag = new LagAnalyzer.LagDetail(
                10L,         // partitionHeadOffset
                now - 100,   // partitionHeadTimestamp
                5L,          // groupCommittedOffset
                now - 5000,  // groupOffsetInterpolatedTimestamp
                -1L,         // groupOffsetFirstObservedTimestamp
                5L,          // offsetLag
                0.0          // groupVelocityRecordsPerSec
        );

        Mockito.when(lagAnalyzer.lag()).thenReturn(Map.of(group, Map.of(tp, lag)));



        //

        try {
            server.start();

            HttpClient client = HttpClient.newHttpClient();

            //

            HttpRequest readyz = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + config.getPort() + "/readyz"))
                    .GET()
                    .build();
            var readyzResponse = client.send(readyz, HttpResponse.BodyHandlers.ofString());
            assertEquals(503, readyzResponse.statusCode());
            assertEquals("503 Service Unavailable (Initializing)", readyzResponse.body());

            //
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + config.getPort() + "/metrics"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(response.body());

            assertEquals(200, response.statusCode());
            assertNotNull(response.body());

            assertTrue(response.body().contains("koffset_refreshed_ts{}"));
            assertTrue(response.body().contains("koffset_refreshed_duration_seconds{}"));
            assertTrue(response.body().contains("koffset_refreshed_age_seconds{}"));
            assertTrue(response.body().contains("koffset_number_of_groups{} 1"));
            assertTrue(response.body().contains("koffset_number_of_group_partitions{} 1"));
            assertTrue(response.body().contains("koffset_group_info{group=\"test-group\",state=\"STABLE\",coordinator=\"1\"} 1"));
            assertTrue(response.body().contains("koffset_group_members{group=\"test-group\"} 0"));
            assertTrue(response.body().contains("koffset_group_members_assigned{group=\"test-group\"} 0"));
            assertTrue(response.body().contains("koffset_latest_offset{topic=\"test-topic\",partition=\"0\"} 10"));

            assertTrue(response.body().contains("koffset_latest_offset_ts{topic=\"test-topic\",partition=\"0\"} ")); //TS
            assertTrue(response.body().contains("koffset_group_offset{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 5"));
            assertTrue(response.body().contains("koffset_group_offset_interpolated_ts{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} "));
            assertTrue(response.body().contains("koffset_group_offset_observed_ts{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} NaN"));
            assertTrue(response.body().contains("koffset_group_lag{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 5"));
            assertTrue(response.body().contains("koffset_group_lag_seconds{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 4.900"));
            assertTrue(response.body().contains("koffset_group_lag{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 5"));
            assertTrue(response.body().contains("koffset_group_lag_seconds{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 4.900"));
            assertTrue(response.body().contains("koffset_group_offset_stale_seconds{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 4.900"));
            assertTrue(response.body().contains("koffset_group_velocity_records_per_sec{group=\"test-group\",topic=\"test-topic\",partition=\"0\"} 0.00"));

            //

            HttpRequest readyz2 = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + config.getPort() + "/readyz"))
                    .GET()
                    .build();
            var readyzResponse2 = client.send(readyz, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, readyzResponse2.statusCode());
            assertEquals("READY", readyzResponse2.body());

            //

            HttpRequest healthz = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + config.getPort() + "/healthz"))
                    .GET()
                    .build();
            var healthzResponse = client.send(healthz, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, healthzResponse.statusCode());
            assertEquals("OK", healthzResponse.body());

            //

            HttpRequest notFound = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + config.getPort() + "/NOTFOUND"))
                    .GET()
                    .build();
            var notFoundResponse = client.send(notFound, HttpResponse.BodyHandlers.ofString());
            assertEquals(404, notFoundResponse.statusCode());
            assertEquals("404 Not Found", notFoundResponse.body());

        } catch (IOException | InterruptedException e) {
        } finally {
            server.stop();
            // make sure there are no errors or issues if stopped is called again
            server.stop();
        }

    }
}