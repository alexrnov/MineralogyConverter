package controller;

import application.Mineralogy;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

import static application.Mineralogy.logger;
import static application.StaticConstants.getNamesOfTasks;
import static application.StaticConstants.getPathsOfTasks;
import static application.StaticConstants.getMaxHeightScrollPane;

public class MainLayout {
  /* компонент элемента интерфейса со списком задач для выбора */
  @FXML private ListView<String> listViewTasks = new ListView<>();

  /* anchorPane, в который загружается интерфейс выбранной задачи */
  @FXML private AnchorPane contentTaskAnchor;

  private Mineralogy mineralogy;
  private int currentIndexTask = 0; // индекс выбранной текущей задачи
  private String nameOfCurrentTask; // название выбранной текущей задачи

  @FXML
  private void initialize() {
    ObservableList<String> items = FXCollections.observableArrayList(
            getNamesOfTasks());

    listViewTasks.setItems(items);
    // loadLayoutByDefault(0);// загрузить по умолчанию интерфейс первой задачи
    // установить в качестве выбранной задачи первую задачу
    listViewTasks.getSelectionModel().select(0);
    nameOfCurrentTask = listViewTasks.getItems().get(0);
    // установить идентификатор для anchorPane, для задания в css
    // contentTaskAnchor.setId("content-pane");
    contentTaskAnchor.styleProperty().bind(
            new SimpleStringProperty("-fx-background-color: ")
                    .concat("#e8e8e8")
                    .concat(";")
    );
  }

  /**
   * загрузить во фрейм контента интерфейс для задачи с индексом indexOfTask
   * @param indexOfTask индекс задачи
   */
  public void loadLayoutByDefault(int indexOfTask) {
    loadLayout(indexOfTask);// загрузить по умолчанию интерфейс первой задачи
  }

  /**
   * Обработчик события выбора задачи из меню задач. Событие возникает
   * в результате выбора клавишей мыши либо клавишей клавиатуры
   */
  @FXML
  private void selectTask() {
    int selectIndexTask = listViewTasks.getSelectionModel().getSelectedIndex();
    nameOfCurrentTask = listViewTasks.getItems().get(selectIndexTask);
    // если задача в списке задач выбрана повторно, не загружать
    // лэйаут, поскольку он уже загружен
    if (currentIndexTask == selectIndexTask) {
      return;
    }
    currentIndexTask = selectIndexTask;
    loadLayout(currentIndexTask);
  }

  private void loadLayout(int indexTask) {
    String pathTask = getPathsOfTasks().get(indexTask);
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(Mineralogy.class.getResource(pathTask));

    try {
      AnchorPane ap = loader.load();
      ap.setStyle("-fx-background-color: #e8e8e8;");
      ScrollPane scrollPane = new ScrollPane();
      // максимальная высота, это значение необходимо указывать, для
      // того, чтобы изменялась полоса прокрутки, когда anchorPane с
      // элементами пользовательского интерфейса больше высоты scrollPane
      scrollPane.setMaxHeight(getMaxHeightScrollPane());
      scrollPane.setContent(ap);

      scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
      scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
      // добавить интерфейс для выбранной задачи
      //contentTaskAnchor.getChildren().setAll(ap);
      contentTaskAnchor.getChildren().setAll(scrollPane);
      TaskLayout controller = loader.getController();
      controller.setMineralogy(mineralogy);
      controller.setCloseDialog();
      controller.setMainLayout(this);
    } catch(IOException e) {
      logger.warning("Error load layout");
    }
  }

  public void setMineralogy(Mineralogy application) {
    this.mineralogy = application;
  }

  /**
   * Управляет блокировкой меню выбора задач
   * @param b Если <code>true</code> - заблокировать меню задач, в обратном
   * случае разблокировать меню задач
   */
  public void blockListView(boolean b) {
    listViewTasks.setMouseTransparent(b);
    listViewTasks.setFocusTraversable(!b);
  }

  /**
   * Делает неактивными пункты меню, когда запущена задача. Пункт для
   * задачи, которая выполняется в данный момент, остается неизменным.
   */
  public void changeStyleListView() {
    listViewTasks.setCellFactory(param -> new ListCell<String>(){
      @Override
      protected void updateItem(String t, boolean bln) {
        super.updateItem(t, bln);
        if (t != null) {
          setText(t);
          setStyle(!t.equals(nameOfCurrentTask) ? "-fx-text-fill: gray" :
                  "-fx-text-fill: white");
        }
      }
    });
  }

  /** Делает активными пункты меню, когда выполнение задачи остановлено. */
  public void defaultStyleListView() {
    listViewTasks.setCellFactory(param -> new ListCell<String>() {
      @Override
      protected void updateItem(String t, boolean bln) {
        super.updateItem(t, bln);
        if (t != null) {
          setText(t); //getStyleClass().add("/view/LagoonTheme.css");
        }
      }
    });
  }

  public String getNameOfCurrentTask() {
    return nameOfCurrentTask;
  }
}








