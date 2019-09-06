import javafx.embed.swing.JFXPanel
import java.io.File.separator as s
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

object TestUtils {
  val inputFolderProbesWithMSD = "." + s + "src" + s + "test" + s +
          "resources" + s + "input" + s + "excel files probes with MSD"

  val outputFolderProbesWithMSD = "." + s + "src" + s + "test" +
          s + "resources" + s + "output" + s + "probes with MSD"

  val inputFolderProbesWithoutMSD = "." + s + "src" + s + "test" + s +
          "resources" + s + "input" + s + "excel files probes without MSD"

  val outputFolderProbesWithoutMSD = "." + s + "src" + s + "test" +
          s + "resources" + s + "output" + s + "probes without MSD"

  val inputFolderIsihogyClient = "." + s + "src" + s + "test" + s +
          "resources" + s + "input" + s + "excel files from isihogy client"

  val outputFolderIsihogyClient = "." + s + "src" + s + "test" + s +
          "resources" + s + "output" + s + "isihogy client"

  val outputFolderAllProbes = "." + s + "src" + s + "test" +
          s + "resources" + s + "output" + s + "probes with all MSD"

  val inputFileIntervalWellsAllMSD = "." + s + "src" + s + "test" + s +
          "resources" + s + "input" + s + "intervalWellsAllMSD.txt"

  val outputFileABSFirstLastProbes = "." + s + "src" + s + "test" +
          s + "resources" + s + "output" + s + "absForFirstLastProbes.txt"

  val outputFilePointsForStarigraphy = "." + s + "src" + s + "test" + s +
          "resources" + s + "output" + s + "isihogy client" +
          s + "absPointsForStratigraphy.txt"

  // Инициализировать JavaFX, чтобы не возникала ошибка: Toolkit not initialized
  @Throws(InterruptedException::class)
  fun initToolkit() {
    val latch = CountDownLatch(1)
    SwingUtilities.invokeLater {
      JFXPanel() // Инициализация окружающей среды JavaFX
      latch.countDown()
    }
    latch.await()
  }
}