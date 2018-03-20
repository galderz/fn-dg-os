package fn.dg.os.vertx.player.dashboard;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

public class WebsocketTask extends Task<Void> {

   static final String HTTP_HOST = System.getProperty("http.host", "vertx-player-myproject.127.0.0.1.nip.io");
   static final int HTTP_PORT = Integer.getInteger("http.port", 80);

   private ObservableList<JsonView> partialResults =
      FXCollections.observableArrayList();

   private Vertx vertx = Vertx.vertx();
   private HttpClient client = vertx.createHttpClient();

   public final ObservableList<JsonView> getPartialResults() {
      return partialResults;
   }

   @Override
   protected Void call() throws Exception {
      connectHttp();
      return null;
   }

   private void connectHttp() {
      client.websocket(HTTP_PORT, HTTP_HOST, "/scores/websocket", ws -> {
         System.out.println("Connected");
         sendPing(ws);

         // Send pings periodically to avoid the websocket connection being closed
         vertx.setPeriodic(5000, id -> {
            sendPing(ws);
         });

         // Register
         JsonObject msg = new JsonObject().put("type", "register").put("address", "image-scores");
         ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

         ws.handler(buff -> {
            System.out.println(buff);
            JsonObject json = new JsonObject(new JsonObject(buff.toString()).getString("body"));

            Platform.runLater(() ->
               partialResults.add(new JsonView(json.toString())));
         });
      });
   }

   static void sendPing(WebSocket ws) {
      JsonObject msg = new JsonObject().put("type", "ping");
      ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));
   }

   @Override
   protected void cancelled() {
      client.close();
      vertx.close();
   }

}
