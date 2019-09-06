package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Контроллер, управляющий поведением диалогового окна отмены
 * выполняемой задачи.
 */
public class CancelDialogLayout {
  @FXML private Button yesButton;
  @FXML private Button noButton;
  private ButtonAnimation yesButtonAnimation;
  private ButtonAnimation noButtonAnimation;
  /* ссылка на задачу, которая выполгяется в данный момент */
  private Task threadingTask;
  /* флаг определяет - нужно ли выполнять действие при нажатии кнопки "Да" */
  private boolean activateYesButton = true;
  /* флаг определяет - нужно ли выйти из приложения */
  private boolean exitFromApplication = false;

  public void initialize() {
    yesButtonAnimation = new ButtonAnimation(yesButton, "10 29 10 29", true);

    yesButton.setOnAction(event -> {
      if (!activateYesButton) {
        return;
      }
      yesButtonAnimation.mouseOnClick();
      Stage stage = (Stage) yesButton.getScene().getWindow();
      if (threadingTask.isRunning()) {
        threadingTask.cancel();
      }
      stage.close();
      // если диалог был вызван при закрытии приложения - выйти из программы
      if (exitFromApplication) {
        Platform.exit();
      }
    });

    yesButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
      event -> yesButtonAnimation.mouseEntered());
    yesButton.addEventHandler(MouseEvent.MOUSE_EXITED,
      event -> yesButtonAnimation.mouseExited());

    noButtonAnimation = new ButtonAnimation(noButton, "10 27 10 27", true);
    noButton.setOnAction(event -> {
      noButtonAnimation.mouseOnClick();
      Stage stage = (Stage) noButton.getScene().getWindow();
      stage.close();
    });

    noButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
      event -> noButtonAnimation.mouseEntered());
    noButton.addEventHandler(MouseEvent.MOUSE_EXITED,
      event -> noButtonAnimation.mouseExited());
  }

  /** получить ссылку на задачу, которая выполняется в данный момент */
  public void setThreadingTask(Task threadingTask) {
    this.threadingTask = threadingTask;
  }

  /** деактивировать кнопку "Да", когда задача уже выполнена */
  public void noActivateYesButton() {
    activateYesButton = false;
    yesButtonAnimation.activation(false);
  }

  /**
   * @param exitFromApplication необходимо установить true - когда
   * диалоговое окно вызвано при закрытии приложении. false - когда
   * диалоговое окно вызвано при нажатии кнопки "Отмена"
   */
  public void closeApplication(boolean exitFromApplication) {
    this.exitFromApplication = exitFromApplication;
  }
}
