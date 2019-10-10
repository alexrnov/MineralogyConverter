package model.task.structure

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import TestUtils.outputFilePointsRoofOfBaseLayers
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.task.thread.ThreadTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class RoofOfBaseLayersToPointsTest {
  private val folder = "input/excel files from isihogy client"
  private var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
  private var validPath = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
  private val excelFile = File(validPath)
  private val mockTask = mockk<ThreadTask>()
  private var parameters: MutableMap<String, Any>? = null

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @BeforeEach
  fun setUp() {
    // если инициализировать коллекцию при объявлении,
    // от теста к тесту она будет модифицироваться, что приведет
    // к ошибке, поэтому она инициализируется здесь
    parameters = mutableMapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFile" to outputFilePointsRoofOfBaseLayers,
            "ageIndexes" to " O1ol;; мБD2-3vm; ;", "useAmendment" to true)
  }

  @AfterEach
  fun tearDown() { parameters = null }

  @Test
  fun `parse age indexes`() {
    val task = RoofOfBaseLayersToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    assertIterableEquals(task.ageIndexesAsList, listOf("O1ol", "мБD2-3vm"))
  }

  @Test
  fun perform1() {
    val outputFile = Paths.get(outputFilePointsRoofOfBaseLayers)
    Files.deleteIfExists(outputFile)
    val task = RoofOfBaseLayersToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(76, table.size)
    assertEquals("132.3", table[2][nameOfAttributeFrom])
    assertEquals("128.5", table[2][nameOfAttributeTo])
    assertEquals("O1ol", table[0][nameOfAttributeLCodeAge])
    assertEquals("мБD2-3vm", table[54][nameOfAttributeLCodeAge])
    assertTrue(Files.exists(outputFile))
    assertEquals(9934, outputFile.toFile().length())
  }

  @Test
  fun containsAgeIndex() {
    parameters!!["ageIndexes"] = "O"
    val task = RoofOfBaseLayersToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    println(task.getStratigraphicTable.size)
  }
}