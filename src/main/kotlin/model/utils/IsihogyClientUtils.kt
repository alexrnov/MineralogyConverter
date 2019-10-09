package model.utils

import application.Mineralogy.logger
import model.constants.CommonConstants.nameOfAttributeDepth
import model.constants.CommonConstants.noData
import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeProjectX
import model.constants.IsihogyClientConstants.nameOfAttributeProjectY
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.constants.IsihogyClientConstants.nameOfCodeAttribute
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import model.constants.IsihogyClientConstants.nameOfAttributeZ
import model.constants.IsihogyClientConstants.noValues
import model.constants.IsihogyClientConstants.nameOfAttributeAllMinerals
import model.constants.IsihogyClientConstants.nameOfAttributeBottomWell
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.numberOfLayers
import model.exception.DataException
import java.util.stream.Collectors
import java.lang.Double.valueOf as toDouble

object IsihogyClientUtils {

  /** Поменять местами координаты X и Y, поскольку в ИСИХОГИ они перепутаны */
  fun interchangeXY(wells: List<MutableMap<String, String>>) {
    wells.forEach {
      val x = it[nameOfAttributeX].toString()
      it.replace(nameOfAttributeX, it[nameOfAttributeY].toString())
      it.replace(nameOfAttributeY, x)
    }
  }

  /**
   * В таблице [table] декодировать значения для поля с кодами. Коды
   * находятся в ассоциативном массиве [codes]. В этом же массиве хранится
   * название поля с кодами (ключ IsihogyClientConstants.nameOfCodeAttribute)
   */
  @Throws(DataException::class)
  fun decodingField(table: List<MutableMap<String, String>>,
                    codes: MutableMap<String, String>) {
    val message = "Ошибка при раскодировании таблицы"
    if (!codes.containsKey(nameOfCodeAttribute)) {
      logger.info("Error when decoding the table: code not found")
      throw DataException(message)
    }
    if (table[0].containsKey(nameOfCodeAttribute)) {
      logger.info("Error when decoding the table: field for decode not found")
      throw DataException(message)
    }

    val codeAttribute: String = codes[nameOfCodeAttribute].toString()
    codes.remove(nameOfCodeAttribute)

    for (row in table) {
      val codeOfRow = row[codeAttribute].toString()
      run find@{ // break для обыного цикла for
        codes.forEach { // перебор кодов
          if (codeOfRow == it.key) {
            row[codeAttribute] = it.value // расшифровка
            return@find
          }
        }
        // выполняется после перебора кодов, если совпадений не найдено
        row[codeAttribute] = noData // если код для расшифровки не найден
      }
    }
  }

  /**
   * Поиск ошибок отсутствия пространственных данных для координат XYZ,
   * а также для глубины скважины, которые необходимы для картирования.
   * Если данные отсутствуют, им присваиваются значения по умолчанию,
   * которые затем необходимо исправить в среде Micromine/ArcGIS.
   * Возвращает список который содержит перечень информационных сообщений
   * для отображения на консоли.
   */
  fun checkOnMissDataXYZDAndFix(probes: List<MutableMap<String, String>>):
          MutableList<String> {
    val zero = "0"
    val z = "2000"
    val depth = "5000"
    val mistakes: MutableList<String> = ArrayList()
    probes.forEach {
      if (it[nameOfAttributeX] == noData || it[nameOfAttributeX] in noValues) {
        mistakes.add("Нет информации по координате X. " +
                "Идентификатор скважины: ${it[nameOfAttributeID]}. ")
        if (it[nameOfAttributeProjectX] == noData
                || it[nameOfAttributeProjectX] in noValues) {
          it[nameOfAttributeX] = zero
          mistakes.add("Координате X присвоено значение: $zero м")
        } else { // если есть информация по проектной координате
          it[nameOfAttributeX] = it[nameOfAttributeProjectX].toString()
          mistakes.add("Координате X присвоено проектное значение: " +
                  "${it[nameOfAttributeProjectX]} м")
        }
      }
      if (it[nameOfAttributeY] == noData || it[nameOfAttributeY] in noValues) {
        mistakes.add("Нет информации по координате Y. " +
                "Идентификатор скважины: ${it[nameOfAttributeID]}. ")
        if (it[nameOfAttributeProjectY] == noData
                || it[nameOfAttributeProjectY] in noValues) {
          it[nameOfAttributeY] = zero
          mistakes.add("Координате Y присвоено значение: $zero м")
        } else { // если есть информация по проектной координате
          it[nameOfAttributeY] = it[nameOfAttributeProjectY].toString()
          mistakes.add("Координате Y присвоено проектное значение: " +
                  "${it[nameOfAttributeProjectY]} м")
        }
      }
      if (it[nameOfAttributeZ] == noData || it[nameOfAttributeZ] in noValues) {
        it[nameOfAttributeZ] = z
        mistakes.add("Нет информации по координате Z. Идентификатор скважины: " +
          "${it[nameOfAttributeID]}. Координате Z присвоено значение: $z м")
      }
      if (it[nameOfAttributeDepth] == noData
              || it[nameOfAttributeDepth] in noValues) {
        it[nameOfAttributeDepth] = depth
        mistakes.add("Нет информации по глубине скважины. " +
                "Идентификатор скважины: ${it[nameOfAttributeID]}. " +
                "Атрибуту глубины скважины присвоено значение: $depth м")
      }
    }
    return mistakes
  }

  /**
   * Поиск ошибок отсутствия пространственных данных для интервалов,
   * которые необходимы для картирования. Если данные отсутствуют, им
   * присваиваются значения по умолчанию, которые затем необходимо
   * исправить в среде Micromine/arcGis. Возвращает список который
   * содержит перечень информационных сообщений для отображения на консоли.
   */
  fun checkOnMissDataFromTo(probes: List<MutableMap<String, String>>):
          MutableSet<String> {
    val from = "0"
    val to = "1"
    val mistakes: MutableSet<String> = HashSet()
    probes.forEach {
      if (it[nameOfAttributeFrom] == noData
              || it[nameOfAttributeFrom] in noValues
              || it[nameOfAttributeTo] == noData
              || it[nameOfAttributeTo] in noValues) {
        it[nameOfAttributeFrom] = from
        it[nameOfAttributeTo] = to
        mistakes.add("Не хватает информации по значениям интервала от-до. " +
                "Идентификатор скважины: ${it[nameOfAttributeID]}. " +
                "Атрибуту ${"От"} присвоено значение: $from м. " +
                "Атрибуту ${"До"} присвоено значение: $to м.")
      }
    }
    return mistakes
  }

  /** Удалить дробную часть атрибутов, для которых эта часть ненужна */
  fun deleteDecimalPart(attribute: String, table: List<MutableMap<String, String>>) {
    table.forEach { it[attribute] = it[attribute]!!.split(".")[0] }
  }

  /**
   * В таблице точек наблюдений оставить только те скважины, по которым
   * есть информация о находках МСА
   */
  fun getWellsWithProbes(observationsPointsTable: List<Map<String, String>>,
                         mineralogyTable: List<Map<String, String>>):
    List<MutableMap<String, String>> {

    val wellsWithProbes = ArrayList<MutableMap<String, String>>()
    observationsPointsTable.forEach find@{ well -> // break для цикла forEach
      mineralogyTable.forEach { probe ->
        if (well[nameOfAttributeID] == probe[nameOfAttributeID]) {
          wellsWithProbes.add(HashMap(well))
          return@find
        }
      }
    }
    return wellsWithProbes
  }

  /* поправка ИСИХОГИ */
  fun makeAmendment(wells: List<MutableMap<String, String>>) {
    wells.forEach { well ->
      var x = toDouble(well[nameOfAttributeX])
      var y = toDouble(well[nameOfAttributeY])
      x -= 20_000
      y -= 10_000
      /* округлить координаты до двух знаков */
      x = Math.round(x * 100.0) / 100.0
      y = Math.round(y * 100.0) / 100.0
      well.replace(nameOfAttributeX, x.toString())
      well.replace(nameOfAttributeY, y.toString())
    }
  }

  /**
   * В таблице с минералогией, все поля с находками МСА, имеющие значение
   * "Нет данных"(CommonConstants.noData), заменяются нулями
   */
  fun replaceNoDataToZero(probes: List<MutableMap<String, String>>,
                          attributesOfMineralogy: List<String>) {
    probes.forEach { probe ->
      attributesOfMineralogy.forEach { attribute ->
        if (probe[attribute] == noData) probe[attribute] = "0"
      }
    }
  }

  /**
   * Устранить проблему совпадающих устьев, когда у разных скважин совпадают
   * координаты X,Y,Z. В этом случае, для координаты X устанавливается
   * минимальное приращение на 0.01 м
   */
  fun fixCoincidentCollarOfWell(topWells: List<MutableMap<String, String>>) {
    // коллекция с которой сопоставляется каждый элемент на предмет совпадения
    // по XYZ. Размер коллекции уменьшается в процессе перебора.
    val wellsForSearch = ArrayList(topWells)
    // коллекция, куда сохраняются совпавшие по XYZ скважины, включая текущую
    // проверяемую скважину
    val coincidentWells: MutableList<MutableMap<String, String>> = ArrayList()
    for (e1 in topWells) { // перебор всех скважин
      var wasCoincidence = false // определяет, были ли уже совпадения
      // текущая скважина e1 сопоставляется с остальными на предмет совпадения
      wellsForSearch.forEach { e2 ->
        if (e1[nameOfAttributeID] != e2[nameOfAttributeID]
                && e1[nameOfAttributeX] == e2[nameOfAttributeX]
                && e1[nameOfAttributeY] == e2[nameOfAttributeY]
                && e1[nameOfAttributeZ] == e2[nameOfAttributeZ]) {
          // если совпадение встречено впервые, добавить сразу текущий элемент
          if (!wasCoincidence) {
            coincidentWells.add(e1)
            wasCoincidence = true
          }
          coincidentWells.add(e2) // добавить элемент, который совпал
        }
      }
      // если были найдены совпадения по XYZ для текущей скважины
      if (coincidentWells.size != 0) {
        wellsForSearch.removeAll(coincidentWells)
        var increment = 0.0
        // перебрать все скважины, совпавшие по XYZ
        for (well in coincidentWells) {
          var x: Double = well[nameOfAttributeX]!!.toDouble() + increment
          x = Math.round(x * 100.0) / 100.0 // округлить до двух знаков
          // необязательно делать изменение отображений в коллекции topWells,
          // достаточно лишь изменить отображения в текущей коллекции
          // совпавших по XYZ скважин, поскольку в нее добавлены отображения,
          // ссылающиеся на коллекцию topWells
          well[nameOfAttributeX] = x.toString()
          // координата следующей скважины будет смещена на 0.01 м
          increment += 0.01
        }
        coincidentWells.clear()
      }
      wellsForSearch.remove(e1) // исключить текущий элемент из поиска
    }
  }

  /**
   * Каждой пробе в таблице минералогии добавляется атрибут "Все МСА"
   * (IsihogyConstants.nameOfAttributeAllMinerals). В этот атрибут
   * записывается суммарное количество находок кристаллов МСА.
   */
  fun addAttributeOfAllMinerals(probes: List<MutableMap<String, String>>,
                                attributesOfMineralogy: List<String>) {
    var allMinerals: Double
    probes.forEach { probe ->
      allMinerals = 0.0
      attributesOfMineralogy.forEach { attribute ->
        allMinerals += probe[attribute]!!.toDouble()
      }
      probe[nameOfAttributeAllMinerals] = allMinerals.toString()
    }
  }

  /**
   * Когда отметка "До" интервала пробы превышает глубину скважины,
   * глубине скважины присваивается отметка "До" этой пробы. При этом,
   * если несколько проб имеют интервалы превышающие глубину скважины,
   * из них выбирается значение "До", имеющее максимальную глубину "До"
   */
  fun fixWhenIntervalMoreThanDepth(wells: List<MutableMap<String, String>>,
                                   probes: List<MutableMap<String, String>>) {
    wells.forEach { well ->
      var maxTo = 0.0 // максимальное значение для отметки "До"
      probes.forEach { probe ->
        if (well[nameOfAttributeID] == probe[nameOfAttributeID]) {
          val depth = well[nameOfAttributeDepth]!!.toDouble()
          val to = probe[nameOfAttributeTo]!!.toDouble()
          if (to > depth) { // значение "До" пробы больше глубины скважины
            if (to > maxTo) maxTo = to // наибольшее значение "До" среди проб
          }
        }
      }
      // если значение "До" пробы оказалось больше глубины скважины
      if (maxTo > 0.0) well[nameOfAttributeDepth] = maxTo.toString()
    }
  }

  /**
   * Возвращает таблицу, сформированную на основе объединения таблиц
   * точек наблюдений и минералогии.
   */
  fun unionTablesForDotWells(wells: List<Map<String, String>>,
                             probes: List<Map<String, String>>):
      List<MutableMap<String, String>> {
    val probesWithDataOfWells: MutableList<MutableMap<String, String>> = ArrayList()
    probes.forEach find@{ probe -> // break для forEach, перейти к следующей пробе
      wells.forEach { well ->
        if (probe[nameOfAttributeID] == well[nameOfAttributeID]) {
          val dotWell: MutableMap<String, String> = HashMap()
          dotWell.putAll(probe) // добавить атрибуты пробы
          dotWell.putAll(well) // добавить атрибуты точки наблюдения
          probesWithDataOfWells.add(dotWell)
          return@find
        }
      }
    }
    return probesWithDataOfWells
  }

  /**
   * В зависимости от типа выборки по стратиграфии, произвести
   * фильтрацию таблицы данных
   */
  fun filterByGeologicalAge(probes: List<MutableMap<String, String>>,
                            typeOfSelectionAge: String):
          List<MutableMap<String, String>> = when (typeOfSelectionAge) {
    "Все пробы" -> probes
    "По всем возрастам" -> probes.stream()
            .filter { it[nameOfAttributeLCodeAge] != "Нет данных"}
            .collect(Collectors.toList())
    "Без возрастов" -> probes.stream()
            .filter { it[nameOfAttributeLCodeAge] == "Нет данных" }
            .collect(Collectors.toList())
    else -> probes.stream()
            .filter { it[nameOfAttributeLCodeAge] == typeOfSelectionAge}
            .collect(Collectors.toList())
  }

  /**
   * В зависимости от типа выборки по наличию находок МСА,
   * произвести фильтрацию таблицы данных
   */
  fun filterByFindsOfCrystals(probes: List<MutableMap<String, String>>,
                              typeOfSelectionFinds: String):
          List<MutableMap<String, String>> = when (typeOfSelectionFinds) {
    "Все пробы" -> probes
    "Есть находки МСА" -> probes.stream()
            .filter { it[nameOfAttributeAllMinerals]!!.toDouble() > 0.0 }
            .collect(Collectors.toList())
    else -> probes.stream() // Пустые пробы
            .filter { it[nameOfAttributeAllMinerals]!!.toDouble() == 0.0 }
            .collect(Collectors.toList())
  }

  /** Вставить атрибут со значением абсолютной отметки забоя скважин */
  fun putBottomOfWellValue(wells: List<MutableMap<String, String>>) {
    var z: Double
    var depth: Double
    var bottomABS: Double
    wells.forEach {
      z = it[nameOfAttributeZ]!!.toDouble()
      depth = it[nameOfAttributeDepth]!!.toDouble()
      bottomABS = Math.round((z - depth) * 100.0) / 100.0
      it[nameOfAttributeBottomWell] = bottomABS.toString()
    }
  }

  /**
   * Каждому слою добавить атрибут, который содержит общее количество
   * таких же слоев, встреченных в текущей скважине. Это необходимо,
   * чтобы в micromine можно было по этому атрибуту подсветить точки,
   * которые встречаются в скважине несколько раз, что свидетельствует
   * о переслаивании стратиграфических подразделений
   * (примером может служить карст). В свою очередь, переслаивание
   * стратиграфических подразделений требует допольнительного
   * редактирования в micromine, поскольку для дальнейшего отстраивания
   * поверхностей в ArcMap по этим точкам, нужно чтобы для точки [x, y]
   * имелось только одно значение z.
   */
  fun assignEachLayersNumberLayersInWell(layers: List<MutableMap<String, String>>) {
    val ids = layers.stream()
            .map { it[nameOfAttributeID] }
            .collect(Collectors.toSet()) // получить набор уникальных id скважин
    ids.forEach { idWell -> // перебор скважин
      val layersForCurrentWell = layers
              .filter { it[nameOfAttributeID] == idWell }
      layersForCurrentWell.forEach {
        it[numberOfLayers] = layersForCurrentWell.size.toString()
      }
    }
  }

  /**
   * Функция для списка слоев пересчитывает значения "От" и "До"
   * относительно абсолютной отметки устья скважины "Z"
   */
  fun absOfFromTo(layers: List<MutableMap<String, String>>) {
    fun getABSValues(layer: Map<String, String>): Pair<String, String> {
      val z = java.lang.Double.valueOf(layer[nameOfAttributeZ])
      val absFrom = Math.round((z - java.lang.Double.valueOf(layer[nameOfAttributeFrom])) * 100.0) / 100.0
      val absTo = Math.round((z - java.lang.Double.valueOf(layer[nameOfAttributeTo])) * 100.0) / 100.0
      return Pair(absFrom.toString(), absTo.toString())
    }
    layers.forEach {
      val (from, to) = getABSValues(it) // деструктурирование
      it[nameOfAttributeFrom] = from
      it[nameOfAttributeTo] = to
    }
  }
}