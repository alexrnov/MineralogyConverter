package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import TestUtils.outputFolderIsihogyClient

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.constants.CommonConstants.nameOfAttributeDepth
import model.constants.CommonConstants.nameOfDotWellsFile
import model.constants.CommonConstants.nameOfIntervalWellsFile
import model.constants.CommonConstants.nameOfTopWellsFile
import model.constants.CommonConstants.noData
import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import model.constants.IsihogyClientConstants.nameOfAttributeZ
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import model.utils.ExcelUtils.listOfExcelFiles
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import java.io.File
import java.io.File.separator as s
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths

internal class ProbesIsihogyClientTest {

  private val mockTask = mockk<ThreadTask>()
  private val folder = "input/excel files from isihogy client"
  private val parameters = mapOf("inputFolder" to inputFolderIsihogyClient,
          "outputFolder" to outputFolderIsihogyClient,
          "findsOfCrystals" to "Все пробы", "useAmendment" to false,
          "createDotFile" to false, "typeOfSelectionAge" to "Все пробы")
  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @Disabled
  @DisplayName("perform all excel files")
  @Test
  fun perform1() {
    val files = listOfExcelFiles(inputFolderIsihogyClient)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    files.forEach {
      try {
        println(it.name)
        task.perform(it)
      } catch (e: GeoTaskException) {}
    }
  }

  @DisplayName("number elements of tables and decoding values")
  @Test
  fun perform2() {
    var path = "input/edit excel files from isihogy client/Россыпной_редакция.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    var table = task.getObservationsPointsTable
    val nameOfCode = "Код Состояния документирования"
    var otherWells = 0
    table.forEach {
      when (it[nameOfAttributeID]) {
        // расшифровка не найдена
        "134383" -> assertEquals(noData, it[nameOfCode])
        "134387" -> assertEquals("Не введено", it[nameOfCode])
        "134388" -> assertEquals("Выполнено первичное документирование",
                it[nameOfCode])
        "155280" -> assertEquals("Выполнено документирование по ГИС",
                it[nameOfCode])
        "155281" -> assertEquals("Выполнено итоговое документирование", it[nameOfCode])
        else -> otherWells++
      }
    }
    assertEquals(20, table.size)
    assertEquals(15, otherWells)

    table = task.getMineralogyTable
    assertEquals(92, table.size)
    assertEquals("J1dh", table[0]["L_Code возраста"])
    assertEquals("Алевролит", table[0]["L_Code породы"])
    assertEquals("J1uk", table[5]["L_Code возраста"])
    assertEquals("Конгломерат", table[5]["L_Code породы"])
  }

  @DisplayName("empty rows in sheet of points observations")
  @Test
  fun perform3() {
    var path = "$folder/Промышленный-3_08_02_18/GIS_1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(4, task.getObservationsPointsTable.size)
  }

  @DisplayName("interchange XY, amendment, attribute all minerals")
  @Test
  fun perform4() {
    var path = "$folder/Россыпной/Россыпной.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)

    var table = task.getObservationsPointsTable
    assertEquals("523297.92", table[0][nameOfAttributeX])
    assertEquals("7223543.26", table[0][nameOfAttributeY])
    assertEquals("523255.38", table[1][nameOfAttributeX])
    assertEquals("7223613.56", table[1][nameOfAttributeY])
    table = task.getMineralogyTable
    assertEquals("6.0", table[0]["Все МСА"])
    assertEquals("7.0", table[1]["Все МСА"])
    assertEquals("13.0", table[3]["Все МСА"])
    assertEquals("533.0", table[8]["Все МСА"])
  }

  @DisplayName("coincident collar of wells")
  @Test
  fun perform5() {
    var path = "$folder/Нижне_накынский/Нижне_накынский_Водораздельный_part2.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getObservationsPointsTable
    var well1: Map<String, String> = HashMap()
    var well2: Map<String, String> = HashMap()
    table.forEach {
      if (it[nameOfAttributeID] == "120009") well1 = it
      if (it[nameOfAttributeID] == "120014") well2 = it
    }
    assertNotEquals(well1[nameOfAttributeX], well2[nameOfAttributeX])
  }

  @DisplayName("fix when interval more than depth")
  @Test
  fun perform6() {
    var path = "input/edit excel files from isihogy client/Россыпной_редакция.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getObservationsPointsTable
    table.filter { it[nameOfAttributeID] == "155279" }
         .forEach { assertEquals("191.0", it[nameOfAttributeDepth]) }
  }

  @DisplayName("create dot table and create files, use amendment")
  @Test
  fun perform7() {
    val parameters = mapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFolder" to outputFolderIsihogyClient,
            "findsOfCrystals" to "Все пробы", "useAmendment" to true,
            "createDotFile" to true, "typeOfSelectionAge" to "Все пробы")
    val topWellsFile =
            Paths.get(outputFolderIsihogyClient + s + nameOfTopWellsFile)
    Files.deleteIfExists(topWellsFile)
    val intervalWellsFile =
            Paths.get(outputFolderIsihogyClient + s + nameOfIntervalWellsFile)
    Files.deleteIfExists(intervalWellsFile)
    val dotFile =
            Paths.get(outputFolderIsihogyClient + s + nameOfDotWellsFile)
    Files.deleteIfExists(dotFile)

    var path = "$folder/Россыпной/Россыпной.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)

    val topWellsTable = task.getObservationsPointsTable
    val intervalWellsTable = task.getMineralogyTable
    val dotWellsTable = task.getDotWells
    assertEquals(20, topWellsTable.size)
    assertEquals(92, intervalWellsTable.size)
    assertEquals(92, dotWellsTable.size)
    if (topWellsTable[0][nameOfAttributeID]
            == intervalWellsTable[0][nameOfAttributeID]
            && topWellsTable[0][nameOfAttributeID]
            == dotWellsTable[0][nameOfAttributeID]) {
      assertEquals("Одна и та же скважина", "Одна и та же скважина")
    } else {
      assertEquals("Разные скважины", "Одна и та же скважина")
    }
    assertEquals(27, topWellsTable[0].size)
    assertEquals(115, intervalWellsTable[0].size)
    // 27 + 115 = 142 - 2(ID_ТН и UIN - общие поля, поэтому они
    // объединяются и размер коллекции становится на два элемента меньше)
    assertEquals(140, dotWellsTable[0].size)

    assertEquals("134383", dotWellsTable[0][nameOfAttributeID])
    assertEquals("503297.92", dotWellsTable[0][nameOfAttributeX])
    assertEquals("7213543.26", dotWellsTable[0][nameOfAttributeY])
    assertEquals("170.77", dotWellsTable[0][nameOfAttributeZ])
    assertEquals("Выполнено итоговое документирование",
            dotWellsTable[0]["Код Состояния документирования"])
    assertEquals("81.0", dotWellsTable[0][nameOfAttributeFrom])
    assertEquals("81.7", dotWellsTable[0][nameOfAttributeTo])
    assertEquals("4", dotWellsTable[0]["pir05_krd"])

    assertTrue(Files.exists(topWellsFile))
    assertTrue(Files.exists(intervalWellsFile))
    assertTrue(Files.exists(dotFile))
  }

  @DisplayName("select geoAge")
  @Test
  fun perform8() {
    val parameters = mutableMapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFolder" to outputFolderIsihogyClient,
            "findsOfCrystals" to "Все пробы", "useAmendment" to true,
            "createDotFile" to true, "typeOfSelectionAge" to "Все пробы")

    var path = "$folder/Промышленный_4/Промышленный_4_участок1_part1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)

    var task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(100, task.getObservationsPointsTable.size)
    assertEquals(2088, task.getMineralogyTable.size)
    assertEquals(2088, task.getDotWells.size)

    parameters.put("typeOfSelectionAge", "По всем возрастам")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(100, task.getObservationsPointsTable.size)
    assertEquals(1969, task.getMineralogyTable.size)
    assertEquals(1969, task.getDotWells.size)

    parameters.put("typeOfSelectionAge", "Без возрастов")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(42, task.getObservationsPointsTable.size)
    assertEquals(119, task.getMineralogyTable.size)
    assertEquals(119, task.getDotWells.size)

    parameters.put("typeOfSelectionAge", "Указать возраст:J1dh")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(90, task.getObservationsPointsTable.size)
    assertEquals(1032, task.getMineralogyTable.size)
    assertEquals(1032, task.getDotWells.size)
  }

  @DisplayName("select by finds of crystals")
  @Test
  fun perform9() {
    val parameters = mutableMapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFolder" to outputFolderIsihogyClient,
            "findsOfCrystals" to "Все пробы", "useAmendment" to true,
            "createDotFile" to true, "typeOfSelectionAge" to "Все пробы")
    var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)

    var task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(77, task.getObservationsPointsTable.size)
    assertEquals(2323, task.getMineralogyTable.size)
    assertEquals(2323, task.getDotWells.size)

    parameters.put("findsOfCrystals", "Есть находки МСА")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(43, task.getObservationsPointsTable.size)
    assertEquals(152, task.getMineralogyTable.size)
    assertEquals(152, task.getDotWells.size)

    parameters.put("findsOfCrystals", "Пустые пробы")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(77, task.getObservationsPointsTable.size)
    assertEquals(2323 - 152, task.getMineralogyTable.size)
    assertEquals(2323 - 152, task.getDotWells.size)
  }

  @DisplayName("conformity between all probes and empty probes")
  @Test
  fun perform10() {
    val parameters = mutableMapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFolder" to outputFolderIsihogyClient,
            "findsOfCrystals" to "Все пробы", "useAmendment" to true,
            "createDotFile" to true, "typeOfSelectionAge" to "Все пробы")
    var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)

    var task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(50, task.getMineralogyTable // все пробы по ТН
            .filter { it[nameOfAttributeID] == "168011" }.count())

    parameters.put("findsOfCrystals", "Есть находки МСА")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(14, task.getMineralogyTable // непустые пробы по ТН
            .filter { it[nameOfAttributeID] == "168011" }.count())

    parameters.put("findsOfCrystals", "Пустые пробы")
    task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(36, task.getMineralogyTable // пустые пробы по ТН
            .filter { it[nameOfAttributeID] == "168011" }.count())

    // количество интервалов пустых проб и точек по пустым пробам
    // должно сходиться
    assertEquals(38, task.getMineralogyTable
            .filter { it[nameOfAttributeID] == "168015" }.count())
    assertEquals(38, task.getDotWells
            .filter { it[nameOfAttributeID] == "168015" }.count())
  }

  @Test
  fun `error - sheet of mineralogy data is empty`() {
    var path = "$folder/Накынский/Накынский_part4.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val excelFile = File(path)
    val task = ProbesIsihogyClient(parameters)
    task.setThreadingTask(mockTask)
    val e = assertThrows(GeoTaskException::class.java) {
      task.perform(excelFile)
    }
    assertEquals("Лист excel с данными минералогии - пуст", e.message)
  }

  @Test
  fun `error init - empty title of mineralogy`() {
    val parameters = mapOf("inputFolder" to "." + s + "src" + s + "test" + s +
            "resources" + s + "input" + s + "edit excel files from isihogy client",
            "outputFolder" to outputFolderIsihogyClient,
            "findsOfCrystals" to "Все пробы", "useAmendment" to false,
            "createDotFile" to false, "typeOfSelectionAge" to "Все пробы")
    val e = assertThrows(ExcelException::class.java) {
      val task = ProbesIsihogyClient(parameters)
      task.setThreadingTask(mockTask)
    }
    assertEquals("Нет названий полей в листе с данными минералогии", e.message)
  }
}