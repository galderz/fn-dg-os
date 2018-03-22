package fn.dg.os.vertx.player;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.KeyValueWithPrevious;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends AbstractVerticle {

   static final Logger log = Logger.getLogger(Main.class.getName());
   private RemoteCacheManager playerRemote; // protostream marshaller
   private RemoteCacheManager scoreRemote; // normal marshaller
   private RemoteCache<String, Player> playerCache;

   // Use String values to avoid:
   // java.lang.ClassNotFoundException: fn.dg.os.vertx.player.Score from [Module "org.infinispan.server.hotrod"
   private RemoteCache<String, String> scoreCache;

   private long playerTimer;
   private long scoreTimer;

   private ScoreListener listener = new ScoreListener();;

   @Override
   public void start(io.vertx.core.Future<Void> future) {
      Router router = Router.router(vertx);
      router.get("/inject").handler(this::inject);
      router.get("/leaderboard").handler(this::getLeaderboard);
      router.get("/scores/*").handler(sockJSHandler(vertx));

      vertx
         .rxExecuteBlocking(this::remoteCacheManager)
         .flatMap(x -> vertx.rxExecuteBlocking(playerCache()))
         .flatMap(x -> vertx.rxExecuteBlocking(scoreCache()))
         .flatMap(x ->
            vertx
               .createHttpServer()
               .requestHandler(router::accept)
               .rxListen(8080))
         .subscribe(
            server -> {
               log.info("Caches retrieved and HTTP server started");
               future.complete();
            }
            , future::fail
         );
   }

   @Override
   public void stop(io.vertx.core.Future<Void> future) {
      vertx.cancelTimer(playerTimer);
      vertx.cancelTimer(scoreTimer);

      vertx
         .rxExecuteBlocking(this::removeScoreListener)
         .flatMap(x -> vertx.rxExecuteBlocking(stopRemote(playerRemote)))
         .flatMap(x -> vertx.rxExecuteBlocking(stopRemote(scoreRemote)))
         .subscribe(
            server -> {
               log.info("Removed listener and stopped remotes");
               future.complete();
            }
            , future::fail
         );
   }

   private void inject(RoutingContext rc) {
      CompletableInterop
         .fromFuture(playerCache.clearAsync())
         .subscribe(
            () -> {
               Random r = new Random();

               playerTimer = vertx.setPeriodic(1000, id -> {
                  final String name = UUID.randomUUID().toString();
                  int score = r.nextInt(1000); // 3 digit number

                  final Player player = new Player(name, score);
                  log.info(String.format("put(value=%s)", player));
                  playerCache.putAsync(name, player);
               });

               scoreTimer = vertx.setPeriodic(1000, id -> {
                  JsonObject scores = new JsonObject();
                  scores.put(Task.DOG.toString(), r.nextDouble());
                  scores.put(Task.CAT.toString(), r.nextDouble());
                  scores.put(Task.PERSON.toString(), r.nextDouble());
                  scores.put(Task.PENGUIN.toString(), r.nextDouble());

                  final String url = UUID.randomUUID().toString();

                  log.info(String.format("put(value=%s)", scores));
                  scoreCache.putAsync(url, scores.toString());
               });

               rc.response().end("Injector started");
            }
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private void getLeaderboard(RoutingContext rc) {
      vertx
         .rxExecuteBlocking(leaderboard())
         .subscribe(
            json ->
               rc.response().end(json.encodePrettily())
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private Handler<Future<JsonObject>> leaderboard() {
      return f -> f.complete(queryLeaderboard(playerCache));
   }

   private static JsonObject queryLeaderboard(RemoteCache<String, Player> remoteCache) {
      log.info("Query leaderboard: ");
      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.from(Player.class)
         .orderBy("score", SortOrder.DESC)
         .maxResults(10)
         .build();
      List<Player> list = query.list();

      final JsonObject json = new JsonObject();

      JsonArray top10 = new JsonArray();
      list.forEach(player -> {
         log.info("Player: " + player);
         top10.add(new JsonObject()
            .put("name", player.getName())
            .put("score", player.getScore())
            .put("achievements", new JsonObject()));
      });

      json.put("top10", top10);
      // TODO: Current players to be calculated using some other method
      json.put("currentPlayers", remoteCache.size());

      return json;
   }

   private static Handler<RoutingContext> sockJSHandler(Vertx vertx) {
      SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
      PermittedOptions outPermit = new PermittedOptions().setAddress("image-scores");
      BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
      sockJSHandler.bridge(options, be -> {
         if (be.type() == BridgeEventType.REGISTER)
            log.info("SockJs: client connected");

         be.complete(true);
      });
      return sockJSHandler;
   }

   private void remoteCacheManager(Future<Void> f) {
      this.playerRemote = new RemoteCacheManager(
         new ConfigurationBuilder()
            .addServer()
            //.host("jdg-app-hotrod")
            .host("infinispan-app-hotrod")
            .port(11222)
            .marshaller(ProtoStreamMarshaller.class)
            .build()
      );

      this.scoreRemote = new RemoteCacheManager(
         new ConfigurationBuilder()
            .addServer()
            //.host("jdg-app-hotrod")
            .host("infinispan-app-hotrod")
            .port(11222)
            .build()
      );

      SerializationContext serialCtx =
         ProtoStreamMarshaller.getSerializationContext(playerRemote);

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      try {
         String playerSchemaFile = protoSchemaBuilder.fileName("player.proto")
            .addClass(Player.class)
            .build(serialCtx);

         RemoteCache<String, String> metadataCache = playerRemote
            .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

         metadataCache.put("player.proto", playerSchemaFile);

         f.complete(null);
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to auto-generate player.proto", e);
         f.fail(e);
      }
   }

   private Handler<Future<RemoteCache<String, Player>>> playerCache() {
      return f -> {
         final RemoteCache<String, Player> cache = playerRemote.getCache("index");
         this.playerCache = cache;
         f.complete(cache);
      };
   }

   private Handler<Future<RemoteCache<String, String>>> scoreCache() {
      return f -> {
         // TODO Should be `scores`
         final RemoteCache<String, String> cache = scoreRemote.getCache("default");
         this.scoreCache = cache;
         cache.addClientListener(listener);
         f.complete(cache);
      };
   }

   private Handler<Future<Void>> stopRemote(RemoteCacheManager remote) {
      return f -> {
         remote.stop();
         f.complete(null);
      };
   }

   private void removeScoreListener(Future<Void> f) {
      scoreCache.removeClientListener(listener);
      f.complete(null);
   }

   @ClientListener(converterFactoryName = "key-value-with-previous-converter-factory")
   private final class ScoreListener {

      @ClientCacheEntryCreated
      @SuppressWarnings("unused")
      public void handleCacheEntryEvent(
            ClientCacheEntryCustomEvent<KeyValueWithPrevious<String, String>> e) {
         System.out.println(e);
         vertx.eventBus().publish("image-scores", toJson(e));
      }

      private String toJson(ClientCacheEntryCustomEvent<KeyValueWithPrevious<String, String>> e) {
         KeyValueWithPrevious<String, String> pair = e.getEventData();
         final JsonObject json = new JsonObject();
         json.put("imageURL", pair.getKey());
         json.put("scores", new JsonObject(pair.getValue()));
         return json.encodePrettily();
      }

   }

}
