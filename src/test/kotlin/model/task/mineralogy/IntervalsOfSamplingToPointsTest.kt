package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFileIntervalWellsAllMSD
import TestUtils.outputFileProbesIntervalsToPoints
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

internal class IntervalsOfSamplingToPointsTest {
  private val mockTask = mockk<ThreadTask>()

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = IntervalsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints))
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
  }
}