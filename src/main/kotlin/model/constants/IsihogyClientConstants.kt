package model.constants

object IsihogyClientConstants {

  /** Название excel листа с точками наблюдений*/
  val observationsPointsSheetName = "Точки наблюдений"

  /** Название excel листа с литостратиграфией */
  val lithostratigraphicSheetName = "Стратиграфия Литология"

  /** Название excel листа с данными минералогии */
  val mineralogySheetName = "Результаты минералогии"

  /** Название excel листа с кодами состояния документирования */
  val stateDocumentationCodesSheetName = "Справ. СОСТОЯНИЕ ДОКУМЕНТИРОВАН"

  /** Название excel листа с кодами типов точек наблюдений */
  val typeOfWellCodesSheetName = "Справ. ТИП ТОЧКИ НАБЛЮДЕНИЯ"

  /** Название excel листа с кодами стратиграфии */
  val stratigraphicCodesSheetName = "Справ. Стратиграфия"

  /** Название excel листа с кодами литологии */
  val lithologyCodesSheetName = "Справ. Литология"

  /** Название excel листа с кодами типов проб */
  val typeOfProbesCodesSheetName = "Справ. ТИП ПРОБЫ"

  /**
   * Название кода, которое одновременно является названием атрибута
   * с закодированными данными. Значение используется при расшифровке
   * закодированных значений
   */
  val nameOfCodeAttribute = "nameOfCodeAttribute"

  /** Название атрибута: идентификатор для скважин */
  val nameOfAttributeID = "ID ТН"

  /** Название атрибута: координата X фактическое значение */
  val nameOfAttributeX = "X факт."

  /** Название атрибута: координата Y фактическое значение */
  val nameOfAttributeY = "Y факт."

  /** Название атрибута: координата Z фактическое значение */
  val nameOfAttributeZ = "Z"

  /** Название атрибута: координата X проектное значение */
  val nameOfAttributeProjectX = "X проект."

  /** Название атрибута: координата Y проектное значение */
  val nameOfAttributeProjectY = "Y проект."

  /** Название атрибута: глубина точки наблюдения */
  val nameOfAttributeDepth = "Глубина ТН"

  /** Название атрибута: От */
  val nameOfAttributeFrom = "От"

  /** Название атрибута: До */
  val nameOfAttributeTo = "До"

  /** Название для атрибута суммы всех находок МСА по текущей пробе */
  val nameOfAttributeAllMinerals = "Все МСА"

  /** Название для атрибута UIN */
  val nameOfAttributeUIN = "UIN"

  /** Название для атрибута возраста */
  val nameOfAttributeLCodeAge = "L_Code возраста"

  /** Название для атрибута литологии */
  val nameOfAttributeLCodeLithology = "L_Code породы"

  /**
   * Массив с числовыми значениями, которые встречаются в таблицах
   * ИСИХОГИ, когда нет реальных значений
   */
  val noValues = listOf("-999999.0", "-999.75", "-995.75")

  /** абсолютная отметка забоя скважины */
  val nameOfAttributeBottomWell = "D"

  /**
   * Название атрибута со значением количества слоев в одной
   * скважине, имеющих одинаковый стратиграфический индекс
   */
  val numberOfLayers = "Количество слоев"
}