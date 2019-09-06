package application;

import controller.MainLayout;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import static application.StaticConstants.getIconPath;
import static application.StaticConstants.getMainLayoutPath;
import static application.StaticConstants.getNameApplication;

public class Mineralogy extends Application {
  public static Logger logger = Logger.getLogger(Mineralogy.class.getName());
  private Stage stage;
  public void start(Stage stage) {
    try {
      new Logging(logger);
      this.stage = stage;
      // загрузить корневой макет из fxml файла.
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(Mineralogy.class.getResource(getMainLayoutPath()));
      // передать контроллеру ссылку на класс приложения
      AnchorPane taskOverview = loader.load(); // получить основную панель приложения
      // создать сцену, содержащую корневой макет.
      Scene scene = new Scene(taskOverview);//создать сцену и поместить туда основную панель приложения
      stage.setScene(scene); // создать сцену
      stage.setTitle(getNameApplication());
      stage.setResizable(false);
      stage.getIcons().add(new Image(getIconPath()));

      // получить контроллер(объект) класса ControllerPril
      MainLayout mainLayout = loader.getController();
      mainLayout.setMineralogy(this);
      mainLayout.loadLayoutByDefault(0);
      stage.show(); // отобразить сцену

    } catch(Exception e) {
      logger.severe("Error create main scene application");
      Platform.exit(); // выйти из приложения
      System.exit(1);
    }
  }

  public List<File> excelFilesChooser() {
    FileChooser fs = new FileChooser();
    fs.getExtensionFilters().addAll(
      new FileChooser.ExtensionFilter("XLS", "*.xls"),
      new FileChooser.ExtensionFilter("XLSX", "*.xlsx"));
    fs.setTitle("Select file or directory");
    return fs.showOpenMultipleDialog(stage);
  }

  public File txtFileOpenDialog() {
    FileChooser fs = new FileChooser();
    fs.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("TXT", "*.txt"));
    fs.setTitle("Select file");
    return fs.showOpenDialog(stage);
  }

  public File txtFileSaveDialog() {
    FileChooser fs = new FileChooser();
    fs.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("TXT", "*.txt"));
    fs.setTitle("Save file");
    return fs.showSaveDialog(stage);
  }

  public File directoryChooser() {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle("Select directory");
    return dc.showDialog(stage);
  }

  public static void main(String[] args) {
    launch(args);
    logger.info("Stop application");
  }

  public Stage getStage() {
    return stage;
  }
}
