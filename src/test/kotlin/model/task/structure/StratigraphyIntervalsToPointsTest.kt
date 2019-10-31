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
            "addPoints" to false, "frequency" to 1, "useAmendment" to true)
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
    assertEquals(10067, table.size)
    assertEquals(4, table[0].size)
    val list1 = listOf("248.2", "247.3", "246.4", "245.5", "244.61", "244.59",
                              "229.21", "229.19", "217.01", "216.99", "197.01", "196.99",
                              "186.41", "186.39", "179.41", "179.39", "162.51", "162.49")
    val list2 = listOf(193, 194, 195, 196, 197, 198, 214, 215, 228, 229, 249,
                              250, 261, 262, 269, 270, 287, 288)
    val map = list1.associate { Pair(it, list2[list1.indexOf(it)]) }
    map.forEach { abs, idx -> assertEquals(abs, table[idx]["Z"]) }
  }

  @Test
  fun `addPoints true _ unionLayers false`() {
    parameters?.set("addPoints", true)
    var task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    var table = task.getStratigraphicTable
    assertEquals(11168, table.size)

    parameters?.set("frequency", 2) // увеличить количество дополнительных точек
    task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    table = task.getStratigraphicTable
    assertEquals(20932, table.size)
  }

}