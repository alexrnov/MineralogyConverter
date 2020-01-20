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
      namesOfTasks[3] -> RemoveOverlapIntervals(parameters)
      namesOfTasks[4] -> IntervalsOfSamplingToPoints(parameters)
      namesOfTasks[5] -> BoundsOfSamplingToPoints(parameters)
      namesOfTasks[6] -> ProbesIsihogyClient(parameters)
      namesOfTasks[7] -> TopAndBottomOfWells(parameters)
      namesOfTasks[8] -> RoofAndSoleStratigraphicLayer(parameters)
      namesOfTasks[9] -> StratigraphyIntervalsToPoints(parameters)
      namesOfTasks[10] -> RoofOfBaseLayersToPoints(parameters)
      namesOfTasks[11] -> ProbesWithMSDAndGenEmpty(parameters)
      else -> throw IllegalArgumentException("incorrect name of task")
    }
}