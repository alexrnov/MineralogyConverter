package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFileIntervalWellsAllMSD
import TestUtils.outputFileProbesIntervalsToPoints
import TestUtils.outputFileRemoveOverlapIntervals
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
  private var parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
          "outputFile" to outputFileRemoveOverlapIntervals)

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = RemoveOverlapIntervals(parameters)
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    //task.writeData()
  }

  @Test
  fun algorithm() {
    val probesWithMSD = listOf(
            mapOf("ID" to "177659", "От" to "71.6", "До" to "72.6"),
            mapOf("ID" to "177659", "От" to "72.6", "До" to "73.4"),
            mapOf("ID" to "177659", "От" to "73.4", "До" to "74.3"),
            mapOf("ID" to "177659", "От" to "74.3", "До" to "75.1"),
            mapOf("ID" to "177659", "От" to "75.1", "До" to "76"),
            mapOf("ID" to "177659", "От" to "76", "До" to "83"))
    val emptyProbes = listOf(
            mapOf("ID" to "177659", "От" to "54.2", "До" to "54.8"),
            mapOf("ID" to "177659", "От" to "67.7", "До" to "68.2"),
            mapOf("ID" to "177659", "От" to "71", "До" to "71.6"),
            mapOf("ID" to "177659", "От" to "71", "До" to "76"),
            mapOf("ID" to "177659", "От" to "76", "До" to "77"),
            mapOf("ID" to "177659", "От" to "77", "До" to "78.2"),
            mapOf("ID" to "177659", "От" to "78.2", "До" to "79.1"),
            mapOf("ID" to "177659", "От" to "81.1", "До" to "82.2"))
    fun f(fromEmpty: Double, toEmpty: Double) {
      for (probeWithMSD in probesWithMSD) {
        val fromMSD = probeWithMSD["От"]?.toDouble() ?: 0.0
        val toMSD = probeWithMSD["До"]?.toDouble() ?: 0.0
        when {
          (toMSD <= fromEmpty || fromMSD >= toEmpty) -> println("Интервал с МСА за пределами пустой пробы")
          (fromMSD <= fromEmpty && toMSD >= toEmpty) -> println("Интервал с МСА полностью перекрывает пустой интервал")
          (fromMSD > fromEmpty && toMSD < toEmpty) -> println("Интервал с МСА лежит внутри пустого интревала")
          (toMSD > fromEmpty && fromMSD < fromEmpty) -> println("Интервал с МСА перекрывает пустой интервал сверху")
          (fromMSD < toEmpty && toMSD >= toEmpty) -> println("Интервал с МСА перекрывает пустую пробу снизу")
        }
      }
    }

    for (emptyProbe in emptyProbes) {
      val fromEmpty = emptyProbe["От"]?.toDouble() ?: 0.0
      val toEmpty = emptyProbe["До"]?.toDouble() ?: 0.0
      f(fromEmpty, toEmpty)
    }

  }
}