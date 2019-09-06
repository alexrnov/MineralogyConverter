package model.task

import application.StaticConstants.namesOfTasks
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

import java.io.File.separator as s
import java.nio.file.Files
import java.nio.file.Paths
import TestUtils.initToolkit
import TestUtils.inputFolderProbesWithMSD
import TestUtils.outputFolderProbesWithMSD
import model.constants.CommonConstants.nameOfDotWellsFile
import model.constants.CommonConstants.nameOfIntervalWellsFile
import model.constants.CommonConstants.nameOfTopWellsFile
import model.task.thread.ManyFilesThreadTask

internal class ManyFilesThreadTaskTest {

  private var taskManyFiles: ManyFilesThreadTask

  init {
    initToolkit()
    val parameters: Map<String, Any> = mapOf("inputFolder" to inputFolderProbesWithMSD,
            "outputFolder" to outputFolderProbesWithMSD,
            "probeVolume" to "15 л", "useReferenceVolume" to true,
            "useAmendment" to true, "createDotFile" to true,
            "typeOfSelectionAge" to "Все пробы")
    taskManyFiles = ManyFilesThreadTask(namesOfTasks[0], parameters)
  }

  @Test
  fun call() {
    val topWellsFile = Paths.get(outputFolderProbesWithMSD + s + nameOfTopWellsFile)
    Files.deleteIfExists(topWellsFile)
    val intervalWellsFile = Paths.get(outputFolderProbesWithMSD + s +
            nameOfIntervalWellsFile)
    Files.deleteIfExists(intervalWellsFile)
    val dotWellsFile = Paths.get(outputFolderProbesWithMSD + s + nameOfDotWellsFile)
    Files.deleteIfExists(dotWellsFile)
    taskManyFiles.call()
    assertTrue(Files.exists(topWellsFile))
    assertTrue(Files.exists(intervalWellsFile))
    assertTrue(Files.exists(dotWellsFile))
  }
}