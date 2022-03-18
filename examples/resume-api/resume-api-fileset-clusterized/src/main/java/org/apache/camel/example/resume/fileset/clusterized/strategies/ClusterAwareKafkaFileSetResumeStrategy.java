package org.apache.camel.example.resume.fileset.clusterized.strategies;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.ResumeCache;
import org.apache.camel.cluster.CamelClusterEventListener;
import org.apache.camel.cluster.CamelClusterMember;
import org.apache.camel.cluster.CamelClusterView;
import org.apache.camel.component.file.consumer.FileResumeSet;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.fileset.strategies.KafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.fileset.strategies.MultiItemCache;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterAwareKafkaFileSetResumeStrategy<K, V> extends KafkaFileSetResumeStrategy<K,V> {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareKafkaFileSetResumeStrategy.class);

    private final ZooKeeperClusterService zooKeeperClusterService;
    private final String namespace;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final DefaultConsumerPropertyFactory consumerPropertyFactory;
//    private final CamelClusterEventListener.Leadership listener;
    private final ExecutorService executorService;

    public ClusterAwareKafkaFileSetResumeStrategy(String topic, MultiItemCache cache, DefaultProducerPropertyFactory producerPropertyFactory, DefaultConsumerPropertyFactory consumerPropertyFactory, ZooKeeperClusterService zooKeeperClusterService, String namespace) throws Exception {
        super(topic, cache, producerPropertyFactory, consumerPropertyFactory);
        this.zooKeeperClusterService = zooKeeperClusterService;
        this.namespace = namespace;
        this.consumerPropertyFactory = consumerPropertyFactory;

        executorService = Executors.newSingleThreadExecutor();
        LOG.debug("Creating a listener / refresher");
        executorService.submit(() -> refresh());

//        listener = new CamelClusterEventListener.Leadership() {
//            @Override
//            public synchronized void leadershipChanged(CamelClusterView view, Optional<CamelClusterMember> leader) {
//
//                if (view.getLocalMember().isLeader()) {
//                    try {
//                        LOG.info("This node is **NOW*** a MASTER. Removing the barrier ...");
//                        latch.countDown();
//                        executorService.shutdown();
//                    } catch (Exception e) {
//                        LOG.error("Failed to load cache: {}", e.getMessage(), e);
//                    }
//                } else {
//                    LOG.debug("Creating a listener / refresher");
//                    executorService.submit(() -> refresh());
//                }
//            }
//        };
//        zooKeeperClusterService.getView(namespace).addEventListener(listener);
    }


//    @Override
//    public void resume(FileResumeSet resumable) {
//        try {
//            latch.await();
//            super.resume(resumable);
//            zooKeeperClusterService.getView(namespace).removeEventListener(listener);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//
//        }
//    }

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

                    LOG.debug("Read from Kafka: {}", value);
                    getCache().add(record.key(), record.value());
                }
            }
        } catch (Exception e) {
            LOG.error("Error ------> {}", e.getMessage(), e);
        }
    }
}
