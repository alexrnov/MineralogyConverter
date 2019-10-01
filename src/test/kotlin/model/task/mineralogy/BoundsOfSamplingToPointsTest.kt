package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFileIntervalWellsAllMSD
import TestUtils.outputFileABSFirstLastProbes
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

internal class BoundsOfSamplingToPointsTest {

  private val mockTask = mockk<ThreadTask>()

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @Test
  fun `compute task and write file`() {
    val outputFile = Paths.get(outputFileABSFirstLastProbes)
    Files.deleteIfExists(outputFile)
    val task = BoundsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileABSFirstLastProbes))
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
    assertTrue(Files.exists(outputFile))
    assertEquals(190056, outputFile.toFile().length())
  }

  @Test
  fun `invalid path to input file`() {
    val e = assertThrows(IllegalArgumentException::class.java) {
        BoundsOfSamplingToPoints(mapOf("inputFile" to "1D:\\incorrect.txt",
                "outputFile" to "1D:\\incorrect.txt"))
    }
    assertEquals("invalid path input or output file", e.message)
  }
}