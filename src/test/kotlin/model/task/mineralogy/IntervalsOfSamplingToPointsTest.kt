package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFileIntervalWellsAllMSD
import TestUtils.inputFileIntervalWellsOnlyMSD
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
  fun `highlight by find_input interval file with all probes`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    var parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "выделить точки по находкам")
    var task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    task.test = true
    var table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    println(table.size)

    parameters["frequency"] = 2
    task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    task.test = true
    table = task.getTableFromFile()
    table.forEach { task.perform(it) }
    println(table.size)
  }

  @Test
  fun `highlight by find and age_input interval file with all probes`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 5,
            "taskName" to "выделить точки по находкам и возрасту;;J1dh")
    var task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    var table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }

    assertTrue(Files.exists(outputFile))
    assertEquals(54547862, outputFile.toFile().length())
    assertIterableEquals(listOf("Стратиграфия", "east", "От", "north", "Z", "До", "находки", "ID",
            "Все_МСА", "generateZ"), task.getCurrentPoints[0].keys.toList())

    Files.deleteIfExists(outputFile)
    parameters["taskName"] = "выделить точки по находкам и возрасту;;J1uk"
    task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    table = task.getTableFromFile()
    table.forEach { task.perform(it) }

    assertTrue(Files.exists(outputFile))
    // размер файла будет такой же, даже несмотря на то, что
    // во входных параметрах указан другой стратиграфический индекс.
    // Т.е. изменение "1.0" на "0.0" и обратно не меняет размер файла
    assertEquals(54547862, outputFile.toFile().length())
    assertIterableEquals(listOf("Стратиграфия", "east", "От", "north", "Z", "До", "находки", "ID",
            "Все_МСА", "generateZ"), task.getCurrentPoints[0].keys.toList())
  }

  @Test
  fun `highlight by find and age_input interval file with non-empty probes`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = IntervalsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsOnlyMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "выделить точки по находкам и возрасту;;J1dh"))
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }

    assertTrue(Files.exists(outputFile))
    assertEquals(576351, outputFile.toFile().length())
    // атрибутов "Стратиграфия" и "находки" быть не должно
    assertIterableEquals(listOf("east", "От", "north", "Z", "До", "ID",
            "Все_МСА", "generateZ"), task.getCurrentPoints[0].keys.toList())
  }

  @Test
  fun `contains string in index age`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = IntervalsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "выделить точки по находкам и возрасту;;J1tn"))
    task.setThreadingTask(mockTask)
    task.test = true
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    val currentPoints = task.allPoints
    assertEquals(242145, currentPoints.size)
    assertEquals(242145, task.numberOfPoints)
    assertEquals(12,
            currentPoints.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())
    assertEquals(0,
            currentPoints.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] != "1.0" }
                    .count())
    // у всех скважин со стратиграфией "J1tn" и ненулевым количеством
    // "Все_МСА" должен быть атрибут "находки" со значением "1.0"
    assertEquals(currentPoints.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
            .count(),
            currentPoints.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())

    assertEquals(2613,
            currentPoints.filter { (it["Стратиграфия"] == "J1dh") && it["Все_МСА"] != "0" }
                    .count())
    // все точки с другими стратиграфическимим индексами (например J1dh)
    // , даже если атрибут "Все_МСА" != 0, должны иметь значение
    // атрибута "находки" = 0.0
    assertEquals(0,
            currentPoints.filter { (it["Стратиграфия"] == "J1dh") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())
  }

  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    var parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "вычислить общую сохранность")
    var task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    var table: Collection<Any?> = task.getTableFromFile()
    //table.forEach { task.perform(it) }

    parameters["inputFile"] = inputFileIntervalWellsOnlyMSD
    task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    table = task.getTableFromFile()
    table.forEach { task.perform(it) }
  }
}