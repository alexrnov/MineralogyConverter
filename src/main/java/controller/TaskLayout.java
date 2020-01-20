package controller;

import application.Mineralogy;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.task.thread.ThreadTask;

import java.io.IOException;

import static application.Mineralogy.logger;
import static application.StaticConstants.*;

abstract public class TaskLayout {
  Mineralogy mineralogy;
  MainLayout mainLayout;
  ThreadTask threadTask;
  CancelDialogLayout cancelDialogController;

  /** Установить ссылку на главный класс приложения */
  public void setMineralogy(Mineralogy application) {
    this.mineralogy = application;
  }

  /** Определяет поведение при нажатии на кнопку-"крестик" приложения */
  public void setCloseDialog() {
    mineralogy.getStage().setOnCloseRequest(e -> {
      if (threadTask != null && threadTask.isRunning()) {
        e.consume(); // прервать закрытие приложение
        // и вывести диалог отмены задачи
        createCancelTaskDialog(true);
      }
    });
  }

  /** Установить ссылку на контроллер меню выбора задач */
  public void setMainLayout(MainLayout mainLayout) {
    this.mainLayout = mainLayout;
  }

  /**
   * Выводит диалог отмены задачи.
   * @param exitFromApplication флаг определяет нужно ли закрывать приложение
   * в случае отмены задачи.
   */
  void createCancelTaskDialog(boolean exitFromApplication) {
    try {
      FXMLLoader fxmlLoader = new FXMLLoader();
      fxmlLoader.setLocation(getClass().getResource(getCancelTaskDialogPath()));
      Scene scene2 = new Scene(fxmlLoader.load(), 350, 200);
      cancelDialogController = fxmlLoader.getController();
      cancelDialogController.setThreadingTask(threadTask);
      cancelDialogController.closeApplication(exitFromApplication);
      Stage stage = new Stage();
      stage.setTitle(getNameApplication());
      stage.setResizable(false);
      stage.getIcons().add(new Image(getIconPath()));
      stage.setScene(scene2);
      // блокировать родительское окно
      stage.initModality(Modality.APPLICATION_MODAL);
      stage.show();
    } catch(IOException e) {
      logger.warning("Error create \"cancel task dialog\"");
    }
  }
}
