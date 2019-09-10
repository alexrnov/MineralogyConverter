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
    // он теста к тесту она будет модифицироваться, что приведет
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
  fun `add points false`() {
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
    assertEquals(1404, table.size)
    assertEquals(33, table[0].size)
  }

  @Test
  fun `add points true`() {
    parameters?.set("addPoints", true)
    parameters?.set("unionLayers", true)
    val task = StratigraphyIntervalsToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    //val table = task.getStratigraphicTable

    //assertEquals(1404, table.size)
    //assertEquals(33, table[0].size)
  }
}