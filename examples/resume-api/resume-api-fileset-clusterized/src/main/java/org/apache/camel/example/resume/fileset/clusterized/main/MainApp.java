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
import org.apache.camel.example.resume.fileset.clusterized.strategies.ClusterizedLargeDirectoryRouteBuilder;
import org.apache.camel.example.resume.fileset.strategies.KafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.fileset.strategies.MultiItemCache;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListener;

/**
 * A Camel Application
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();

        ZooKeeperClusterService clusterService = new ZooKeeperClusterService();

        String nodeId = System.getProperty("resume.example.node.id");
        String nodeHost = System.getProperty("resume.example.zk.host");

        clusterService.setNamespace("");
        clusterService.setId(nodeId);
        clusterService.setNodes(nodeHost);
        clusterService.setBasePath("/camel/cluster");

        main.addMainListener(new MainListener() {
            @Override
            public void beforeInitialize(BaseMainSupport main) {
                try {
                    clusterService.start();
                    main.getCamelContext().addService(clusterService);
                } catch (Exception e) {
                    System.out.println("Unable to add the cluster service");
                    System.exit(1);
                }
            }

            @Override
            public void beforeConfigure(BaseMainSupport main) {

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

            }
        });

        KafkaFileSetResumeStrategy resumeStrategy = getUpdatableConsumerResumeStrategyForSet();
        RouteBuilder routeBuilder = new ClusterizedLargeDirectoryRouteBuilder(resumeStrategy);

        main.configure().addRoutesBuilder(routeBuilder);

        main.run(args);
    }

    private static KafkaFileSetResumeStrategy getUpdatableConsumerResumeStrategyForSet() {
        String bootStrapAddress = System.getProperty("bootstrap.address", "localhost:9092");
        String kafkaTopic = System.getProperty("resume.type.kafka.topic", "offsets");

        final DefaultConsumerPropertyFactory consumerPropertyFactory = new DefaultConsumerPropertyFactory(bootStrapAddress);

        consumerPropertyFactory.setKeyDeserializer(FileDeserializer.class.getName());
        consumerPropertyFactory.setValueDeserializer(FileDeserializer.class.getName());
        consumerPropertyFactory.setOffsetReset("earliest");
        consumerPropertyFactory.setGroupId("resume-cluster");

        final DefaultProducerPropertyFactory producerPropertyFactory = new DefaultProducerPropertyFactory(bootStrapAddress);

        producerPropertyFactory.setKeySerializer(FileSerializer.class.getName());
        producerPropertyFactory.setValueSerializer(FileSerializer.class.getName());

        MultiItemCache<File, File> cache = new MultiItemCache<>();

        return new KafkaFileSetResumeStrategy(kafkaTopic, cache, producerPropertyFactory, consumerPropertyFactory);
    }

}

