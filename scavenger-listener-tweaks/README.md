# Testing

Start Infinispan 9.2.1.Final:

```bash
cd /opt/infinispan-server-9.2.1.Final
./bin/standalone.sh
```

Compile and deploy listener:

```bash
mvn -DskipTests=true clean package wildfly:deploy
```

Run the test:

```bash
mvn test
```