package model.task.mineralogy

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

internal class RemoveOverlapIntervalsTest {

  private val mockTask = mockk<ThreadTask>()

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }
  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    var parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints,
            "taskName" to "общая сохранность")
    var task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    var table: Collection<Any?> = task.getTableFromFile()
    //table.forEach { task.perform(it) }
    //task.writeData()

    parameters["inputFile"] = inputFileIntervalWellsOnlyMSD
    task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    table = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
  }
}