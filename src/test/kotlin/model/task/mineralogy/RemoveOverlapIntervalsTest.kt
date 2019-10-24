package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFileIntervalWellsAllMSD
import TestUtils.outputFileProbesIntervalsToPoints
import TestUtils.outputFileRemoveOverlapIntervals
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.File.separator as s
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
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

  @Disabled
  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = RemoveOverlapIntervals(parameters)
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
  }

  @Test
  fun perform2() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = RemoveOverlapIntervals(parameters)
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    task.perform("126952")
  }

  @Test
  fun `algorithm test of remove overlap`() {
    val probesWithMSD = listOf(
            mapOf("От" to "11.5", "До" to "12.5"), // проба внутри пустого интервала
            mapOf("От" to "13.5", "До" to "14.2"), // проба внутри пустоко интервала
            mapOf("От" to "15.1", "До" to "15.9"), // проба внутри пустого интервала
            mapOf("От" to "17.0", "До" to "18.0"), // проба соответствует пустому интервалу
            mapOf("От" to "17.5", "До" to "18.5"), // проба перекрывает пустой интервал сверху
            mapOf("От" to "19.5", "До" to "20.5"), // проба перекрывает пустой интервал снизу
            mapOf("От" to "20.7", "До" to "20.9"), // проба находится за пределами пустого интервала (сверху)
            mapOf("От" to "33.0", "До" to "35.9")) // проба находится за пределами пустого интервала (снизу)
    val emptyProbes = listOf(
            mapOf("От" to "10.5", "До" to "15.5"),
            mapOf("От" to "8.5", "До" to "16.0"),
            mapOf("От" to "17.0", "До" to "18.0"),
            mapOf("От" to "18.0", "До" to "19.0"),
            mapOf("От" to "19.0", "До" to "20.0"),
            mapOf("От" to "21.0", "До" to "22.0"),
            mapOf("От" to "30.0", "До" to "32.0"))

    fun f(pFromEmpty: Double, pToEmpty: Double): HashSet<String> {
      var fromEmpty = pFromEmpty
      var toEmpty = pToEmpty
      val list = HashSet<String>()

      for (probeWithMSD in probesWithMSD) {
        val fromMSD = probeWithMSD["От"]?.toDouble() ?: 0.0
        val toMSD = probeWithMSD["До"]?.toDouble() ?: 0.0
        when {
          (toMSD <= fromEmpty || fromMSD >= toEmpty) -> {
            //println("1. MSD[$fromMSD-$toMSD], Интервал с МСА за пределами пустой пробы")
          }
          (fromMSD <= fromEmpty && toMSD >= toEmpty) -> {
            //println("2. MSD[$fromMSD-$toMSD], Интервал с МСА полностью перекрывает пустой интервал")
            return list // если проба с МСА полностью перекрывает пустой интевал, вернуть пустой список
          }
          (fromMSD > fromEmpty && toMSD < toEmpty) -> {
            //println("3. MSD[$fromMSD-$toMSD], Интервал с МСА лежит внутри пустого интревала")
            val list2 = f(fromEmpty, fromMSD)
            val list3 = f(toMSD, toEmpty)
            list.addAll(list2)
            list.addAll(list3)
          }
          (toMSD > fromEmpty && fromMSD <= fromEmpty && toMSD < toEmpty) -> {
            //println("4. MSD[$fromMSD-$toMSD], Интервал с МСА перекрывает пустую пробу сверху")
            fromEmpty = toMSD
          }
          (fromMSD < toEmpty && toMSD >= toEmpty && fromMSD > fromEmpty) -> {
            //println("5. MSD[$fromMSD-$toMSD], Интервал с МСА перекрывает пустую пробу снизу")
            toEmpty = fromMSD
          }
          else -> println("Другой случай")
        }
      }
      if (list.size == 0) list.add("Empty[$fromEmpty-$toEmpty]")
      return list
    }

    val resultSet = HashSet<String>()
    for (emptyProbe in emptyProbes) {
      val fromEmpty = emptyProbe["От"]?.toDouble() ?: 0.0
      val toEmpty = emptyProbe["До"]?.toDouble() ?: 0.0
      resultSet.addAll(f(fromEmpty, toEmpty))
    }
    println("probesWithMSD: ")
    probesWithMSD.forEach { println(it) }
    println("-")
    println("emptyProbes: ")
    emptyProbes.forEach { println(it) }
    println("-")
    println("notOverlapEmptyProbes: ")
    resultSet.forEach { println(it) }
  }
}