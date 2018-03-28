package fn.dg.os.fnc.injector;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import static fn.dg.os.fnc.injector.Task.DOG;
import static fn.dg.os.fnc.injector.Task.PERSON;
import static fn.dg.os.fnc.injector.Task.POKEMON;
import static fn.dg.os.fnc.injector.Task.RH_LOGO;

public class Injector extends AbstractVerticle {

   static final Logger log = Logger.getLogger(Injector.class.getName());

   static final Map<String, String> TASKS = new HashMap<>();

   private RemoteCacheManager remote;
   private RemoteCache<String, String> tasksCache;
   private RemoteCache<String, String> scoresCache;

   private long scoreTimer;

   static {
      initTask(RH_LOGO);
      initTask(DOG);
      initTask(PERSON);
      initTask(POKEMON);
   }

   private static void initTask(Task task) {
      TASKS.put(UUID.randomUUID().toString(), new JsonObject()
         .put("description", task.description)
         .put("type", task.type.toString().toLowerCase())
         .put("stage", task.stage)
         .put("points", task.points)
         .encode()
      );
   }

   @Override
   public void start(io.vertx.core.Future<Void> future) throws Exception {
      Router router = Router.router(vertx);
      router.get("/inject").handler(this::inject);
      router.get("/stop-inject").handler(this::stopInject);

      router.route("/sink*").handler(BodyHandler.create());
      router.post("/sink").handler(this::sink);

      vertx
         .rxExecuteBlocking(this::remoteCacheManager)
         .flatMap(x -> vertx.rxExecuteBlocking(scoresCache()))
         .flatMap(x -> vertx.rxExecuteBlocking(tasksCache()))
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

   private void stopInject(RoutingContext rc) {
      log.info("Stop injector");
      final boolean cancelled = vertx.cancelTimer(scoreTimer);

      if (cancelled)
         rc.response().end("Injector stopped");
      else
         rc.response().end("Injector had not been started");
   }

   private void sink(RoutingContext rc) {
      final String body = rc.getBodyAsString();
      System.out.println("Body is: " + body);

      rc.response().end("{done: true}");
   }

   @Override
   public void stop(io.vertx.core.Future<Void> future) throws Exception {
      log.info("Stop the injector...");

      vertx.cancelTimer(scoreTimer);

      vertx
         .rxExecuteBlocking(stopRemote(remote))
         .subscribe(
            server -> {
               log.info("Cancelled timer and stop remotes");
               future.complete();
            }
            , future::fail
         );
   }

   private void inject(RoutingContext rc) {
      initCaches()
         .subscribe(
            () -> {
               Random r = new Random();

               DecimalFormat df = new DecimalFormat("#.#");
               scoreTimer = vertx.setPeriodic(1000, id -> {

                  // Key
                  final String txId = UUID.randomUUID().toString();

                  // Value
                  JsonObject value = new JsonObject();

                  final String url = UUID.randomUUID().toString();
                  value.put("imageURL", url);

                  final String taskId = Task.values()[r.nextInt(TASKS.values().size())].id;
                  value.put("taskId", taskId);

                  JsonObject scores = new JsonObject();
                  scores.put(Task.RH_LOGO.description,
                     Double.parseDouble(df.format(r.nextDouble())));
                  scores.put(Task.DOG.description,
                     Double.parseDouble(df.format(r.nextDouble())));
                  scores.put(Task.PERSON.description,
                     Double.parseDouble(df.format(r.nextDouble())));
                  scores.put(Task.POKEMON.description,
                     Double.parseDouble(df.format(r.nextDouble())));
                  value.put("scores", scores);

                  log.info(String.format("put(key=%s, value=%s)", txId, value));
                  scoresCache.putAsync(txId, value.toString());
               });

               rc.response().end("Injector started");
            }
            , failure ->
               rc.response().end("Failed: " + failure)
         );
   }

   private Completable initCaches() {
      return CompletableInterop
         .fromFuture(
            scoresCache
               .clearAsync()
               .thenCompose(x -> tasksCache.clearAsync())
               .thenCompose(x -> tasksCache.putAllAsync(TASKS))
         );
   }

   private void remoteCacheManager(Future<Void> f) {
      this.remote = new RemoteCacheManager(
         new ConfigurationBuilder()
            .addServer()
            //.host("jdg-app-hotrod")
            .host("infinispan-app-hotrod")
            .port(11222)
            .build()
      );

      f.complete(null);
   }

   private Handler<Future<RemoteCache<String, String>>> tasksCache() {
      return f -> {
         final RemoteCache<String, String> cache = remote.getCache("tasks");
         this.tasksCache = cache;
         f.complete(cache);
      };
   }

   private Handler<Future<RemoteCache<String, String>>> scoresCache() {
      return f -> {
         final RemoteCache<String, String> cache = remote.getCache("scores");
         this.scoresCache = cache;
         f.complete(cache);
      };
   }

   private Handler<Future<Void>> stopRemote(RemoteCacheManager remote) {
      return f -> {
         remote.stop();
         f.complete(null);
      };
   }

}
