package controller

import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Button
import javafx.util.Duration

/**
 * Экземпляр класса используется для анимации кнопки
 */
class ButtonAnimation(private var button: Button, private var padding:String,
                      private var activity: Boolean) {

  private lateinit var outBorderColor: SimpleObjectProperty<String>
  private lateinit var upRadialColor: SimpleObjectProperty<String>
  private lateinit var bottomColor: SimpleObjectProperty<String>

  private lateinit var externalBorderFlash: Timeline
  private lateinit var upRadialColorFlash: Timeline
  private lateinit var bottomColorFlash: Timeline

  init {
    if (activity) {
      activate()
    } else {
      deactivate()
    }
  }

  /** анимация, когда мышь находится над кнопкой */
  fun mouseEntered() {
    if (!activity) {
      return
    }

    externalBorderFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(outBorderColor,"rgba(91,133,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.10), KeyValue(outBorderColor,"rgba(94,136,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(outBorderColor,"rgba(97,139,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.20), KeyValue(outBorderColor,"rgba(100,142,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(outBorderColor,"rgba(103,145,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.30), KeyValue(outBorderColor,"rgba(106,148,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(outBorderColor,"rgba(109,151,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.40), KeyValue(outBorderColor,"rgba(112,154,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(outBorderColor,"rgba(115,157,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.50), KeyValue(outBorderColor,"rgba(118,160,212,1.0)", Interpolator.LINEAR)))
    externalBorderFlash.cycleCount = 1
    externalBorderFlash.play()

    upRadialColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(upRadialColor,"rgba(35,114,194,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.1),  KeyValue(upRadialColor,"rgba(34,114,196,0.98)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(upRadialColor,"rgba(33,115,199,0.96)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.2),  KeyValue(upRadialColor,"rgba(31,116,202,0.94)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(upRadialColor,"rgba(29,117,206,0.92)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.3),  KeyValue(upRadialColor,"rgba(27,118,209,0.90)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(upRadialColor,"rgba(25,119,212,0.88)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.4),  KeyValue(upRadialColor,"rgba(23,120,216,0.86)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(upRadialColor,"rgba(21,121,219,0.84)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.5),  KeyValue(upRadialColor,"rgba(19,122,223,0.82)", Interpolator.LINEAR)))
    upRadialColorFlash.cycleCount = 1
    upRadialColorFlash.play()

    bottomColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(bottomColor,"rgba(34,207,245,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.1), KeyValue(bottomColor,"rgba(41,207,245,0.9)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(bottomColor,"rgba(48,207,245,0.8)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.2), KeyValue(bottomColor,"rgba(55,207,245,0.7)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(bottomColor,"rgba(62,207,245,0.6)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.3), KeyValue(bottomColor,"rgba(69,207,245,0.5)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(bottomColor, "rgba(76,207,245,0.4)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.4), KeyValue(bottomColor,"rgba(83,207,245,0.3)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(bottomColor,"rgba(90,207,245,0.2)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.5), KeyValue(bottomColor,"rgba(100,207,245,0.1)", Interpolator.LINEAR)))
    bottomColorFlash.cycleCount = 1
    bottomColorFlash.play()
  }

  /** анимация, когда мышка уходит за пределы кнопки */
  fun mouseExited() {
    if (!activity) {
      return
    }
    externalBorderFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(outBorderColor,"rgba(115,157,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.10), KeyValue(outBorderColor,"rgba(112,154,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(outBorderColor,"rgba(109,151,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.20), KeyValue(outBorderColor,"rgba(106,148,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(outBorderColor,"rgba(103,145,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.30), KeyValue(outBorderColor,"rgba(100,142,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(outBorderColor,"rgba(97,139,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.40), KeyValue(outBorderColor,"rgba(94,136,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(outBorderColor,"rgba(91,133,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.50), KeyValue(outBorderColor,"rgba(88,130,212,1.0)", Interpolator.LINEAR)))
    externalBorderFlash.cycleCount = 1
    externalBorderFlash.play()

    upRadialColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(upRadialColor,"rgba(19,122,223,0.82)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.10), KeyValue(upRadialColor,"rgba(21,121,219,0.84)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(upRadialColor,"rgba(23,120,216,0.86)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.20), KeyValue(upRadialColor,"rgba(25,119,212,0.88)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(upRadialColor,"rgba(27,118,209,0.90)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.30), KeyValue(upRadialColor,"rgba(29,117,206,0.92)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(upRadialColor,"rgba(31,116,202,0.94)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.40), KeyValue(upRadialColor,"rgba(33,115,199,0.96)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(upRadialColor,"rgba(34,114,196,0.98)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.50), KeyValue(upRadialColor,"rgba(35,114,194,1.00)", Interpolator.LINEAR)))
    upRadialColorFlash.cycleCount = 1
    upRadialColorFlash.play()

    bottomColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(bottomColor,"rgba(100,207,245,0.1)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.10), KeyValue(bottomColor,"rgba(90,207,245,0.2)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(bottomColor,"rgba(83,207,245,0.3)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.20), KeyValue(bottomColor, "rgba(76,207,245,0.4)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(bottomColor,"rgba(69,207,245,0.5)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.30), KeyValue(bottomColor,"rgba(62,207,245,0.6)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(bottomColor,"rgba(55,207,245,0.7)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.40), KeyValue(bottomColor,"rgba(48,207,245,0.8)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(bottomColor,"rgba(41,207,245,0.9)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.50), KeyValue(bottomColor,"rgba(34,207,245,1.0)", Interpolator.LINEAR)))
    bottomColorFlash.cycleCount = 1
    bottomColorFlash.play()
  }

  /** анимация, когда кнопка была нажата */
  fun mouseOnClick() {
    if (!activity) {
      return
    }
    externalBorderFlash = Timeline(
      KeyFrame(Duration.seconds(0.05), KeyValue(outBorderColor,"rgba(115,157,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.10), KeyValue(outBorderColor,"rgba(109,151,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.15), KeyValue(outBorderColor,"rgba(103,145,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.20), KeyValue(outBorderColor,"rgba(97,139,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.25), KeyValue(outBorderColor,"rgba(88,130,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.30), KeyValue(outBorderColor,"rgba(97,139,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.35), KeyValue(outBorderColor,"rgba(103,145,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.40), KeyValue(outBorderColor,"rgba(109,151,212,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.45), KeyValue(outBorderColor,"rgba(115,157,212,1.0)", Interpolator.LINEAR)))
    externalBorderFlash.cycleCount = 1
    externalBorderFlash.play()

    upRadialColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.025), KeyValue(upRadialColor,"rgba(19,122,223,0.82)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.050), KeyValue(upRadialColor,"rgba(21,121,219,0.84)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.075), KeyValue(upRadialColor,"rgba(23,120,216,0.86)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.100), KeyValue(upRadialColor,"rgba(25,119,212,0.88)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.125), KeyValue(upRadialColor,"rgba(27,118,209,0.90)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.150), KeyValue(upRadialColor,"rgba(29,117,206,0.92)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.175), KeyValue(upRadialColor,"rgba(31,116,202,0.94)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.200), KeyValue(upRadialColor,"rgba(33,115,199,0.96)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.225), KeyValue(upRadialColor,"rgba(34,114,196,0.98)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.250), KeyValue(upRadialColor,"rgba(35,114,194,1.00)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.275),  KeyValue(upRadialColor,"rgba(34,114,196,0.98)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.300), KeyValue(upRadialColor,"rgba(33,115,199,0.96)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.325),  KeyValue(upRadialColor,"rgba(31,116,202,0.94)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.350), KeyValue(upRadialColor,"rgba(29,117,206,0.92)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.375),  KeyValue(upRadialColor,"rgba(27,118,209,0.90)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.400), KeyValue(upRadialColor,"rgba(25,119,212,0.88)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.425),  KeyValue(upRadialColor,"rgba(23,120,216,0.86)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.450), KeyValue(upRadialColor,"rgba(21,121,219,0.84)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.475),  KeyValue(upRadialColor,"rgba(19,122,223,0.82)", Interpolator.LINEAR)))
    upRadialColorFlash.cycleCount = 1
    upRadialColorFlash.play()

    bottomColorFlash = Timeline(
      KeyFrame(Duration.seconds(0.025), KeyValue(bottomColor,"rgba(100,207,245,0.1)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.050), KeyValue(bottomColor,"rgba(90,207,245,0.2)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.075), KeyValue(bottomColor,"rgba(83,207,245,0.3)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.100), KeyValue(bottomColor, "rgba(76,207,245,0.4)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.125), KeyValue(bottomColor,"rgba(69,207,245,0.5)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.150), KeyValue(bottomColor,"rgba(62,207,245,0.6)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.175), KeyValue(bottomColor,"rgba(55,207,245,0.7)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.200), KeyValue(bottomColor,"rgba(48,207,245,0.8)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.225), KeyValue(bottomColor,"rgba(41,207,245,0.9)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.250), KeyValue(bottomColor,"rgba(34,207,245,1.0)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.275), KeyValue(bottomColor,"rgba(41,207,245,0.9)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.300), KeyValue(bottomColor,"rgba(48,207,245,0.8)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.325), KeyValue(bottomColor,"rgba(55,207,245,0.7)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.350), KeyValue(bottomColor,"rgba(62,207,245,0.6)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.375), KeyValue(bottomColor,"rgba(69,207,245,0.5)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.400), KeyValue(bottomColor, "rgba(76,207,245,0.4)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.425), KeyValue(bottomColor,"rgba(83,207,245,0.3)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.450), KeyValue(bottomColor,"rgba(90,207,245,0.2)", Interpolator.LINEAR)),
      KeyFrame(Duration.seconds(0.475), KeyValue(bottomColor,"rgba(100,207,245,0.1)", Interpolator.LINEAR)))
    bottomColorFlash.cycleCount = 1
    bottomColorFlash.play()
  }

  fun activation(b: Boolean) {
    this.activity = b
    if (activity) {
      activate()
    } else {
      deactivate()
    }
  }

  private fun activate() {
    outBorderColor = SimpleObjectProperty("rgba(88,130,212,1.0)")
    upRadialColor = SimpleObjectProperty("rgba(36,113,190,1.0)")
    bottomColor = SimpleObjectProperty("rgba(23,207,245,1.0)")
    button.styleProperty().bind(SimpleStringProperty(
      " -fx-background-color: ")
      .concat(outBorderColor)
      .concat(", linear-gradient(#e6fbff 0%, #a6f0ff 20%, " +
              "#18d7ff 100%), linear-gradient(#d0f7ff, ")
      .concat(bottomColor)
      .concat("), radial-gradient(center 50% 0%, radius 100%, ")
      .concat(upRadialColor)
      .concat(", rgba(255,255,255,0)); " +
              "-fx-background-radius: 5,4,3,5; " +
              "-fx-background-insets: 0,1,2,0; -fx-text-fill: white; " +
              "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) ," +
              " 5, 0.0 , 0 , 1 ); -fx-font-family: \"Arial\"; " +
              "-fx-text-fill: white; -fx-font-size: 12px; " +
              "-fx-font-weight: bold; -fx-padding: $padding;"))
  }

  private fun deactivate() {
    outBorderColor = SimpleObjectProperty("#9f9f9f")
    upRadialColor = SimpleObjectProperty("#797979")
    bottomColor = SimpleObjectProperty("#9f9f9f")

    button.styleProperty().bind(SimpleStringProperty(
      " -fx-background-color: ")
      .concat(outBorderColor)
      .concat(", linear-gradient(#f0f0f0 0%, #dddddd 20%, " +
              "#bfbfbf 100%), linear-gradient(white, ")
      .concat(bottomColor)
      .concat("), radial-gradient(center 50% 0%, radius 100%, ")
      .concat(upRadialColor)
      .concat(", rgba(255,255,255,0)); " +
              "-fx-background-radius: 5,4,3,5; " +
              "-fx-background-insets: 0,1,2,0; -fx-text-fill: white; " +
              "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) ," +
              " 5, 0.0 , 0 , 1 ); -fx-font-family: \"Arial\"; " +
              "-fx-text-fill: white; -fx-font-size: 12px; " +
              "-fx-font-weight: bold; -fx-padding: $padding;"))
  }
}