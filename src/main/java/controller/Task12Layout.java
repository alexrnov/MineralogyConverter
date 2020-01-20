package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.task.thread.ManyFilesThreadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static application.StaticConstants.*;

/**
 * Контроллер для интерфейса первой задачи Минералогия(Web-ресурс)
 * в Micromine. В этой задаче используется набор excel-файлов,
 * которые загружены с веб-ресурса "МСА по всем объектам"
 */
public class Task12Layout extends TaskLayout {

  @FXML private TextField inputFolderTextField;
  @FXML private TextField outputFolderTextField;
  @FXML private TextField stratigraphicTextField;
  @FXML private Button inputFolderButton;
  @FXML private Button outputFolderButton;
  @FXML private Button runTaskButton;
  @FXML private Button cancelTaskButton;
  @FXML private TextArea consoleTextArea;
  @FXML private ProgressBar progressBar;
  @FXML private Label processPercentLabel;
  @FXML private ComboBox<String> probeVolumeComboBox;
  @FXML private ComboBox<String> selectionAgeComboBox;
  @FXML private CheckBox referenceVolumeCheckBox;
  @FXML private CheckBox amendmentCheckBox;
  @FXML private CheckBox createDotFileCheckBox;

  private ButtonAnimation runTaskAnimation;
  private ButtonAnimation cancelTaskAnimation;

  @FXML
  private void initialize() {
    consoleTextArea.setEditable(false);
    consoleTextArea.setWrapText(true); // автоперенос строк в консоли

    createFolderButton(inputFolderButton, inputFolderTextField,
            new ImageView(getOpenDialogPath()));
    createFolderButton(outputFolderButton, outputFolderTextField,
            new ImageView(getOpenDialogPath()));
    createButtonRunTask();
    createButtonCancelTask();

    inputFolderTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(inputFolderTextField);
      }
    });

    outputFolderTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(outputFolderTextField);
      }
    });

    referenceVolumeCheckBox.setOnAction(e -> {
      if (referenceVolumeCheckBox.isSelected()) {
        probeVolumeComboBox.setDisable(false);
      } else {
        probeVolumeComboBox.setDisable(true);
      }
    });

    ObservableList<String> volumes = FXCollections.observableArrayList(
            "5 л", "10 л", "15 л", "20 л");
    probeVolumeComboBox.getItems().addAll(volumes);
    probeVolumeComboBox.setValue("5 л");
    referenceVolumeCheckBox.setSelected(true);

    ObservableList<String> ages = FXCollections.observableArrayList("Все пробы",
            "По всем возрастам", "Без возрастов", "Указать возраст");
    selectionAgeComboBox.getItems().addAll(ages);
    selectionAgeComboBox.setValue("Все пробы");

    createDotFileCheckBox.setSelected(true);
    amendmentCheckBox.setSelected(true);

    stratigraphicTextField.setDisable(true);

    // активировать текстовое поле для ввода стратиграфического индекса
    // только когда выбран пункт меню "Указать возраст"
    selectionAgeComboBox.setOnAction(e -> {
      String selectItem = selectionAgeComboBox.getValue();
      if (selectItem.equals("Указать возраст")) {
        stratigraphicTextField.setDisable(false);
      } else {
        stratigraphicTextField.setDisable(true);
      }
    });

    // задать предел длины текстового поля для ввода стратиграфического индекса
    stratigraphicTextField.textProperty().addListener((ov, oldValue, newValue) -> {
      final byte maxLength = 9;
      if (stratigraphicTextField.getText().length() > maxLength) {
        String s = stratigraphicTextField.getText().substring(0, maxLength);
        stratigraphicTextField.setText(s);
      }
    });
  }

  private void createFolderButton(Button inputFolderButton, TextField textInputFolder, ImageView openDialogPathImage) {
    inputFolderButton.setGraphic(openDialogPathImage);
    ButtonAnimation animation = new ButtonAnimation(inputFolderButton,
            "5 5 5 5", true);
    inputFolderButton.setOnAction(e -> {
      File f = mineralogy.directoryChooser();
      if (f != null) {
        textInputFolder.setText(f.getPath());
        defaultStyle(textInputFolder);
      }
    });
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> animation.mouseEntered());
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> animation.mouseExited());
  }

  private void createButtonRunTask() {
    runTaskAnimation = new ButtonAnimation(runTaskButton,
            "10 15 10 15", true);
    runTaskButton.setText(getTextRunButton());
    runTaskButton.setOnAction(event -> {
      runTaskAnimation.mouseOnClick();
      runTask();
    });
    runTaskButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> runTaskAnimation.mouseEntered());
    runTaskButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> runTaskAnimation.mouseExited());
  }

  private void createButtonCancelTask() {
    cancelTaskAnimation = new ButtonAnimation(cancelTaskButton,
            "10 24 10 24", false);
    cancelTaskButton.setText(getTextCancelButton());
    cancelTaskButton.setOnAction(event -> {
      cancelTaskAnimation.mouseOnClick();
      if (threadTask != null && threadTask.isRunning()) {
        createCancelTaskDialog(false);
      }
    });
    cancelTaskButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> cancelTaskAnimation.mouseEntered());
    cancelTaskButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> cancelTaskAnimation.mouseExited());
  }

  private void runTask() {

    if (threadTask != null && threadTask.isRunning()) return;

    if (!checkInputParameters()) return;

    //подазумевается if (threadTask == null || threadTask.isDone())
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("inputFolder", inputFolderTextField.getText());
    parameters.put("outputFolder", outputFolderTextField.getText());
    parameters.put("useAmendment", amendmentCheckBox.isSelected());
    parameters.put("useReferenceVolume", referenceVolumeCheckBox.isSelected());
    parameters.put("probeVolume", probeVolumeComboBox.getValue());
    parameters.put("createDotFile", createDotFileCheckBox.isSelected());

    String s = selectionAgeComboBox.getValue();
    if (selectionAgeComboBox.getValue().equals("Указать возраст")) {
      s = s + ":" + stratigraphicTextField.getText();
    }
    parameters.put("typeOfSelectionAge", s);

    threadTask = new ManyFilesThreadTask(mainLayout.getNameOfCurrentTask(),
            parameters);

    threadTask.setOnRunning(event -> {
      mainLayout.blockListView(true);
      mainLayout.changeStyleListView();
      runTaskAnimation.activation(false);
      cancelTaskAnimation.activation(true);
    });

    threadTask.setOnSucceeded(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
      if (cancelDialogController != null) {
        cancelDialogController.noActivateYesButton();
      }
      //threadTask.getValue());
    });

    threadTask.setOnCancelled(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      threadTask.clearProgressBar();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
    });

    threadTask.setOnFailed(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      threadTask.clearProgressBar();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
    });

    progressBar.progressProperty().bind(threadTask.progressProperty());
    consoleTextArea.textProperty().bind(threadTask.messageProperty());
    processPercentLabel.textProperty().bind(threadTask.titleProperty());

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.execute(threadTask);
    executorService.shutdown();
  }

  /**
   * Реализация метода должна определять способ проверки входных
   * параметров для каждой задачи.
   */
  private boolean checkInputParameters() {
    boolean b = true;
    File inputFolder = new File(inputFolderTextField.getText());
    File outputFolder = new File(outputFolderTextField.getText());
    if (!inputFolder.isDirectory()) {
      inputFolderTextField.setStyle(getErrorStyleTextField());
      b = false;
    }
    if (!outputFolder.isDirectory()) {
      outputFolderTextField.setStyle(getErrorStyleTextField());
      b = false;
    }
    return b;
  }

  private void defaultStyle(TextField textField) {
    if (textField.getStyle().contains(getErrorStyleTextField())) {
      textField.setStyle(getDefaultStyleTextField());
    }
  }
}
