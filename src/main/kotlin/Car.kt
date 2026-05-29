/**
 * Класс, представляющий автомобиль в системе.
 * Поля:
 * id - идентификатор автомобиля
 * plate - номер автомобиля
 * brand - марка автомобиля
 * driver - имя водителя
 * mileage - пробег
 * rate - тариф
 * status - текущий статус
 */
data class Car(
    val id: Int,
    val plate: String,
    val brand: String,
    val driver: String,
    val mileage: Int,
    val rate: Double,
    val status: String
)
