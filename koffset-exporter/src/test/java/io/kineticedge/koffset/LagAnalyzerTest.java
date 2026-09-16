package io.kineticedge.koffset;

import io.kineticedge.koffset.config.CollectorConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

class LagAnalyzerTest extends KafkaContainerTest {

    public void createTopic(String topic, int partitions) {
        admin().createTopics(List.of(new NewTopic(topic, partitions, (short) 1)))
                .all()
                .toCompletionStage()
                .toCompletableFuture()
                .join();
    }

    public void createGroupAtOffset(String groupId, String topic, int partition, long offset) {

        TopicPartition tp = new TopicPartition(topic, partition);
        OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(offset);

        // Map of partition to the desired offset
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.of(tp, offsetAndMetadata);

        // This will create the group if it doesn't exist, or update it if it does
        admin().alterConsumerGroupOffsets(groupId, offsets)
                .all()
                .toCompletionStage()
                .toCompletableFuture()
                .join();

        System.out.println("Group " + groupId + " initialized at offset " + offset);
    }

    @Test
    void x() throws InterruptedException {

        String topic = "test";

        createTopic(topic, 1);

        KafkaProducer<String, String> producer = new KafkaProducer<>(
                Map.ofEntries(
                        Map.entry("bootstrap.servers", bootstrapServers()),
                        Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
                        Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                )
        );

        Map<Integer, Long> timestamps = new ConcurrentHashMap<>();

        IntStream.range(0, 10).forEach(i -> {
                    final String key = "key" + i;
                    final String value = "value" + i;
                    producer.send(
                            new ProducerRecord<>(topic, key, value),
                            (metadata, exception) -> {
                                timestamps.put(i, metadata.timestamp());
                            });
                }
        );
        producer.flush();

        createGroupAtOffset("test-group", topic, 0, 5);

        CollectorConfig config = new CollectorConfig(
                30_000L,
                0L,
                100L,
                10,
                -1 // do not do the freshness check, otherwise partitionLagTimestamps will not be able to be compared to
        );

        var analyizer = new LagAnalyzer(config, admin());

        analyizer.start();

        Thread.sleep(500L);

        var lag = analyizer.lag();

        var groupLag = lag.get("test-group");

        var partitionLag = groupLag.get(new TopicPartition(topic, 0));

        Assertions.assertEquals(10L, partitionLag.partitionHeadOffset());
        Assertions.assertEquals(5L, partitionLag.groupCommittedOffset());

        Assertions.assertEquals(-1L, partitionLag.groupOffsetFirstObservedTimestamp());
        Assertions.assertEquals(5L, partitionLag.offsetLag());
        Assertions.assertEquals(0.0, partitionLag.groupVelocityRecordsPerSec());

        // max Timetamp
        Assertions.assertEquals(timestamps.get(9), partitionLag.partitionHeadTimestamp());

        //
        Assertions.assertEquals(timestamps.get(5), partitionLag.groupOffsetInterpolatedTimestamp());
        //Assertions.assertTrue(Math.abs(timestamps.get(5) - partitionLag.groupOffsetInterpolatedTimestamp()) < 1);

    }

}