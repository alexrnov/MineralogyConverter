package model.task

import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import java.io.File

/**
 * Данный абстрактный класс определяет переменные и методы,
 * необходимые для реализации задачи по обработке
 * геолого-геофизических данных из нескольких входных файлов.
 */
abstract class GeoTaskManyFiles(val parameters: Map<String, Any>): GeoTask {

  /**
   * Список входных файлов, - эти файлы затем по очереди
   * передаются методу perform()
   */
  var inputFiles: MutableList<File> = ArrayList()

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
   * Метод преобразует данные из текущего входного файла (например Excel)
   * в любой выходной формат(например, *.txt для Micromine, *.shp
   * или просто вывод информации на консоль)
   */
  @Throws(GeoTaskException::class)
  abstract fun perform(file: File)
}