package model.exception

class GeoTaskException(message: String): Exception(message) {
  companion object {
    private val serialVersionUID: Long = 1L
  }
}