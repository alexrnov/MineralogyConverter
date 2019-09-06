package model.task.structure

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import java.io.File.separator as s
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeLithology
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import model.constants.IsihogyClientConstants.nameOfAttributeZ
import model.constants.IsihogyClientConstants.numberOfLayers
import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import model.utils.ExcelUtils
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.net.URLDecoder

internal class RoofAndSoleStratigraphicLayerTest {

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
                    "outputFile" to "." + s + "src" + s + "test" + s + "resources" +
                            s + "output" + s + "isihogy client" + s +
                            "roofAndSoleStratigraphicLayer.txt",
                    "ageIndex" to "J1dh", "unionLayers" to true,
                    "unionPackets" to true, "useAmendment" to true)
  }

  @AfterEach
  fun tearDown() {
    parameters = null
  }

  @DisplayName("union layers, number of layers for each well, " +
          "with amendment and without")
  @Test
  fun perform1() {
    var task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    var table = task.getStratigraphicTable
    assertEquals(83, table.size)
    assertEquals(31, table[0].size)
    assertNull(table[0]["Описание"])
    assertNull(table[0][nameOfAttributeLCodeLithology])
    assertEquals("J1dh", table[0][nameOfAttributeLCodeAge])

    assertEquals("178051", table[0][nameOfAttributeID])
    assertEquals("88.3", table[0][nameOfAttributeFrom])
    assertEquals("133.0", table[0][nameOfAttributeTo])
    assertEquals("1", table[0][numberOfLayers])

    assertEquals("168053", table[2][nameOfAttributeID])
    assertEquals("83.2", table[2][nameOfAttributeFrom])
    assertEquals("115.3", table[2][nameOfAttributeTo])
    assertEquals("2", table[2][numberOfLayers])

    assertEquals("168053", table[3][nameOfAttributeID])
    assertEquals("119.1", table[3][nameOfAttributeFrom])
    assertEquals("135.0", table[3][nameOfAttributeTo])
    assertEquals("2", table[3][numberOfLayers])
    assertEquals("502413.0", table[0][nameOfAttributeX])
    assertEquals("7212205.0", table[0][nameOfAttributeY])

    parameters!!["useAmendment"] = false
    task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    table = task.getStratigraphicTable
    assertEquals("522413.0", table[0][nameOfAttributeX])
    assertEquals("7222205.0", table[0][nameOfAttributeY])
  }

  @DisplayName("union packets of age index (J1sn) and without union packets")
  @Test
  fun perform2() {
    parameters!!["ageIndex"] = "J1sn"
    var task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    var table = task.getStratigraphicTable
    assertEquals(76, table.size)

    assertEquals("168010", table[3][nameOfAttributeID])
    assertEquals("3.1", table[3][nameOfAttributeFrom])
    assertEquals("50.2", table[3][nameOfAttributeTo])
    assertEquals("1", table[3][numberOfLayers])

    parameters!!["unionPackets"] = false
    task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    table = task.getStratigraphicTable
    assertEquals(293, table.size)
  }

  @DisplayName("decode table observationsPoints, union and without union " +
          "layers, union tables")
  @Test
  fun perform3() {
    var task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    var table = task.getStratigraphicTable
    assertEquals("Скважина поисковая вертикальная", table[0]["Код типа ТН"])
    assertEquals("Скв. на завер. геофиз. аномалии", table[48]["Код типа ТН"])
    assertEquals("Скв. на завер. геофиз. аномалии", table[49]["Код типа ТН"])
    assertEquals(31, table[0].size)
    assertEquals("502413.0", table[0][nameOfAttributeX])
    assertEquals("7212205.0", table[0][nameOfAttributeY])
    assertEquals("2000", table[0][nameOfAttributeZ])

    parameters!!["unionLayers"] = false
    task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    table = task.getStratigraphicTable
    assertEquals(33, table[0].size)
    assertEquals(250, table.size)
    assertEquals("Глина карбонатная", table[0][nameOfAttributeLCodeLithology])
    assertEquals("Глина карбонатная", table[0]["Описание"])
    assertEquals("Алевролит глинистый", table[242][nameOfAttributeLCodeLithology])

  }

  @Disabled
  @DisplayName("perform all excel files")
  @Test
  fun perform() {
    val files = ExcelUtils.listOfExcelFiles(inputFolderIsihogyClient)
    val task = RoofAndSoleStratigraphicLayer(parameters!!)
    task.setThreadingTask(mockTask)
    files.forEach {
      try {
        println(it.name)
        task.perform(it)
      } catch (e: GeoTaskException) {}
    }
  }
}