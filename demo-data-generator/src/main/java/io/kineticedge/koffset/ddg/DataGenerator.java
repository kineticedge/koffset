package io.kineticedge.koffset.ddg;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class DataGenerator {

    private static final Logger log = LoggerFactory.getLogger(DataGenerator.class);

    private final Random random = new Random();

    public DataGenerator() {
    }

    public void run() throws Exception {
        int topicCount = 500; // Reduced for sanity in local testing, but scales easily

        setupInfrastructure(topicCount);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Start 20 Producers
            IntStream.range(0, 10).forEach(i -> executor.submit(() -> startProducer(i, topicCount)));
            // Start 20 Consumers
            IntStream.range(0, 30).forEach(i -> {

                List<String> topics = IntStream.range(0, 4).mapToObj(j -> "demo-topic-" + random.nextInt(topicCount)).toList();

                executor.submit(() -> startConsumer(i, topics));
            });

            log.info("Simulation running. 20 virtual producers and 20 virtual consumers active.");
            Thread.currentThread().join();
        }
    }

    private void setupInfrastructure(int topicCount) throws Exception {

        Admin admin = Admin.create(adminConfig());

        log.info("Creating topics...");
        List<NewTopic> newTopics = IntStream.range(0, topicCount)
                .mapToObj(i -> new NewTopic("demo-topic-" + i, 2, (short) 1))
                .toList();

        try {
            admin.createTopics(newTopics).all().get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            log.info("Some topics already exist, continuing...", e);
        }
        Thread.sleep(500L);

        log.info("Setting Client Quotas to simulate lag...");
        List<ClientQuotaAlteration> listX = IntStream.range(0, 100).mapToObj(i -> new ClientQuotaAlteration(
                        new ClientQuotaEntity(
                                Map.ofEntries(
                                        Map.entry(ClientQuotaEntity.CLIENT_ID, "consumer-" + i)
                                )
                        ),
                        List.of(
                                //new ClientQuotaAlteration.Op("producer_byte_rate", 20480.0),
                                new ClientQuotaAlteration.Op("consumer_byte_rate", 50_000.0)
                        )
                )
        ).toList();

        List<ClientQuotaAlteration> list = List.of(
                new ClientQuotaAlteration(
                        new ClientQuotaEntity(
                                Map.ofEntries(
                                        Map.entry(ClientQuotaEntity.CLIENT_ID, "fast-consumer")
                                )
                        ),
                        List.of(
                                //new ClientQuotaAlteration.Op("producer_byte_rate", 20480.0),
                                new ClientQuotaAlteration.Op("consumer_byte_rate", 500_000.0)
                        )
                ),
                new ClientQuotaAlteration(
                        new ClientQuotaEntity(
                                Map.ofEntries(
                                        Map.entry(ClientQuotaEntity.CLIENT_ID, "slow-consumer")
                                )
                        ),
                        List.of(
                                //new ClientQuotaAlteration.Op("producer_byte_rate", 20480.0),
                                new ClientQuotaAlteration.Op("consumer_byte_rate", 10_000.0)
                        )
                )
        );


        admin.alterClientQuotas(list).all().get();

    }

    private void startProducer(int id, int topicCount) {
        try (var producer = new KafkaProducer<String, String>(producerConfig("consumer-" + id))) {
            while (!Thread.interrupted()) {
                String topic = "demo-topic-" + random.nextInt(topicCount);
                producer.send(new ProducerRecord<>(topic, random.nextInt(2), "key", "payload-" + System.nanoTime()));
                Thread.sleep(100); // 10 msgs/sec per producer
            }
        } catch (Exception ignored) {
        }
    }

    private void startConsumer(int id, Collection<String> topics) {

        String clientId = (id % 5 == 0) ? "slow-consumer" : "fast-consumer-" + id;

        try (var consumer = new KafkaConsumer<String, String>(consumerConfig("demo-group-" + id, clientId))) {
            consumer.subscribe(topics); // Regex for many topics
            while (!Thread.interrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
//                if (!records.isEmpty()) {
//                    consumer.commitSync();
//                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private Map<String, Object> adminConfig() {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        );
    }

    private Map<String, Object> producerConfig(String clientId) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"),
                Map.entry(CommonClientConfigs.CLIENT_ID_CONFIG, clientId),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)

        );
    }

    private Map<String, Object> consumerConfig(String groupId, String clientId) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"),
                Map.entry(CommonClientConfigs.CLIENT_ID_CONFIG, clientId),
                Map.entry(ConsumerConfig.GROUP_ID_CONFIG, groupId),
                Map.entry(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
        );
    }

}
