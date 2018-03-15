package fn.dg.os.vertx.hello;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Handler;
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
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

   static final Logger log = Logger.getLogger(Main.class.getName());

   @Override
   public void start(io.vertx.core.Future<Void> future) {
      Router router = Router.router(vertx);
      router.get("/hello").handler(this::hello);
      router.get("/inject").handler(this::inject);
      router.get("/eventbus/*").handler(sockJSHandler(vertx));

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
         .flatMap(cache -> addEventPusher(new EventPusher(), cache).andThen(just(cache)))
         .subscribe(
            cache -> {
               Random r = new Random();

               vertx.setPeriodic(1000, id -> {
                  int playerNumber = r.nextInt(1000) + 100; // 3 digit number
                  cache.putAsync("player" + playerNumber, "blah");
               });

               rc.response().end("Injector started");
            }
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private Completable addEventPusher(Object listener, RemoteCache<String, String> cache) {
      return CompletableInterop.fromFuture(
         CompletableFuture
            .supplyAsync(() -> {
               cache.addClientListener(listener);
               return null;
            })
      );
   }

   private void hello(RoutingContext rc) {
      vertx
         .rxExecuteBlocking(Main::remoteCacheManager)
         .flatMap(remote -> vertx.rxExecuteBlocking(remoteCache(remote)))
         .flatMap(cache -> CompletableInterop.fromFuture(cache.putAsync("hola", "mundo")).andThen(just(cache)))
         .flatMap(cache -> Single.fromFuture(cache.getAsync("hola")))
         .subscribe(
            value ->
               rc.response().end("Value was: " + value)
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private static void remoteCacheManager(Future<RemoteCacheManager> f) {
      f.complete(
         new RemoteCacheManager(
            new ConfigurationBuilder().addServer()
               .host("jdg-app-hotrod")
               .port(11222)
               .build()));
   }

   private static Handler<Future<RemoteCache<String, String>>> remoteCache(RemoteCacheManager remote) {
      return f -> f.complete(remote.getCache("default"));
   }

   private static Handler<RoutingContext> sockJSHandler(Vertx vertx) {
      SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
      PermittedOptions outPermit = new PermittedOptions().setAddress("datagrid-events");
      BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
      sockJSHandler.bridge(options, be -> {
         if (be.type() == BridgeEventType.REGISTER)
            log.info("SockJs: client connected");

         be.complete(true);
      });
      return sockJSHandler;
   }

   @ClientListener
   private class EventPusher {

      @ClientCacheEntryCreated
      @SuppressWarnings("unused")
      public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
         vertx.eventBus().publish("datagrid-events", toJson(e));
      }

      private String toJson(ClientCacheEntryCreatedEvent e) {
         Map<String, Object> map = new HashMap<>();
         map.put("key", e.getKey());
         map.put("version", e.getVersion());
         return new JsonObject(map).encode();
      }

   }

}
