# Instructions for Function C scores

Install Infinispan via template.

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
wsk -i trigger create cacheEntryTrigger \
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
wsk -i rule create scoresRule cacheEntryTrigger scoresPushAction
```

Add some test data:

```
cd fn-c-injector
./deploy.sh
curl http://fn-c-injector-myproject.192.168.64.9.nip.io/inject
```

Injector can be stopped via:

```bash
curl http://fn-c-injector-myproject.192.168.64.9.nip.io/stop-inject
```

Check if action triggered:

```bash
wsk -i activation list scoresPushAction

> activations
> 5da3e50fea6c4591a3e50fea6c259136 scoresPushAction
> 67c116e9164f4b278116e9164f4b27fb scoresPushAction
```

Check contents of activation:

```bash
wsk -i activation result 5da3e50fea6c4591a3e50fea6c259136

> {
>    "score": {
>        "eventType": "CREATE",
>        "key": "fc3d1922-fcc1-4258-8f70-12754d2eb5ff",
>        "value": "{\"red hat logo\":0.7,\"dog\":0.8,\"person\":0.5,\"pokemon\":0.3}"
>    }
> }
```


# Instructions for simple test

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
wsk -i trigger create cacheEntryTrigger \
    --feed redhatdevelopers/infinispan-feed \
    -p hotrod_server_host infinispan-app-hotrod \
    -p hotrod_port 11222 \
    -p cache_name default
```

Create action:

```bash
mvn clean package
wsk -i action create scoresPushAction \
  target/fn-c.jar \
  --main fn.dg.os.fnc.CalculateScoresAction
```

Create rule:

```bash
wsk -i rule create scoresRule cacheEntryTrigger scoresPushAction
```

Add some test data:

```bash
curl -X GET  --data "hello world"  \
  $(minishift openshift service infinispan-feed-provider -n myproject --url)/data/add/testKey2018
```

Check if action triggered:

```bash
wsk -i activation list --limit 1 scoresPushAction

> activations
> 44cd582dbf1340d28d582dbf1310d239 scoresPushAction
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