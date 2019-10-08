package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFolderProbesWithoutMSD
import TestUtils.outputFolderProbesWithoutMSD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLDecoder

internal class ProbesWithoutMSDTest {
  private val nameOfExcelFile =
          "input/excel files probes without MSD/Нижне-накынский-2.xls"
  private val mockTask = mockk<ThreadTask>()
  private val excelFile: File
  private val volume = "10 л"

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
    val resource = ClassLoader.getSystemResource(nameOfExcelFile)
    val configPath = URLDecoder.decode(resource.file, "UTF-8")
    excelFile = File(configPath)
  }

  @Test
  @DisplayName("not to use reference volume, amendment, createDotFile, all probes")
  fun perform1() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to false,
            "useAmendment" to false, "createDotFile" to false,
            "typeOfSelectionAge" to "Все пробы")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(322, task.getTopWells.size)
    assertEquals(5191, task.getIntervalWells.size)
    assertEquals(0, task.getDotWells.size)

    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2",
    "Линия" to "518", "Глубина ТН" to "84.0", "Зона" to "M20", "X" to "520647",
    "Y" to "7220527", "Участок" to "Ханнинский", "Точка" to "427",
    "Z" to "237.8", "ID" to "177668", "Тип ТН" to "Скв. поиск.", "IDW" to "300")
    assertEquals(expectedTopWell, task.getTopWells[300])

    val actualIntervalWell = task.getIntervalWells[2934]
    assertEquals(55, actualIntervalWell.size)
    assertEquals("202", actualIntervalWell["IDW"])
    assertEquals("63.9", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))

    assertEquals("3 л", actualIntervalWell["Объем"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
    assertEquals("73", actualIntervalWell["минеральная ассоциация шлиха/пирит"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/гроссуляр"])
    assertEquals("2", actualIntervalWell["минеральная ассоциация шлиха/дистен"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/барит"])
    assertEquals("50", actualIntervalWell["количество минералов/альмандин"])
    assertEquals("50", actualIntervalWell["количество минералов/ставролит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
  }

  @Test
  @DisplayName("use reference volume, amendment, createDotFile, all probes")
  fun perform2() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Все пробы")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(322, task.getTopWells.size)
    assertEquals(5191, task.getIntervalWells.size)
    assertEquals(5191, task.getDotWells.size)

    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2",
            "Линия" to "518", "Глубина ТН" to "84.0", "Зона" to "M20", "X" to "500647.0",
            "Y" to "7210527.0", "Участок" to "Ханнинский", "Точка" to "427",
            "Z" to "237.8", "ID" to "177668", "Тип ТН" to "Скв. поиск.", "IDW" to "300")
    assertEquals(expectedTopWell, task.getTopWells[300])
    val actualIntervalWell = task.getIntervalWells[2934]
    assertEquals(55, actualIntervalWell.size)
    assertEquals("202", actualIntervalWell["IDW"])
    assertEquals("63.9", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))

    assertEquals("3 л", actualIntervalWell["Объем"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
    assertEquals("73", actualIntervalWell["минеральная ассоциация шлиха/пирит"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/гроссуляр"])
    assertEquals("2", actualIntervalWell["минеральная ассоциация шлиха/дистен"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/барит"])

    assertEquals("166.67", actualIntervalWell["количество минералов/альмандин"])
    assertEquals("166.67", actualIntervalWell["количество минералов/ставролит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
  }

  @Test
  @DisplayName("all geological ages")
  fun perform3() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "По всем возрастам")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(322, task.getTopWells.size)
    assertEquals(4857, task.getIntervalWells.size)
    assertEquals(4857, task.getDotWells.size)
  }

  @Test
  @DisplayName("without geological ages")
  fun perform4() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Без возрастов")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(72, task.getTopWells.size)
    assertEquals(334, task.getIntervalWells.size)
    assertEquals(334, task.getDotWells.size)
  }

  @Test
  @DisplayName("select stratigraphic index")
  fun perform5() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Указать возраст:J1dh")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(253, task.getTopWells.size)
    assertEquals(2119, task.getIntervalWells.size)
    assertEquals(2119, task.getDotWells.size)
  }

  @Test
  @DisplayName("incorrect stratigraphic index")
  fun perform6() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithoutMSD,
            "outputFolder" to outputFolderProbesWithoutMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Указать возраст:A")
    val task = ProbesWithoutMSD(parameters)
    task.setThreadingTask(mockTask)
    val e = assertThrows(GeoTaskException::class.java) {
      task.perform(excelFile)
    }
    assertEquals("Проб с указанной выборкой по стратиграфии не найдено",
            e.message)
  }
}