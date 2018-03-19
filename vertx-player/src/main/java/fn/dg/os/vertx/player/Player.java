package fn.dg.os.vertx.player;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "Player")
public class Player {

   private String name;
   private int score;

   // Required for proto schema builder
   public Player() {
   }

   public Player(String name, int score) {
      this.score = score;
      this.name = name;
   }

   @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
   @ProtoField(number = 10, required = true)
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @ProtoDoc("@IndexedField")
   @ProtoField(number = 20, required = true)
   public int getScore() {
      return score;
   }

   public void setScore(int score) {
      this.score = score;
   }

   @Override
   public String toString() {
      return "Player{" +
         "name='" + name + '\'' +
         ", score=" + score +
         '}';
   }

}
