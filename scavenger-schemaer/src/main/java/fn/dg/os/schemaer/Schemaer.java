package fn.dg.os.schemaer;

import io.vertx.core.Handler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import me.escoffier.keynote.Player;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

public class Schemaer extends AbstractVerticle {

    static final Logger log = Logger.getLogger(Schemaer.class.getName());

    String hotrod;
    int delay;

    RemoteCacheManager remote;
    long timerId;

    AtomicInteger counter;

    @Override
    public void start(io.vertx.core.Future<Void> future) {
        hotrod = System.getenv("HOTROD_SERVICE_NAME");
        String delayEnv = System.getenv("SCHEMA_CHECK_DELAY");
        if (Objects.isNull(hotrod) || Objects.isNull(delayEnv))
            future.fail(
                "HOTROD_SERVICE_NAME and SCHEMA_CHECK_DELAY env variables need to be given"
            );
        
        delay = Integer.parseInt(delayEnv);

        vertx
            .rxExecuteBlocking(this::startSchemaer)
            .subscribe(
                x -> {
                    log.info("Schemaer started");
                    future.complete();
                }
                , future::fail
            );
    }

    @Override
    public void stop(io.vertx.core.Future<Void> future) {
        vertx.cancelTimer(timerId);

        vertx
            .rxExecuteBlocking(stopRemote(remote))
            .subscribe(
                server -> {
                    log.info(
                        "Timer cancelled and remote connection stopped"
                    );
                    future.complete();
                }
                , future::fail
            );
    }

    private Handler<Future<Void>> stopRemote(RemoteCacheManager remote) {
        return f -> {
            if (remote != null && remote.isStarted())
                remote.stop();
            f.complete();
        };
    }

    private void startSchemaer(Future<Void> future) {
        timerId = vertx.setTimer(getDelay(), id -> {
            tryAddSchema();

            vertx.setTimer(getDelay(), x -> tryAddSchema());
        });

        future.complete();
    }

    private void tryAddSchema() {
        try {
            this.remote = new RemoteCacheManager(
                new ConfigurationBuilder()
                    .addServer()
                    .host(hotrod)
                    .port(11222)
                    .marshaller(ProtoStreamMarshaller.class)
                    .build()
            );

            RemoteCache<String, String> metadataCache = remote
                .getCache(PROTOBUF_METADATA_CACHE_NAME);

            final String metadata = metadataCache.get("player.proto");
            if (metadata == null)
                registerSchema(metadataCache);
            else
                log.info(String.format(
                    "Player schema already registered in service=%s", hotrod
                ));
        } catch (Throwable t) {
            log.log(Level.WARNING, "Schemaer failed", t);
            counter.set(0);
        } finally {
            this.remote.stop();
        }
    }

    private void registerSchema(RemoteCache<String, String> metaCache) {
        log.info(String.format(
            "Register Player schema in service=%s", hotrod
        ));

        SerializationContext serialCtx =
            ProtoStreamMarshaller.getSerializationContext(remote);

        ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
        try {
            String playerSchemaFile = protoSchemaBuilder
                .fileName("player.proto")
                .addClass(Player.class)
                .build(serialCtx);

            metaCache.put("player.proto", playerSchemaFile);

            String errors = metaCache.get(".errors");
            if (errors != null)
                log.severe("Errors found in proto file: " + errors);
        } catch (IOException e) {
            log.log(
                Level.SEVERE
                , "Unable to auto-generate player.proto"
                , e
            );
        }
    }

    // milliseconds
    private int getDelay() {
        final int count = this.counter.get();
        if (count < 10)
            return 5_000;
        else if (count < 20)
            return 10_000;
        else if (count < 30)
            return 30_000;

        return 60_000;
    }

}
