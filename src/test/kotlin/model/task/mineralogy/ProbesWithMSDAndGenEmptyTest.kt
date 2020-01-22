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

internal class ProbesWithMSDTestAndGenEmpty { // класс доступен везде внутри модуля
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
  @DisplayName("use reference volume, amendment, createDotFile, all probes")
  fun perform1() {
    val parameters = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD,
            "probeVolume" to volume, "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Все пробы")
    val task = ProbesWithMSDAndGenEmpty(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    }
}