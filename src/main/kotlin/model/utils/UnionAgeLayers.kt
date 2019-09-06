package model.utils

import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.utils.CollectionUtils.copyListWithSubMap
import java.util.stream.Collectors

/**
 * Класс позволяет объединить сопредельные стратиграфические слои с
 * одинаковым стратиграфическим индексом. Например, если скважина
 * имеет следующую стратиграфию QIV/J1/J1/T2-3/J1/O1ol/Olol/Olol,
 * то на выходе получится стратиграфия: QIV/J1/T2-3/J1/O1ol. При этом,
 * обновятся и абсолютные отметки стратиграфических слоев. Кроме того,
 * предусмотрена также возможность объединения сопредельных пачек,
 * относящихся к одному стратиграфическому подразделению. Например,
 * пачки J1sn$, J1sn@, J1sn#, J1sn! будут объединены в один слой J1sn.
 */
class UnionAgeLayers(layersForUnion: List<MutableMap<String, String>>) {
  private val layersForUnion = copyListWithSubMap(layersForUnion)
  private val layersWithUnion = ArrayList<MutableMap<String, String>>()

  /**
   * Упростить стратиграфические индексы посредством замены индексов
   * пачек на индекс стратиграфического подразделения.
   * Например, если индекс стратиграфического подразделения [indexAge] - J1sn,
   * то всем индексам пачек, таким как J1sn$, J1sn@, J1sn#, J1sn!
   * - будет присвоен индекс J1sn.
   */
  fun simplifyPacketsForIndex(indexAge: String) {
      layersForUnion
        .filter { it[nameOfAttributeLCodeAge].toString().contains(indexAge) }
        .map { it[nameOfAttributeLCodeAge] = indexAge }
  }

  fun getTableWithUnionLayers(): List<MutableMap<String, String>> {
    val ids = layersForUnion.stream()
            .map { it[nameOfAttributeID] }
            .collect(Collectors.toSet()) // получить набор уникальных id скважин
    ids.forEach { idWell -> // перебор скважин
      val layersForCurrentWell = layersForUnion.filter { it[nameOfAttributeID] == idWell }
      // запомнить данные по первому слою
      var from = layersForCurrentWell[0][nameOfAttributeFrom]
      var to = layersForCurrentWell[0][nameOfAttributeTo]
      var indexForUnion = layersForCurrentWell[0][nameOfAttributeLCodeAge]
      // перебор стратиграфических слоев текущей скважины
      layersForCurrentWell.forEach { lay ->
        // если индекс слоя такой же, как и у предыдущего,
        // - переприсвоить значение атрибута "До"
        if (lay[nameOfAttributeLCodeAge] == indexForUnion) {
          to = lay[nameOfAttributeTo]
        } else { // если индекс не такой, как у предыдущего слоя
          // записать предыдущий слой в выходной список,
          addCurrentLay(idWell, from, to, indexForUnion)
          // и запомнить данные по текущему слою
          from = lay[nameOfAttributeFrom]
          to = lay[nameOfAttributeTo]
          indexForUnion = lay[nameOfAttributeLCodeAge]
        }
      }
      // добавить слой по последней скважине
      addCurrentLay(idWell, from, to, indexForUnion)
    }
    return layersWithUnion
  }

  private fun addCurrentLay(vararg attributes: String?) {
    val currentLay = HashMap<String,String>()
    attributes[0]?.let { currentLay.put(nameOfAttributeID, it) }
    attributes[1]?.let { currentLay.put(nameOfAttributeFrom, it) }
    attributes[2]?.let { currentLay.put(nameOfAttributeTo, it) }
    attributes[3]?.let { currentLay.put(nameOfAttributeLCodeAge, it) }
    layersWithUnion.add(currentLay)
  }
}