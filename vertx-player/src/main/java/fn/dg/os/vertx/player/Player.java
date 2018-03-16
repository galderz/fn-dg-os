package fn.dg.os.vertx.player;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import java.util.UUID;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "Player")
public class Player {

   private String id;
   private int score;

   // Required for proto schema builder
   public Player() {
   }

   public Player(String id, int score) {
      this.score = score;
      this.id = id;
   }

   @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
   @ProtoField(number = 10, required = true)
   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
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
         "id='" + id + '\'' +
         ", score=" + score +
         '}';
   }

}
