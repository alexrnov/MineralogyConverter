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
  fun `input interval file with all probes`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 5,
            "taskName" to "подсветить точки по возрасту;;J1dh")
    var task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    var table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
    assertTrue(Files.exists(outputFile))
    assertEquals(54547862, outputFile.toFile().length())

    Files.deleteIfExists(outputFile)
    parameters["taskName"] = "подсветить точки по возрасту;;J1uk"
    task = IntervalsOfSamplingToPoints(parameters)
    task.setThreadingTask(mockTask)
    table = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
    assertTrue(Files.exists(outputFile))
    // размер файла будет такой же, даже несмотря на то, что
    // во входных параметрах указан другой стратиграфический индекс.
    // Т.е. изменение "1.0" на "0.0" и обратно не меняет размер файла
    assertEquals(54547862, outputFile.toFile().length())
  }

  @Test
  fun `input interval file with non-empty probes`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = IntervalsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsOnlyMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "подсветить точки по возрасту;;J1dh"))
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    task.writeData()
    assertTrue(Files.exists(outputFile))
    assertEquals(576351, outputFile.toFile().length())
    assertEquals(false, task.getDotWells[1].containsKey("находки"))
    assertEquals(false, task.getDotWells[2].containsKey("Стратиграфия"))
  }

  @Test
  fun `contains string in index age`() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    val task = IntervalsOfSamplingToPoints(mapOf(
            "inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
            "taskName" to "подсветить точки по возрасту;;J1tn"))
    task.setThreadingTask(mockTask)
    val table: Collection<Any?> = task.getTableFromFile()
    table.forEach { task.perform(it) }
    val dotWells = task.getDotWells
    assertEquals(242145, dotWells.size)
    assertEquals(12,
            dotWells.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())
    assertEquals(0,
            dotWells.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] != "1.0" }
                    .count())
    // у всех скважин со стратиграфией "J1tn" и ненулевым количеством
    // "Все_МСА" должен быть атрибут "находки" со значением "1.0"
    assertEquals(dotWells.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
            .count(),
            dotWells.filter { (it["Стратиграфия"] == "J1tn!") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())

    assertEquals(2613,
            dotWells.filter { (it["Стратиграфия"] == "J1dh") && it["Все_МСА"] != "0" }
                    .count())
    // все точки с другими стратиграфическимим индексами (например J1dh)
    // , даже если атрибут "Все_МСА" != 0, должны иметь значение
    // атрибута "находки" = 0.0
    assertEquals(0,
            dotWells.filter { (it["Стратиграфия"] == "J1dh") && it["Все_МСА"] != "0" }
                    .filter { it["находки"] == "1.0" }
                    .count())
  }

  @Test
  fun perform() {
    val outputFile = Paths.get(outputFileProbesIntervalsToPoints)
    Files.deleteIfExists(outputFile)
    var parameters = mutableMapOf("inputFile" to inputFileIntervalWellsAllMSD,
            "outputFile" to outputFileProbesIntervalsToPoints, "frequency" to 1,
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