package fn.dg.os.dashboard;

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

   private final TableView<KeyVersionView> table = new TableView<>();
   private final ExecutorService exec = Executors.newSingleThreadExecutor();
   private WebsocketTask task;

   @Override
   public void start(Stage stage) {
      BorderPane root = new BorderPane();
      Scene scene = new Scene(root, 800, 600);

      table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      table.setEditable(true);

      TableColumn keyCol = getTableCol("Key", 10, "key");
      TableColumn versionCol = getTableCol("Version", 64, "version");

      table.getColumns().addAll(keyCol, versionCol);

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
      TableColumn<KeyVersionView, String> typeCol = new TableColumn<>(colName);
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
