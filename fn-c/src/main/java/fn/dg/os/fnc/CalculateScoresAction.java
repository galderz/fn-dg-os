package fn.dg.os.fnc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

import java.util.Collections;

public class CalculateScoresAction {

   public static JsonObject main(JsonObject args) {
      System.out.printf("Received score: %s%n", args);

      JsonParser parser = new JsonParser();
      final JsonObject value = parser.parse(args.get("value").getAsString()).getAsJsonObject();

      MicroserviceA provider = Feign.builder()
         .client(new OkHttpClient())
         .decoder(new GsonDecoder())
         .encoder(new GsonEncoder())
         .logger(new Slf4jLogger(MicroserviceA.class))
         //.target(MicroserviceA.class, "http://fn-c-injector.myproject:8080");
         .target(MicroserviceA.class, "http://fn-c-injector-myproject.192.168.64.9.nip.io:80");

      JsonObject response = provider.forwardScore(value);

//      JsonObject response = new JsonObject();
//      response.add("score", args);
//      response.add("value", value);
      return response;
   }

   interface MicroserviceA {
      @RequestLine("POST /sink")
      @Headers("Content-Type: application/json")
      JsonObject forwardScore(JsonObject request);
   }

}
