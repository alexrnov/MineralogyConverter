package model.task

import application.StaticConstants
import model.task.mineralogy.*
import model.task.structure.*

object TypeOfGeoTask {

  /**
   * В зависимости от названия задачи, возвращает экземпляр класса,
   * который реализует эту самую задачу.
   */
  @Throws(IllegalArgumentException::class)
  fun getType(nameOfTask: String, parameters: Map<String, Any>): GeoTask =
    when (nameOfTask) { // паттерн АБСТРАКТНЫЙ МЕТОД
      StaticConstants.namesOfTasks[0] -> ProbesWithMSD(parameters)
      StaticConstants.namesOfTasks[1] -> ProbesWithoutMSD(parameters)
      StaticConstants.namesOfTasks[2] -> ProbesWithAllMSD(parameters)
      StaticConstants.namesOfTasks[3] -> BoundsOfSampling(parameters)
      StaticConstants.namesOfTasks[4] -> ProbesIsihogyClient(parameters)
      StaticConstants.namesOfTasks[5] -> TopAndBottomOfWells(parameters)
      StaticConstants.namesOfTasks[6] -> RoofAndSoleStratigraphicLayer(parameters)
      StaticConstants.namesOfTasks[7] -> StratigraphyIntervalsToPoints(parameters)
      else -> throw IllegalArgumentException("incorrect name of task")
    }
}