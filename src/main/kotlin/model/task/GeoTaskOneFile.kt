package model.task

import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import java.io.IOException

/**
 * Данный абстрактный класс определяет переменные и методы,
 * необходимые для реализации задачи по обработке
 * геолого-геофизических данных из одного входного файла.
 */
abstract class GeoTaskOneFile(val parameters: Map<String, Any>): GeoTask {

  /** Ссылка на класс задачи JavaFX */
  protected lateinit var task: ThreadTask

  /**
   * Установить ссылку на класс задачи JavaFX, для того чтобы можно было
   * передавать сведения о выполнении текущей задачи на консоль
   * приложения. Метод должен быть вызван раньше других методов.
   */
  fun setThreadingTask(task: ThreadTask) {
    this.task = task // паттерн НАБЛЮДАТЕЛЬ
  }

  /**
   * Метод должен считывать входной файл, и на его основе возвращать
   * коллекцию с данными. Каждый элемент этой коллекции должен
   * передаваться в метод perform. При этом, итерация коллекции
   * позволяет произвести вычисление прогресса выполнения задачи.
   */
  @Throws(SecurityException::class, IOException::class)
  abstract fun getTableFromFile(): Collection<Any?>

  /** Обработка текущего массива данных */
  @Throws(GeoTaskException::class)
  abstract fun perform(any: Any?)

  /**
   * Метод должен вызываться в конце выполнения задачи, и определять
   * способ сохранения полученных данных (запись в файл, вывод на консоль)
   */
  @Throws(SecurityException::class, IOException::class)
  abstract fun writeData()
}