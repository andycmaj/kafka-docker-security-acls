package com.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateAclsOptions;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class Acl {

  public static boolean create(Admin client, NewTopic topic)
      throws InterruptedException, ExecutionException {
    ArrayList<AclBinding> bindings = new ArrayList<>();
    ResourcePattern topicResource = new ResourcePattern(
        ResourceType.TOPIC,
        topic.name(),
        PatternType.LITERAL);

    String customerPrinciple = "User:producer";
    String customerHost = "localhost:9092";

    AccessControlEntry writeAccess = new AccessControlEntry(
        customerPrinciple,
        customerHost,
        AclOperation.READ,
        AclPermissionType.ALLOW);
    AccessControlEntry readAccess = new AccessControlEntry(
        customerPrinciple,
        customerHost,
        AclOperation.WRITE,
        AclPermissionType.DENY);
    bindings.add(new AclBinding(topicResource, writeAccess));
    bindings.add(new AclBinding(topicResource, readAccess));
    CreateAclsOptions options = new CreateAclsOptions();
    options.timeoutMs(50000);

    CreateAclsResult result = client.createAcls(bindings);
    result.all().get();
    boolean isDone = result.all().isDone();

    return isDone;
  }

}
