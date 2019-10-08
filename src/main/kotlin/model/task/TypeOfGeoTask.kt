package model.task

import application.StaticConstants.namesOfTasks
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
      namesOfTasks[0] -> ProbesWithMSD(parameters)
      namesOfTasks[1] -> ProbesWithoutMSD(parameters)
      namesOfTasks[2] -> ProbesWithAllMSD(parameters)
      namesOfTasks[3] -> IntervalsOfSamplingToPoints(parameters)
      namesOfTasks[4] -> BoundsOfSamplingToPoints(parameters)
      namesOfTasks[5] -> ProbesIsihogyClient(parameters)
      namesOfTasks[6] -> TopAndBottomOfWells(parameters)
      namesOfTasks[7] -> RoofAndSoleStratigraphicLayer(parameters)
      namesOfTasks[8] -> StratigraphyIntervalsToPoints(parameters)
      namesOfTasks[9] -> RoofOfBaseLayersToPoints(parameters)
      else -> throw IllegalArgumentException("incorrect name of task")
    }
}