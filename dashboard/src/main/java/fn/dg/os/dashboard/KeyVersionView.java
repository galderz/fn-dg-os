package fn.dg.os.dashboard;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class KeyVersionView {

   private final SimpleStringProperty key;
   private final SimpleLongProperty version;

   public KeyVersionView(String key
      , long version
   ) {
      this.key = new SimpleStringProperty(key);
      this.version = new SimpleLongProperty(version);
   }

   public String getKey() {
      return key.get();
   }

   public SimpleStringProperty keyProperty() {
      return key;
   }

   public void setKey(String key) {
      this.key.set(key);
   }

   public long getVersion() {
      return version.get();
   }

   public SimpleLongProperty versionProperty() {
      return version;
   }

}
