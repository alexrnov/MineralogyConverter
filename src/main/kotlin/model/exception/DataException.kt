package model.exception

class DataException(message: String): Exception(message) {
  companion object {
    private val serialVersionUID: Long = 3L
  }
}