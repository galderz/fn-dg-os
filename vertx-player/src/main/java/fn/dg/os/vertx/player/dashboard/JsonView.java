package fn.dg.os.vertx.player.dashboard;

import javafx.beans.property.SimpleStringProperty;

public class JsonView {

   private final SimpleStringProperty json;

   public JsonView(String json) {
      this.json = new SimpleStringProperty(json);
   }

   public String getJson() {
      return json.get();
   }

   public SimpleStringProperty jsonProperty() {
      return json;
   }

   public void setJson(String json) {
      this.json.set(json);
   }

}
