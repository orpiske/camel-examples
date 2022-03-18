/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.example.resume.fileset.clusterized.main;

import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.FileDeserializer;
import org.apache.camel.example.resume.clients.kafka.FileSerializer;
import org.apache.camel.example.resume.fileset.clusterized.strategies.ClusterAwareKafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.fileset.clusterized.strategies.ClusterizedLargeDirectoryRouteBuilder;
import org.apache.camel.example.resume.fileset.strategies.MultiItemCache;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Camel Application
 */
public class MainApp {
    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();

        ZooKeeperClusterService clusterService = new ZooKeeperClusterService();

        String nodeId = System.getProperty("resume.example.node.id");
        String nodeHost = System.getProperty("resume.example.zk.host");

        clusterService.setId(nodeId);
        clusterService.setNodes(nodeHost);
        clusterService.setBasePath("/camel/cluster");

        main.addMainListener(new MainListener() {
            @Override
            public void beforeInitialize(BaseMainSupport main) {
                LOG.info("Before initialize");
            }

            @Override
            public void beforeConfigure(BaseMainSupport main) {
                LOG.info("Before configure");
                try {
                    LOG.info("Starting the cluster service");
                    main.getCamelContext().addService(clusterService);

                    ClusterAwareKafkaFileSetResumeStrategy resumeStrategy = getUpdatableConsumerResumeStrategyForSet();
                    main.getCamelContext().getRegistry().bind("testResumeStrategy", resumeStrategy);

                    RouteBuilder routeBuilder = new ClusterizedLargeDirectoryRouteBuilder();
                    main.getCamelContext().addRoutes(routeBuilder);
                } catch (Exception e) {
                    LOG.error("Unable to add the cluster service: {}", e.getMessage(), e);
                    System.exit(1);
                }
            }

            @Override
            public void afterConfigure(BaseMainSupport main) {
            }

            @Override
            public void configure(CamelContext context) {
            }

            @Override
            public void beforeStart(BaseMainSupport main) {
            }

            @Override
            public void afterStart(BaseMainSupport main) {
            }

            @Override
            public void beforeStop(BaseMainSupport main) {
            }

            @Override
            public void afterStop(BaseMainSupport main) {
                main.shutdown();
                System.exit(0);
            }
        });

        main.run(args);
    }

    private static ClusterAwareKafkaFileSetResumeStrategy getUpdatableConsumerResumeStrategyForSet() {
        String bootStrapAddress = System.getProperty("bootstrap.address", "localhost:9092");
        String kafkaTopic = System.getProperty("resume.type.kafka.topic", "offsets");

        final DefaultConsumerPropertyFactory consumerPropertyFactory = new DefaultConsumerPropertyFactory(bootStrapAddress);

        consumerPropertyFactory.setKeyDeserializer(FileDeserializer.class.getName());
        consumerPropertyFactory.setValueDeserializer(FileDeserializer.class.getName());
        consumerPropertyFactory.setOffsetReset("earliest");

        final DefaultProducerPropertyFactory producerPropertyFactory = new DefaultProducerPropertyFactory(bootStrapAddress);

        producerPropertyFactory.setKeySerializer(FileSerializer.class.getName());
        producerPropertyFactory.setValueSerializer(FileSerializer.class.getName());

        MultiItemCache<File, File> cache = new MultiItemCache<>();

        return new ClusterAwareKafkaFileSetResumeStrategy(kafkaTopic, cache, producerPropertyFactory,
                consumerPropertyFactory);
    }

}

