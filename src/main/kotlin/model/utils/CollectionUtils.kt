package model.utils

object CollectionUtils {

  /**
   * Полностью копирует список со вложенными коллекциями
   * Map<String, String> (глубокое копирование)
   * [inputList] копируемый список с вложенными отображениями
   * Возвращает полную копию списка [inputList]
   */
  fun copyListWithSubMap(inputList: List<MutableMap<String, String>>):
          MutableList<MutableMap<String, String>> {
    val copyList = ArrayList<MutableMap<String, String>>()
    inputList.forEach { e ->
      val copyMap = e.toMutableMap() // или val copyMap = HashMap(e)
      copyList.add(copyMap)
    }
    return copyList
  }

  /**
   * Для вложенных коллекций оставляет только те элементы ключ-значение,
   * ключи которых соответствуют списку ключей [requiredFields]
   */
  fun retainRequiredFields(list: List<MutableMap<String, String>>,
                           requiredFields: Set<String>) {
    list.map {
      // создать новый экземпляр - иначе при удалении элементов
      // из e повреждается объект keys
      val keys = it.keys.toMutableSet() // набор ключей
      // или val keys: MutableSet<String> = HashSet(it.keys)
      keys.removeAll(requiredFields) // удалить из списка ненужных - нужные
      keys.forEach {key -> it.remove(key) } // удалить ненужные пары ключ-значение
    }
  }
}