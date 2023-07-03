package com.testing;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import org.testcontainers.containers.DockerComposeContainer;

public class AppTest {

    @Test
    public void testApp() throws Exception {
        // At the moment, this must be run from the project root (next to
        // docker-compose.yml)
        File file = new File("docker-compose.yml");
        try (var container = new DockerComposeContainer(file)) {
            System.out.println("Starting ....");
            container.start();

            var client = this.createKafkaAdminClient();
            var newTopic = new NewTopic("Hello.world", 1, (short) 1);
            var result = client.createTopics(Collections.singletonList(newTopic)).values();
            assertTrue(result.size() > 0);
            System.out.println("Checking topics - BEGIN");
            String gotTopic = client.listTopics().names().get().iterator().next();
            assertEquals(gotTopic, "Hello.world");
            System.out.println("Checking topics - COMPLETE (got: " + gotTopic + ")");

            System.out.println("Creating ACLs - BEGIN");
            boolean aclCreated = Acl.create(client, newTopic);
            assertTrue(aclCreated);
            System.out.println("Creating ACLs - COMPLETE");

            System.out.println("Checking ACLs - BEGIN");
            var acls = client.describeAcls(AclBindingFilter.ANY).values().get();
            assertTrue(acls.size() > 0);
            acls.forEach(System.out::println);
            System.out.println("Checking ACLs - COMPLETE");

            // Shut down docker compose containers
            container.stop();
        }
    }

    private Admin createKafkaAdminClient() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\"; "); // System.getenv("KAFKA_SASL_JAAS_CONFIG"));
        return Admin.create(props);
    }
}
