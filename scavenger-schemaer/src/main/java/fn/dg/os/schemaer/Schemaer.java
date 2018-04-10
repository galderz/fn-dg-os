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
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Schemaer extends AbstractVerticle {

    static final Logger log = Logger.getLogger(Schemaer.class.getName());

    String hotrod;
    int delay;

    RemoteCacheManager remote;
    long timerId;

    @Override
    public void start(io.vertx.core.Future<Void> future) {
        hotrod = System.getenv("HOTROD_SERVICE_NAME");
        String delayEnv = System.getenv("SCHEMA_CHECK_DELAY");
        if (Objects.isNull(hotrod) || Objects.isNull(delayEnv))
            future.fail("HOTROD_SERVICE_NAME and SCHEMA_CHECK_DELAY env variables need to be given");
        
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
                    log.info("Removed listener and stopped remotes");
                    future.complete();
                }
                , future::fail
            );
    }

    private Handler<Future<Void>> stopRemote(RemoteCacheManager remote) {
        return f -> {
            remote.stop();
            f.complete(null);
        };
    }

    private void startSchemaer(Future<Void> future) {
        this.remote = new RemoteCacheManager(
            new ConfigurationBuilder()
                .addServer()
                    .host(hotrod)
                    .port(11222)
                .marshaller(ProtoStreamMarshaller.class)
                .build()
        );

        timerId = vertx.setPeriodic(delay, id -> {
            RemoteCache<String, String> metadataCache = remote
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

            final String metadata = metadataCache.get("player.proto");
            if (metadata == null)
                registerSchema(metadataCache);
        });
    }

    private void registerSchema(RemoteCache<String, String> metaCache) {
        SerializationContext serialCtx =
            ProtoStreamMarshaller.getSerializationContext(remote);

        ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
        try {
            String playerSchemaFile = protoSchemaBuilder.fileName("player.proto")
                .addClass(Player.class)
                .build(serialCtx);

            metaCache.put("player.proto", playerSchemaFile);

            String errors = metaCache.get(".errors");
            if (errors != null)
                log.severe("Errors found in proto file: " + errors);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to auto-generate player.proto", e);
        }
    }

}
