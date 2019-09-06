package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFolderProbesWithMSD
import TestUtils.outputFolderProbesWithMSD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.exception.GeoTaskException
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import java.io.File
import java.net.URLDecoder

internal class ProbesWithMSDTest { // класс доступен везде внутри модуля
  private val nameOfExcelFile =
          "input/excel files probes with MSD/Нижне-накынский-2.xls"
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
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD,
            "probeVolume" to volume, "useReferenceVolume" to false,
            "useAmendment" to false, "createDotFile" to false,
            "typeOfSelectionAge" to "Все пробы")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(150, task.getTopWells.size)
    assertEquals(617, task.getIntervalWells.size)
    assertEquals(0, task.getDotWells.size)
    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2",
            "Линия" to "464", "Глубина ТН" to "77.5", "Зона" to "M20", "X" to "517753",
            "Y" to "7215163", "Участок" to "Озерный", "Z" to "225.90",
            "Точка" to "399", "ID" to "177591", "Тип ТН" to "Скв. поиск.",
             "IDW" to "148")
    assertEquals(expectedTopWell, task.getTopWells[148])

    val actualIntervalWell = task.getIntervalWells[616]

    assertEquals(267, actualIntervalWell.size)
    assertEquals("149", actualIntervalWell["IDW"])
    assertEquals("84.5", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))

    assertEquals("24", actualIntervalWell["Все МСА"])
    assertEquals("0", actualIntervalWell["Хромшпинелиды"])
    assertEquals("16", actualIntervalWell["пироп/по классам крупности/-0.5"])
    assertEquals("10", actualIntervalWell["пироп/класс износа I/-0.5"])
    assertEquals("5", actualIntervalWell["пироп/гипергенные/-0.5"])
    assertEquals("99", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
  }

  @Test
  @DisplayName("use reference volume, amendment, createDotFile, all probes")
  fun perform2() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Все пробы")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(150, task.getTopWells.size)
    assertEquals(617, task.getIntervalWells.size)
    assertEquals(617, task.getDotWells.size)
    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2", "Линия" to "464",
            "Глубина ТН" to "77.5", "Зона" to "M20", "X" to "497753.0",
            "Y" to "7205163.0", "Участок" to "Озерный", "Z" to "225.9",
            "Точка" to "399", "ID" to "177591", "Тип ТН" to "Скв. поиск.",
            "IDW" to "148")
    assertEquals(expectedTopWell, task.getTopWells[148])

    var actualIntervalWell = task.getIntervalWells[616]
    val actualDotWell = task.getDotWells[616]
    assertEquals(267, actualIntervalWell.size)
    assertEquals("149", actualIntervalWell["IDW"])
    assertEquals("84.5", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))
    assertEquals("497924.0", actualIntervalWell["X"])
    assertEquals("7205293.0", actualIntervalWell["Y"])
    assertEquals("497924.0", actualDotWell["X"])
    // количество кристаллов с учетом пересчета на эталонный объем пробы
    assertEquals("21.05", actualIntervalWell["Все МСА"])
    assertEquals("0", actualIntervalWell["Хромшпинелиды"])
    assertEquals("14.04", actualIntervalWell["пироп/по классам крупности/-0.5"])
    assertEquals("8.77", actualIntervalWell["пироп/класс износа I/-0.5"])
    assertEquals("4.39", actualIntervalWell["пироп/гипергенные/-0.5"])
    // минеральная ассоциация не пересчитывается - поскольку она выражена в процентах
    // ильменит - первый атрибут в разделе "минералогическая ассоциация шлиха"
    assertEquals("1", actualIntervalWell["минеральная ассоциация шлиха/ильменит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])

    actualIntervalWell = task.getIntervalWells[3]
    // раздел "количество минералов" пересчитывается
    // альмандин - первый атрибут в разделе "количество минералов"
    assertEquals("83.33", actualIntervalWell["количество минералов/альмандин"])
  }

  @Test
  @DisplayName("all geological ages")
  fun perform3() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD, "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "По всем возрастам")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(143, task.getTopWells.size)
    assertEquals(557, task.getIntervalWells.size)
    assertEquals(557, task.getDotWells.size)
  }

  @Test
  @DisplayName("without geological ages")
  fun perform4() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Без возрастов")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(29, task.getTopWells.size)
    assertEquals(60, task.getIntervalWells.size)
    assertEquals(60, task.getDotWells.size)
  }

  @Test
  @DisplayName("select stratigraphic index")
  fun perform5() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD, "probeVolume" to volume,
            "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Указать возраст:J1dh")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(53, task.getTopWells.size)
    assertEquals(127, task.getIntervalWells.size)
    assertEquals(127, task.getDotWells.size)
  }

  @Test
  @DisplayName("incorrect stratigraphic index")
  fun perform6() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD, "probeVolume" to volume,
            "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Указать возраст:A")
    val task = ProbesWithMSD(parameters)
    task.setThreadingTask(mockTask)
    val e = assertThrows(GeoTaskException::class.java) {
      task.perform(excelFile)
    }
    assertEquals("Проб с указанной выборкой по стратиграфии не найдено",
                  e.message)
  }
}