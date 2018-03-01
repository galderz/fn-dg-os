package fn.dg.os.wsk;

import java.nio.charset.Charset;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;

public class App {
  public static JsonObject main(JsonObject args) {
    JsonObject response = new JsonObject();
    if (args.has("text")) {
      String text = args.getAsJsonPrimitive("text").getAsString();
      try {
        Hasher hasher = Hashing.md5().newHasher();
        hasher.putString(text.toString(), Charset.forName("UTF-8"));
        response.addProperty("text", text);
        response.addProperty("md5", hasher.hash().toString());
      } catch (Exception e) {
        response.addProperty("Error", e.getMessage());
      }
    }
    return response;
  }
}
