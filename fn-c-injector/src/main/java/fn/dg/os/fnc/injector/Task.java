package fn.dg.os.fnc.injector;

import java.util.UUID;

public enum Task {
   RH_LOGO(Type.MAP, 10, "red hat logo")
   , DOG(Type.MAP, 20, "dog")
   , PERSON(Type.MAP, 40, "person")
   , POKEMON(Type.ISLAND, 100, "pokemon")
   ;

   final String id;
   final Type type;
   final int stage = 0;
   final int points;
   final String description;

   Task(Type type, int points, String description) {
      this.type = type;
      id = UUID.randomUUID().toString();
      this.points = points;
      this.description = description;
   }

   enum Type {
      MAP
      , ISLAND
   }

}
