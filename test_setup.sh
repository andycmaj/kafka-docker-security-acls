#/bin/bash

echo "List topics ..."
kafka-topics  --list --bootstrap-server localhost:9092 --command-config client-properties/adminclient.properties

echo "List acls ..."
kafka-acls --list --bootstrap-server localhost:9092 --command-config client-properties/adminclient.properties
