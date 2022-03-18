package org.apache.camel.example.resume.fileset.clusterized.strategies;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.fileset.strategies.KafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.fileset.strategies.MultiItemCache;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterAwareKafkaFileSetResumeStrategy<K, V> extends KafkaFileSetResumeStrategy<K,V> {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareKafkaFileSetResumeStrategy.class);

    private final DefaultConsumerPropertyFactory consumerPropertyFactory;
    private final ExecutorService executorService;

    public ClusterAwareKafkaFileSetResumeStrategy(String topic, MultiItemCache cache, DefaultProducerPropertyFactory producerPropertyFactory, DefaultConsumerPropertyFactory consumerPropertyFactory) {
        super(topic, cache, producerPropertyFactory, consumerPropertyFactory);
        this.consumerPropertyFactory = consumerPropertyFactory;

        // We need to keep refreshing the cache
        executorService = Executors.newSingleThreadExecutor();
        LOG.debug("Creating a listener / refresher");
        executorService.submit(() -> refresh());
    }

    public void refresh() {

        try {
            Properties prop = (Properties) consumerPropertyFactory.getProperties().clone();
            prop.setProperty(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

            Consumer<K, V> consumer = new KafkaConsumer<>(prop);

            consumer.subscribe(Collections.singletonList(getTopic()));

            while (true) {
                var records = consumer.poll(getPollDuration());
                if (records.isEmpty()) {
                    continue;
                }

                for (var record : records) {
                    V value = record.value();

                    LOG.trace("Read from Kafka: {}", value);
                    getCache().add(record.key(), record.value());
                }
            }
        } catch (Exception e) {
            LOG.error("Error ------> {}", e.getMessage(), e);
        }
    }
}
