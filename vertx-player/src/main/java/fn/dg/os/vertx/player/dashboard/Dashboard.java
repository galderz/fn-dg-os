package fn.dg.os.vertx.player.dashboard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dashboard extends Application {

   private final TableView<JsonView> table = new TableView<>();
   private final ExecutorService exec = Executors.newSingleThreadExecutor();
   private WebsocketTask task;

   @Override
   public void start(Stage stage) {
      BorderPane root = new BorderPane();
      Scene scene = new Scene(root, 800, 600);

      table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      table.setEditable(true);

      TableColumn jsonCol = getTableCol("Json", 64, "json");

      table.getColumns().addAll(jsonCol);

      root.setCenter(table);

      task = new WebsocketTask();
      table.setItems(task.getPartialResults());
      task.exceptionProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue != null) {
            Exception ex = (Exception) newValue;
            ex.printStackTrace();
         }
      });

      exec.submit(task);

      stage.setOnCloseRequest(we -> {
         this.stop();
         System.out.println("Bye.");
      });

      stage.setTitle("Dashboard");
      stage.setScene(scene);
      stage.show();
   }

   private TableColumn getTableCol(String colName, int minWidth, String fieldName) {
      TableColumn<JsonView, String> typeCol = new TableColumn<>(colName);
      typeCol.setMinWidth(minWidth);
      typeCol.setCellValueFactory(new PropertyValueFactory<>(fieldName));
      return typeCol;
   }

   @Override
   public void stop() {
      if (task != null)
         task.cancel();

      exec.shutdown();
   }

   public static void main(String[] args) {
      launch(args);
   }

}
