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
 * Контроллер для интерфейса второй задачи Минералогия ИСИХОГИ
 * в Micromine. В этой задаче используется набор excel-файлов,
 * которые загружены из ИСИХОГИ
 */
public class Task6Layout extends TaskLayout {

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
  @FXML private ComboBox<String> selectionAgeComboBox;
  @FXML private ComboBox<String> selectionFindsOfCrystalsComboBox;
  @FXML private CheckBox amendmentCheckBox;
  @FXML private CheckBox createDotFileCheckBox;

  private ButtonAnimation inputFolderAnimation;
  private ButtonAnimation outputFolderAnimation;
  private ButtonAnimation runTaskAnimation;
  private ButtonAnimation cancelTaskAnimation;

  @FXML
  private void initialize() {
    consoleTextArea.setEditable(false);
    consoleTextArea.setWrapText(true); // автоперенос строк в консоли

    createInputFolderButton(new ImageView(getOpenDialogPath()));
    createOutputFolderButton(new ImageView(getOpenDialogPath()));
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


    ObservableList<String> ages = FXCollections.observableArrayList("Все пробы",
            "По всем возрастам", "Без возрастов", "Указать возраст");
    selectionAgeComboBox.getItems().addAll(ages);
    selectionAgeComboBox.setValue("Все пробы");

    ObservableList<String> numberCrystals = FXCollections.observableArrayList(
            "Все пробы", "Есть находки МСА", "Пустые пробы");
    selectionFindsOfCrystalsComboBox.getItems().addAll(numberCrystals);
    selectionFindsOfCrystalsComboBox.setValue("Все пробы");

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

  private void createInputFolderButton(ImageView openDialogPathImage) {
    inputFolderButton.setGraphic(openDialogPathImage);
    inputFolderAnimation = new ButtonAnimation(inputFolderButton,
            "5 5 5 5", true);
    inputFolderButton.setOnAction(e -> {
      File f = mineralogy.directoryChooser();
      if (f != null) {
        inputFolderTextField.setText(f.getPath());
        defaultStyle(inputFolderTextField);
      }
    });
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> inputFolderAnimation.mouseEntered());
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> inputFolderAnimation.mouseExited());
  }

  private void createOutputFolderButton(ImageView openDialogPathImage) {
    outputFolderButton.setGraphic(openDialogPathImage);
    outputFolderAnimation = new ButtonAnimation(outputFolderButton,
            "5 5 5 5", true);
    outputFolderButton.setOnAction(e -> {
      File f = mineralogy.directoryChooser();
      if (f != null) {
        outputFolderTextField.setText(f.getPath());
        defaultStyle(outputFolderTextField);
      }
    });
    outputFolderButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> outputFolderAnimation.mouseEntered());
    outputFolderButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> outputFolderAnimation.mouseExited());
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

    if (threadTask != null && threadTask.isRunning()) {
      return;
    }

    if (!checkInputParameters()) {
      return;
    }

    //if (threadTask == null || threadTask.isDone())
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("inputFolder", inputFolderTextField.getText());
    parameters.put("outputFolder", outputFolderTextField.getText());
    parameters.put("findsOfCrystals", selectionFindsOfCrystalsComboBox.getValue());
    parameters.put("useAmendment", amendmentCheckBox.isSelected());
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
