# kafka-docker-security-acls

The objective of this repo is to allow the testing of authorization of topics and the behavior of client
when being authorized / not authorized by the broker.

## Requirements

- Recent/latest version of docker and docker-compose
- Maven
- JDK >= 17

## Executing JUnit tests in Test Containers

This project contains an exampple of starting kaka + zookeeper in a docker-compose Test Container and running tests to:

- Create a topic
- Apply ACL to the topic
- Confirm topic & ACL were created

Run this test with:

```shell
mvn test
```

## Docker Startup 

This project can also be used to test a Kafka + Zookeeper in docker. 

To start the environment run:

`docker-compose up` (add -d to background it)

To stop the environment use:

`docker-compose down`

### Configuration details

The Kafka broker is configured with SASL/Plaintext authentication and Zookeeper is configured with SASL/Digest
authentication (Zookeeper don't support SASL/Plaintext). The brokers are adding ACLs to the Zookeeper nodes when
they write and the broker will not allow access to topics without ACLs set (`allow.everyone.if.no.acl.found=false`).

This is a strict setting and the broker has 3 users configured

* `admin` - configured as super user
* `consumer`
* `producer`

The script `topic_create.sh` creates the topic `first_topic` and set the ACLs for user `producer` to write and the
user `consumer` to read from this topic (in the case of the consumer, from any consumer group id).

The JAAS files used to configure the usernames and passwords, as well as the client credentials used by the broker
are in the directory `security` of the repo (and they are mounted as `/opt/security` in broker and in zookeeper).

The broker will expose the port 19092 as a SASL authenticated port in the localhost.

The `docker-compose.yml` is using Confluent docker images 5.4.1, although older version should work fine (the initial
version of this repo was tested in 5.3.0)

## How to use the environment for testing

### Installing the clients

It is required to have Kafka tools installed to be able to use this environment. The best way to do it in a Mac is:

`brew install kafka`

Another very handy tool is kafkacat that could be conveniently installed by doing:

`brew install kafkacat`


### Create a topic and apply ACLs

Run the following command to execute the `topic_create.sh` against kafka:

```shell
docker exec -i kafka /bin/bash - < topic_create.sh
```

Output: 

```text
WARNING: Due to limitations in metric names, topics with a period ('.') or underscore ('_') could collide. To avoid issues it is best to use either, but not both.
Created topic first_topic.
Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=first_topic, patternType=LITERAL)`:
        (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=CREATE, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=first_topic, patternType=LITERAL)`:
        (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=CREATE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW)

Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=first_topic, patternType=LITERAL)`:
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)
        (principal=User:consumer, host=*, operation=DESCRIBE, permissionType=ALLOW)

Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`:
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=first_topic, patternType=LITERAL)`:
        (principal=User:producer, host=*, operation=CREATE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW)
        (principal=User:consumer, host=*, operation=DESCRIBE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`:
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)
```

### Test kafka commands

Confirm you can execute `kafka-topics` and `kafka-acls` commands.

```shell
docker exec -it kafka /bin/bash -  < topic_confirm.sh 
```

Output:
```
List topics ...
__consumer_offsets
_confluent-command
_confluent-metrics
_confluent-telemetry-metrics
_confluent_balancer_api_state
first_topic
List acls ...
Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=first_topic, patternType=LITERAL)`:
        (principal=User:producer, host=*, operation=CREATE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=DESCRIBE, permissionType=ALLOW)
        (principal=User:consumer, host=*, operation=DESCRIBE, permissionType=ALLOW)
        (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)

Current ACLs for resource `ResourcePattern(resourceType=GROUP, name=*, patternType=LITERAL)`:
        (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)
```

### Producing to the broker

Once the tools are installed, it is possible to produce to the topic by running:

`kafka-console-producer --broker-list localhost:19092 --producer.config client-properties/producer.properties --topic first_topic`

> The command above assumes that the topic first\_topic was created and the ACLs for producing were assigned.
> To perform this action just run the script `topic\_create.sh`

### Consuming from the broker

Similarly to consumer from the topic:

`kafka-console-consumer.sh --bootstrap-server localhost:19092 --consumer.config client-properties/consumer.properties --group test-consumer-group --topic first_topic`

> The same comment from the previous section applies here regarding the ACLs

## Test results

### Kafka Console CLI

The commands below are executed from the directory where this repo was cloned (due to the client-properties relative directory)

#### Producer tests

##### No authentication configured

| Step | Action |
|---|---|
| Pre-requisites | * None |
| Test Steps | * Execute the producer<br>`kafka-console-producer.sh --broker-list localhost:19092 --topic first_topic`<br>(note that the `producer.config` is not added to cause the authentication mismatch) |
| Expected Results | * Client tries continuously to connect to the broker |

##### No authorization for the topic

| Step | Action |
|---|---|
| Pre-requisites | * Remove the producer ACL |
| Test Steps | * Execute the producer with the proper authentication<br>`kafka-console-producer.sh --broker-list localhost:19092 --producer.config client-properties/producer.properties --topic first_topic` |
| Expected Results | * Client will fail due to authorization error |

##### Remove the authorization from a running producer

| Step | Action |
|---|---|
| Pre-requisites | * Make sure the producer ACL is in place |
| Test Steps | * Execute the producer with the proper authentication<br>`kafka-console-producer.sh --broker-list localhost:19092 --producer.config client-properties/producer.properties --topic first_topic`<br> * Remove the producer ACL |
| Expected Results | * Client start producing normally<br>* Client will generate one error message for each producing attempt after the ACL removal |

#### Consumer tests

##### No authentication configured

| Step | Action |
|---|---|
| Pre-requisites | * None |
| Test Steps | * Execute the consumer<br>`kafka-console-consumer.sh --bootstrap-server localhost:19092 --group test-consumer-group --topic first_topic`<br>(note that the `consumer.config` is not added to cause the authentication mismatch) |
| Expected Results | * Client tries continuously to connect to the broker |

##### No authorization for the topic

| Step | Action |
|---|---|
| Pre-requisites | * Remove the consumer ACL |
| Test Steps | * Execute the consumer with the proper authentication<br>`kafka-console-consumer.sh --bootstrap-server localhost:19092 --consumer.config client-properties/consumer.properties --group test-consumer-group --topic first_topic` |
| Expected Results | * Client will fail due to authorization error |

##### Remove the authorization from a running consumer

| Step | Action |
|---|---|
| Pre-requisites | * Make sure the consumer ACL is in place |
| Test Steps | * Execute the consumer with the proper authentication<br>`kafka-console-consumer.sh --bootstrap-server localhost:19092 --consumer.config client-properties/consumer.properties --group test-consumer-group --topic first_topic`<br> * Remove the consumer ACL |
| Expected Results | * Client start consuming normally<br>* Client will generate one error message once the ACL is removed |

##### No authorization for the consumer group

| Step | Action |
|---|---|
| Pre-requisites | * Change the consumer ACL to authorize only a specific consumer group (different from test-consumer-group) |
| Test Steps | * Execute the consumer with the proper authentication<br>`kafka-console-consumer.sh --bootstrap-server localhost:19092 --consumer.config client-properties/consumer.properties --group test-consumer-group --topic first_topic` |
| Expected Results | * Client will fail due to authorization error |



### Kafka Java app

#

