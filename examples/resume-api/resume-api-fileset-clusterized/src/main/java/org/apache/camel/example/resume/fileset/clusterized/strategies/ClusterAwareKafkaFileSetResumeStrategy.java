package org.apache.camel.example.resume.fileset.clusterized.strategies;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.cluster.CamelClusterEventListener;
import org.apache.camel.cluster.CamelClusterMember;
import org.apache.camel.cluster.CamelClusterView;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.fileset.strategies.KafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.fileset.strategies.MultiItemCache;
import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterAwareKafkaFileSetResumeStrategy extends KafkaFileSetResumeStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareKafkaFileSetResumeStrategy.class);

    private final ZooKeeperClusterService zooKeeperClusterService;
    private final String namespace;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final CamelClusterEventListener.Leadership listener;

    public ClusterAwareKafkaFileSetResumeStrategy(String topic, MultiItemCache cache, DefaultProducerPropertyFactory producerPropertyFactory, DefaultConsumerPropertyFactory consumerPropertyFactory, ZooKeeperClusterService zooKeeperClusterService, String namespace) throws Exception {
        super(topic, cache, producerPropertyFactory, consumerPropertyFactory);
        this.zooKeeperClusterService = zooKeeperClusterService;
        this.namespace = namespace;

//        listener = new CamelClusterEventListener.Leadership() {
//            @Override
//            public synchronized void leadershipChanged(CamelClusterView view, Optional<CamelClusterMember> leader) {
//                if (view.getLocalMember().isLeader()) {
//                    try {
//                        LOG.info("This node is **NOW*** a MASTER. Removing the barrier ...");
//                        latch.countDown();
//
//                    } catch (Exception e) {
//                        LOG.error("Failed to load cache: {}", e.getMessage(), e);
//                    }
//                }
//            }
//        };
//        zooKeeperClusterService.getView(namespace).addEventListener(listener);
    }
//
//    @Override
//    protected void loadCache() throws Exception {
//        latch.await();
//
//        try {
//            super.loadCache();
//        } finally {
//            zooKeeperClusterService.getView(namespace).removeEventListener(listener);
//        }
//    }

    public void refresh() {

    }
}
