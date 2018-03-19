package fn.dg.os.vertx.player;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

   static final Logger log = Logger.getLogger(Main.class.getName());

   @Override
   public void start(io.vertx.core.Future<Void> future) {
      Router router = Router.router(vertx);
      router.get("/inject").handler(this::inject);
      router.get("/leaderboard").handler(this::getLeaderboard);

      vertx
         .createHttpServer()
         .requestHandler(router::accept)
         .rxListen(8080)
         .subscribe(
            server -> {
               log.info("Http server started");
               future.complete();
            }
            , future::fail
         );
   }

   private void inject(RoutingContext rc) {
      vertx
         .rxExecuteBlocking(Main::remoteCacheManager)
         .flatMap(remote -> vertx.rxExecuteBlocking(remoteCache(remote)))
         .flatMap(cache -> CompletableInterop.fromFuture(cache.clearAsync()).andThen(just(cache)))
         .subscribe(
            cache -> {
               Random r = new Random();

               vertx.setPeriodic(1000, id -> {
                  final String uuid = UUID.randomUUID().toString();
                  int score = r.nextInt(1000); // 3 digit number

                  final Player player = new Player(uuid, score);
                  log.info(String.format("put(value=%s)", player));
                  cache.putAsync(uuid, player);
               });

               rc.response().end("Injector started");
            }
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private void getLeaderboard(RoutingContext rc) {
      vertx
         .rxExecuteBlocking(Main::remoteCacheManager)
         .flatMap(remote -> vertx.rxExecuteBlocking(remoteCache(remote)))
         .flatMap(cache -> vertx.rxExecuteBlocking(leaderboard(cache)))
         .subscribe(
            json ->
               rc.response().end(json.encodePrettily())
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private static Handler<Future<JsonArray>> leaderboard(RemoteCache<String, Player> remoteCache) {
      return f -> f.complete(queryLeaderboard(remoteCache));
   }

   private static JsonArray queryLeaderboard(RemoteCache<String, Player> remoteCache) {
      log.info("Query leaderboard: ");
      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.from(Player.class)
         .orderBy("score", SortOrder.DESC)
         .maxResults(10)
         .build();
      List<Player> list = query.list();
      JsonArray json = new JsonArray();
      list.forEach(player -> {
         log.info("Player: " + player);
         // TODO: Just send back user name and score
         json.add(new JsonObject().put(player.getId().toString(), player.getScore()));
      });

      return json;
   }

   private static void remoteCacheManager(Future<RemoteCacheManager> f) {
      final RemoteCacheManager remote = new RemoteCacheManager(
         new ConfigurationBuilder()
            .addServer()
               //.host("jdg-app-hotrod")
               .host("infinispan-app-hotrod")
               .port(11222)
            .marshaller(ProtoStreamMarshaller.class)
            .build()
      );

      SerializationContext serialCtx =
         ProtoStreamMarshaller.getSerializationContext(remote);

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      try {
         String playerSchemaFile = protoSchemaBuilder.fileName("player.proto")
            .addClass(Player.class)
            .build(serialCtx);

         RemoteCache<String, String> metadataCache = remote
            .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

         metadataCache.put("player.proto", playerSchemaFile);

         f.complete(remote);
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to auto-generate player.proto", e);
         f.fail(e);
      }
   }

   private static Handler<Future<RemoteCache<String, Player>>> remoteCache(RemoteCacheManager remote) {
      return f -> f.complete(remote.getCache("index"));
   }

}
