package model.task.structure

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import TestUtils.outputFilePointsForStarigraphy
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.task.thread.ThreadTask
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.net.URLDecoder

internal class StratigraphyIntervalsToPointsTest {
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
            "outputFile" to outputFilePointsForStarigraphy,
            "ageIndexes" to " J1sn\$;;J1sn#; J1sn@; ;J1sn!", "unionLayers" to false,
            "addPoints" to false, "useAmendment" to true)
  }

  @AfterEach
  fun tearDown() { parameters = null }

  @Test
  fun `parse age indexes`() {
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    assertIterableEquals(task.ageIndexesAsSet,
            setOf("J1sn$", "J1sn#", "J1sn@", "J1sn!"))
  }

  @Test
  fun `add points false _ union layers false`() {
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(1404, table.size)
    assertEquals(4, table[0].size)
    assertEquals("232.8", table[0]["Z"])
    assertEquals("141.75", table[20]["Z"])
  }

  @Test
  fun `addPoints false _ unionLayers true`() {
    parameters?.set("unionLayers", true)
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(720, table.size)
  }

  @Test
  fun `add points true _ unionLayers true`() {
    parameters?.set("addPoints", true)
    parameters?.set("unionLayers", true)
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(9978, table.size)
    assertEquals(4, table[0].size)
    assertEquals("244.61", table[195]["Z"])
    assertEquals("244.59", table[196]["Z"])
    assertEquals("217.01", table[226]["Z"])
    assertEquals("216.99", table[227]["Z"])
    assertEquals("197.01", table[247]["Z"])
    assertEquals("196.99", table[248]["Z"])
    assertEquals("105.2", table[346]["Z"])
  }

  @Test
  fun `addPoints true _ unionLayers false`() {
    parameters?.set("addPoints", true)
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(10870, table.size)
  }

  @Test
  fun `several points empty`() {
    parameters?.set("unionLayers", true)
    parameters?.set("addPoints", true)
    val path = "$folder/Южно_накынский/Южно_накынский_Лиственничный_part1.xls"
    val validPath = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(validPath)
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
  }
}