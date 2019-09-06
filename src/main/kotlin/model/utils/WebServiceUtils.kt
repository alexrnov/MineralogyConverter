package model.utils

import model.constants.CommonConstants.nameOfAttributeDepth
import model.constants.CommonConstants.noData
import model.constants.ProbesWithMSDConstants
import model.constants.ProbesWithMSDConstants.firstIndexOfNumberCrystal
import model.constants.ProbesWithMSDConstants.firstIndexOfNumberMineral
import model.constants.ProbesWithMSDConstants.indexAndNameOfColumns
import model.constants.ProbesWithMSDConstants.lastIndexOfNumberCrystal
import model.constants.ProbesWithMSDConstants.lastIndexOfNumberMineral
import model.constants.ProbesWithMSDConstants.numberOfProbe
import model.constants.ProbesWithoutMSDConstants
import java.util.stream.Collectors
import java.lang.Double.valueOf as toDouble

/**
 * Набор утилит для решения задач, связанных с данными, загруженными с
 * Web-ресурса "МСА по всем объектам"
 */
object WebServiceUtils {

  /**
   * Возвращает список скважин с уникальными именемами скважин(номер/линия).
   * Используется для создания файла устьев скважин - чтобы нескольким пробам
   * из одной скважины соответствовала только одна скважина в файле устьев
   * Используется для задач, связанных с обработкой данных минералогии
   * из excel-файла  и загрузкой их в Micromine.
   * [topWells] список скважин, где возможны скважины с повторяющимися именами
   * @return скважины с уникальными именами
   */
  fun getWellsWithUniqueNames(topWells: List<MutableMap<String, String>>):
          MutableList<MutableMap<String, String>> {
    val uniqueValues = ArrayList<MutableMap<String, String>>()
    uniqueValues.add(HashMap(topWells.first()))
    // переменная определяет, добавлена ли скважина в список уникальных скважин
    var addedWell: Boolean
    for (i in 1 until topWells.size) {
      var k = 0
      do { // перебор уникальных скважин
        addedWell = equalsNamesWells(topWells[i], uniqueValues[k])
        k++
      // если список уникальных скважин пройден или найдена новая скважина
      } while (k < uniqueValues.size && !addedWell)
      if (!addedWell) { // если текущей скважины нет в списке уникальных скважин
        uniqueValues.add(HashMap(topWells[i])) // добавить ее в список
      }
    }
    return uniqueValues
  }

  private fun equalsNamesWells(list1: Map<String, String>,
                               list2: Map<String, String>): Boolean =
    list1["Линия"] == list2["Линия"] && list1["Точка"] == list2["Точка"]

  /* поправка ИСИХОГИ */
  fun makeAmendment(wells: List<MutableMap<String, String>>) {
    wells.forEach { well ->
      var x = toDouble(well["X"])
      var y = toDouble(well["Y"])
      var z = toDouble(well["Z"])
      x -= 20000
      y -= 10000
      /* округлить координаты до двух знаков */
      x = Math.round(x * 100.0) / 100.0
      y = Math.round(y * 100.0) / 100.0
      z = Math.round(z * 100.0) / 100.0
      well.replace("X", x.toString())
      well.replace("Y", y.toString())
      well.replace("Z", z.toString())
    }
  }

  /** Заменить запятую на точку для некоторых атрибутов */
  fun replaceCommaForWells(wells: List<MutableMap<String, String>>) {
    wells.forEach { currentProbe ->
      currentProbe.replace("X", currentProbe["X"]!!.replace(",", "."))
      currentProbe.replace("Y", currentProbe["Y"]!!.replace(",", "."))
      currentProbe.replace("Z", currentProbe["Z"]!!.replace(",", "."))
      currentProbe.replace("От", currentProbe["От"]!!.replace(",", "."))
      currentProbe.replace("До", currentProbe["До"]!!.replace(",", "."))
      currentProbe.replace("Объем", currentProbe["Объем"]!!.replace(",", "."))
    }
  }

  /**
   * Проверить, является ли значение "От" меньше "До". Если нет, тогда
   * значению "До" присвоить значение "От" + 0.1
   */
  fun checkSequenceIntervals(intervalWells: List<MutableMap<String, String>>) {
    var from: Double
    var to: Double
    for (probe in intervalWells) {
      from = probe["От"]!!.toDouble()
      to = probe["До"]!!.toDouble()
      if (from >= to) {
        var newTo = from + 0.1
        newTo = Math.round(newTo * 100.0) / 100.0 // округлить до двух знаков
        probe["До"] = newTo.toString()
      }
    }
  }

  /**
   * В зависимости от типа выборки по стратиграфии произвести
   * фильтрацию таблицы данных
   */
  fun selectionByGeologicalAge(probes: MutableList<MutableMap<String, String>>,
                               typeOfSelectionAge: String):
          MutableList<MutableMap<String, String>> = when (typeOfSelectionAge) {
      "Все пробы" -> probes
      "По всем возрастам" -> probes.stream()
                .filter { it["Стратиграфия"] != "Нет данных"}
                .collect(Collectors.toList())
      "Без возрастов" -> probes.stream()
                .filter { it["Стратиграфия"] == "Нет данных" }
                .collect(Collectors.toList())
      else -> probes.stream()
                .filter { it["Стратиграфия"] == typeOfSelectionAge}
                .collect(Collectors.toList())
  }

  /**
   * Поиск ошибок отсутствия пространственных данных, которые необходимы для
   * картирования. Если данные отсутствуют, им присваиваются значения по
   * умолчанию, которые затем необходимо исправить в среде Micromine/arcGis.
   * Возвращает список который содержит перечень информационных сообщений
   * для отображения на консоли.
   */
  fun checkOnMissSpatialData(probes: List<MutableMap<String, String>>):
          MutableList<String> {
    val zero = "0"
    val z = ProbesWithMSDConstants.defaultZ
    val to = "1"
    val mistakes: MutableList<String> = ArrayList()
    probes.forEach {
      if (it["X"] == noData) {
        it["X"] = zero
        mistakes.add("Нет информации по координате X. № в объекте работ: " +
          "${it[numberOfProbe]}. Координате X присвоено значение $zero м")
      }

      if (it["Y"] == noData)  {
        it["Y"] = zero
        mistakes.add("Нет информации по координате Y. № в объекте работ: " +
          "${it[numberOfProbe]}. Координате Y присвоено значение $zero м")
      }

      if (it["Z"] == noData) {
        it["Z"] = z
        mistakes.add("Нет информации по кооринате Z. № в объекте работ: " +
          "${it[numberOfProbe]}. Координате Z присвоено значение $z м")
      }

      if (it["От"] == noData || it["До"] == noData) {
        it["От"] = zero
        it["До"] = to
        mistakes.add("Не хватает информации по значениям интервала от-до. " +
          "№ в объекте работ: ${it[numberOfProbe]}. " +
          "Атрибуту ${"От"} присвоено значение: $zero м. " +
          "Атрибуту ${"До"} присвоено значение: $to м")
      }
    }
    return mistakes
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
        if (e1["IDW"] != e2["IDW"] && e1["X"] == e2["X"] && e1["Y"] == e2["Y"]
                && e1["Z"] == e2["Z"]) {
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
          var x: Double = well["X"]!!.toDouble() + increment
          x = Math.round(x * 100.0) / 100.0 // округлить до двух знаков
          // необязательно делать изменение отображений в коллекции topWells,
          // достаточно лишь изменить отображения в текущей коллекции
          // совпавших по XYZ скважин, поскольку в нее добавлены отображения,
          // ссылающиеся на коллекцию topWells
          well["X"] = x.toString()
          // координата следующей скважины будет смещена на 0.01 м
          increment += 0.01
        }
        coincidentWells.clear()
      }
      wellsForSearch.remove(e1) // исключить текущий элемент из поиска
    }
  }

  /**
   * Присвоить интервалам id-номера из таблицы устьев скважин, с той целью,
   * чтобы потом связать файл устьев и файл интервалов в Micromine
   */
  fun assignIDToIntervals(uniqueTopWells: List<MutableMap<String, String>>,
                          intervalWells: List<MutableMap<String, String>>) {
    uniqueTopWells.forEach { top ->
      intervalWells.forEach { interval ->
        if (equalsNamesWells(top, interval))
          interval["IDW"] = top["IDW"].toString()
      }
    }
  }

  /**
   * Пересчет количеста найденных зерен МСА и количества минералов
   * (сервис "МСА по всем объектам") на определенный объем
   * (например на 5, 10, 15, 20 литров). Обновляет поля, которые содержат
   * количестов кристаллов для различных минералов и их совокупностей.
   * [intervalWells] - список, который содержит отображения проб.
   * [referenceVolumeProbe] - эталонный объем пробы (обычно 5 или 10 литров).
   */
  fun updateCrystalNumberWithMSD(intervalWells: List<MutableMap<String, String>>,
                                 referenceVolumeProbe: Byte) {
    intervalWells.forEach { currentProbe ->
      var realProbeVolume: String? = currentProbe["Объем"]
      if (realProbeVolume != noData) {
        realProbeVolume = realProbeVolume!!.substring(0,
                realProbeVolume.length - 2)
        // перебрать все значения с количеством кристаллов МСА для различных типов
        // минералов, и заменить эти значения на пересчитанные с эталонным объемом
        (firstIndexOfNumberCrystal..lastIndexOfNumberCrystal).forEach {
          recalculationVolumeWithMSD(it, currentProbe, referenceVolumeProbe,
                  realProbeVolume)
        }

        // перебрать все значения с количеством минералов (альмандин, гроссуляр)
        // и заменить эти значения на пересчитанные с эталонным объемом
        (firstIndexOfNumberMineral..lastIndexOfNumberMineral).forEach {
          recalculationVolumeWithMSD(it, currentProbe, referenceVolumeProbe,
                  realProbeVolume)
        }
      }
    }
  }

  private fun recalculationVolumeWithMSD(index: Int, currentProbe: MutableMap<String, String>,
                                         referenceVolume: Byte, realVolume: String) {
    val numberOfCrystal = toDouble(
            currentProbe[indexAndNameOfColumns[index]]!!.replace(",","."))
    if (numberOfCrystal != 0.0) {
      var newValue = numberOfCrystal * referenceVolume / toDouble(realVolume)
      // округлить до двух знаков
      newValue = Math.round(newValue * 100.0) / 100.0
      val key: String? = indexAndNameOfColumns[index]
      if (key != null) {
        currentProbe[key] = newValue.toString()
      }
    }
  }

  /**
   * Пересчет количеста найденных минералов (Сервис "Пробы без МСА")
   * на определенный объем (например на 5, 10, 15, 20 литров).
   * Обновляет поля, которые содержат количестов кристаллов
   * для различных минералов и их совокупностей.
   * [intervalWells] - список, который содержит отображения проб.
   * [referenceVolumeProbe] - эталонный объем пробы (обычно 5 или 10 литров).
   */
  fun updateCrystalNumberWithoutMSD(intervalWells: List<MutableMap<String, String>>,
                                    referenceVolumeProbe: Byte) {
    intervalWells.forEach { currentProbe ->
      var realProbeVolume: String? = currentProbe["Объем"]
      if (realProbeVolume != noData) {
        realProbeVolume = realProbeVolume!!.substring(0,
                realProbeVolume.length - 2)

        // перебрать все значения с количеством минералов (альмандин, гроссуляр)
        // и заменить эти значения на пересчитанные с эталонным объемом
        (ProbesWithoutMSDConstants.firstIndexOfNumberMineral..ProbesWithoutMSDConstants.lastIndexOfNumberMineral).forEach {
          recalculationVolumeWithoutMSD(it, currentProbe, referenceVolumeProbe,
                  realProbeVolume)
        }
      }
    }
  }

  private fun recalculationVolumeWithoutMSD(index: Int, currentProbe: MutableMap<String, String>,
                                            referenceVolume: Byte, realVolume: String) {
    val numberOfCrystal = toDouble(
            currentProbe[ProbesWithoutMSDConstants.indexAndNameOfColumns[index]]!!.replace(",", "."))
    if (numberOfCrystal != 0.0) {
      var newValue = numberOfCrystal * referenceVolume / toDouble(realVolume)
      // округлить до двух знаков
      newValue = Math.round(newValue * 100.0) / 100.0
      val key: String? = ProbesWithoutMSDConstants.indexAndNameOfColumns[index]
      if (key != null) {
        currentProbe[key] = newValue.toString()
      }
    }
  }


  /**
   * Для каждой скважины находится самый глубокий интервал
   * опробования, подошва этого интервала и будет значением
   * глубины скважины.
   */
  fun defineDepthOfWells(topWells: List<MutableMap<String, String>>,
                         intervalWells: List<MutableMap<String, String>>) {
    topWells.forEach { well ->
      // максимальное значение подошвы интервала опробования, которое
      // и будет глубиной скважины
      val max = intervalWells
              .filter { it["IDW"] == well["IDW"] } // слои для текущей скважины
              .map { it["До"]?.toDouble()?: 1000.0 } // получить значение подошвы
              .max() // получить максимальное значение
              .toString()
      well[nameOfAttributeDepth] = max
    }
  }
}
