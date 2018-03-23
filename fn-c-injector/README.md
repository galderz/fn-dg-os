# Instructions for injector test

Install Infinispan Feed Provider.
Instructions below assumes you have a CouchDB and OpenWhisk set up available:

```bash
cd infinispan-feed-provider
mvn clean package fabric8:deploy
```

Install Infinispan Feed Action:

```bash
wsk -i package create --shared yes redhatdevelopers

cd infinispan-feed-action
mvn clean package

wsk -i action update -a feed true redhatdevelopers/infinispan-feed \
  target/infinispan-feed-action.jar \
  --main org.workspace7.openwhisk.InfinispanFeedAction
```

Create trigger:

```bash
wsk -i trigger create scoresTrigger \
    --feed redhatdevelopers/infinispan-feed \
    -p hotrod_server_host infinispan-app-hotrod \
    -p hotrod_port 11222 \
    -p cache_name scores
```

Create action:

```bash
cd fn-c
mvn clean package
wsk -i action create scoresPushAction \
  target/fn-c.jar \
  --main fn.dg.os.fnc.CalculateScoresAction
```

Create rule:

```bash
wsk -i rule create scoresRule scoresTrigger scoresPushAction
```

Deploy injector:

```bash
cd fn-c-injector
./deploy.sh
```

Start injector:

```bash
curl http://fn-c-injector-myproject.192.168.64.9.nip.io/inject
```

Check if action triggered:

```bash
wsk -i activation list scoresPushAction

> activations
> 44cd582dbf1340d28d582dbf1310d239 scoresPushAction
...
```

Check contents of activation:

```bash
wsk -i activation result 44cd582dbf1340d28d582dbf1310d239

> {
>     "score-received": {
>         "eventType": "UPDATE",
>         "key": "testKey2018",
>         "value": "N.A",
>         "verison": "4294967298"
>     }
> }
```