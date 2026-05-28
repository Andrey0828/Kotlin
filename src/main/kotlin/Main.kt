import java.io.File
import com.google.gson.Gson
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.PrintStream


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

class TaxiFleetSystem {
    private val cars: MutableList<Car> = mutableListOf()
    private val gson = Gson()
    private val jsonFile = "cars.json"
    private val csvFile = "cars.csv"
    private val statusOptions = listOf("активен", "неактивен", "ремонт", "на заказе")


    // Загружает данные при запуске программы: сначала JSON, если его нет - CSV
    fun loadData() {
        val jsonExists = File(jsonFile).exists()
        val csvExists = File(csvFile).exists()
        when {
            jsonExists -> loadFromJson()
            csvExists -> loadFromCsv()
            else -> {
                println("[!] Файлы не найдены. Начинаем с пустого списка.")
            }
        }
    }


    // Загружает записи автомобилей из JSON-файла
    fun loadFromJson() {
        val file = File(jsonFile)
        try {
            val loaded: List<Car> = gson.fromJson(file.readText(), Array<Car>::class.java)?.toList() ?: emptyList()
            cars.clear()
            cars.addAll(loaded)
            println("Загружено ${loaded.size} записей из JSON.")
        } catch (e: Exception) {
            println("[!] Ошибка чтения JSON: ${e.message}")
        }
    }


    // Преобразует одну CSV-строку в объект Car
    private fun parseCsvRecord(record: CSVRecord): Car? {
        return try {
            Car(
                id = record.get("id").toInt(),
                plate = record.get("plate"),
                brand = record.get("brand"),
                driver = record.get("driver"),
                mileage = record.get("mileage").toInt(),
                rate = record.get("rate").toDouble(),
                status = record.get("status")
            )
        } catch (_: Exception) {
            println("[!] Пропущена строка ${record.recordNumber} (ошибка формата): ${record.toList().joinToString(";")}")
            null
        }
    }


    // Загружает записи автомобилей из CSV-файла
    fun loadFromCsv() {
        val file = File(csvFile)
        try {
            val csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .get()
            val loaded = file.bufferedReader().use { reader ->
                csvFormat.parse(reader).mapNotNull { parseCsvRecord(it) }
            }
            cars.clear()
            cars.addAll(loaded)
            println("Загружено ${loaded.size} записей из CSV.")
        } catch (e: Exception) {
            println("[!] Ошибка чтения CSV: ${e.message}")
        }
    }

    // Печатает заголовок таблицы автомобилей
    private fun printHeader() {
        println()
        println(
            "%-5s %-12s %-15s %-20s %-10s %-8s %-12s"
                .format("ID", "Номер", "Марка", "Водитель", "Пробег", "Тариф", "Статус")
        )
        println("-".repeat(86))
    }

    // Печатает одну запись автомобиля в табличном формате
    private fun printCar(c: Car) {
        println(
            "%-5d %-12s %-15s %-20s %-10d %-8.2f %-12s"
                .format(c.id, c.plate, c.brand, c.driver, c.mileage, c.rate, c.status)
        )
    }

    // Выводит все записи автопарка на экран
    fun displayAll() {
        if (cars.isEmpty()) {
            println("[!] Список автомобилей пуст.")
            return
        }
        printHeader()
        cars.forEach { printCar(it) }
        println()
    }

    // Возвращает следующий свободный идентификатор
    private fun nextId() = (cars.maxOfOrNull { it.id } ?: 0) + 1

    // Добавляет новый автомобиль в список
    fun addCar() {
        println("\n--- Добавление нового автомобиля ---")
        val id = nextId()
        val plate = readNonBlank("Госномер: ")
        val brand = readNonBlank("Марка: ")
        val driver = readNonBlank("Водитель: ")
        val mileage = readInt("Пробег (км): ", min = 0)
        val rate = readDouble("Тариф (руб/км): ", min = 0.0)
        val status = readStatus()

        cars.add(Car(id, plate, brand, driver, mileage, rate, status))
        println("Автомобиль добавлен с ID=$id.")
    }

    // Редактирует существующую запись по идентификатору
    fun editCar() {
        println("\n--- Редактирование записи ---")
        val id = readInt("Введите ID автомобиля: ")
        val idx = cars.indexOfFirst { it.id == id }
        if (idx == -1) {
            println("[!] Автомобиль с ID=$id не найден.")
            return
        }
        val old = cars[idx]
        println("Текущие данные:")
        printHeader()
        printCar(old)
        println()
        println("Подсказка: Enter - оставить без изменений")

        val plate = readOrKeep("Госномер [${old.plate}]: ", old.plate)
        val brand = readOrKeep("Марка [${old.brand}]: ", old.brand)
        val driver = readOrKeep("Водитель [${old.driver}]: ", old.driver)
        val mileage = readIntOrKeep("Пробег [${old.mileage}]: ", old.mileage, min = 0)
        val rate = readDoubleOrKeep("Тариф [${old.rate}]: ", old.rate, min = 0.0)
        val status = readStatusOrKeep("Статус [${old.status}] (${statusOptions.joinToString("/")}): ", old.status)

        cars[idx] = Car(id, plate, brand, driver, mileage, rate, status)
        println("Запись обновлена.")
    }

    // Удаляет запись автомобиля по идентификатору
    fun deleteCar() {
        println("\n--- Удаление записи ---")
        val id = readInt("Введите ID автомобиля: ")
        val removed = cars.removeIf { it.id == id }
        if (removed) {
            println("Запись с ID=$id удалена.")
        } else {
            println("[!] Автомобиль с ID=$id не найден.")
        }
    }

    // Выполняет поиск по конкретному полю и выводит найденные записи
    private fun searchBy(field: String, predicate: (String, Car) -> Boolean) {
        val query = readNonBlank("Запрос по $field: ")
        val result = cars.filter { predicate(query, it) }
        if (result.isEmpty()) {
            println("[!] Ничего не найдено.")
        } else {
            printHeader()
            result.forEach { printCar(it) }
            println()
        }
    }

    // Запускает поиск записей по выбранному текстовому полю
    fun searchCars() {
        println("\n--- Поиск ---")
        println("1. По марке")
        println("2. По водителю")
        println("3. По госномеру")
        println("4. По статусу")
        when (readInt("Выберите критерий: ", 1, 4)) {
            1 -> searchBy("марке") { q, c -> c.brand.contains(q, ignoreCase = true) }
            2 -> searchBy("водителю") { q, c -> c.driver.contains(q, ignoreCase = true) }
            3 -> searchBy("госномеру") { q, c -> c.plate.contains(q, ignoreCase = true) }
            4 -> searchBy("статусу") { q, c -> c.status.equals(q, ignoreCase = true) }
        }
    }

    // Сортирует записи по выбранному полю и выводит результат
    fun sortCars() {
        println("\n--- Сортировка ---")
        println("1. По марке")
        println("2. По водителю")
        println("3. По пробегу")
        println("4. По тарифу")
        println("5. По ID")
        val field = readInt("Выберите поле: ", 1, 5)
        val descending = readInt("Направление (1 - по возрастанию, 2 - по убыванию): ", 1, 2) == 2
        val sorted = when (field) {
            1 -> cars.sortedBy { it.brand }
            2 -> cars.sortedBy { it.driver }
            3 -> cars.sortedBy { it.mileage }
            4 -> cars.sortedBy { it.rate }
            5 -> cars.sortedBy { it.id }
            else -> cars.toList()
        }
        val result = if (descending) sorted.asReversed() else sorted
        printHeader()
        result.forEach { printCar(it) }
        println()
    }

    // Вычисляет и выводит агрегированные показатели по автопарку
    fun showAggregates() {
        if (cars.isEmpty()) {
            println("[!] Нет данных для вычисления.")
            return
        }
        println("\n--- Агрегированные показатели ---")
        println("Количество автомобилей : ${cars.size}")
        println("--- Пробег ---")
        println("Сумма: ${cars.sumOf { it.mileage }} км")
        println("Среднее: ${"%.1f".format(cars.map { it.mileage }.average())} км")
        println("Минимум: ${cars.minOf { it.mileage }} км")
        println("Максимум: ${cars.maxOf { it.mileage }} км")
        println("--- Тариф ---")
        println("Среднее: ${"%.2f".format(cars.map { it.rate }.average())} руб/км")
        println("Минимум: ${"%.2f".format(cars.minOf { it.rate })} руб/км")
        println("Максимум: ${"%.2f".format(cars.maxOf { it.rate })} руб/км")
        println()
    }

    // Сохраняет данные сразу в JSON и CSV-файлы
    fun saveAll() {
        saveToJson()
        saveToCsv()
    }

    // Сохраняет текущий список автомобилей в JSON-файл
    fun saveToJson() {
        try {
            File(jsonFile).writeText(gson.toJson(cars))
            println("Данные сохранены в '$jsonFile'.")
        } catch (e: Exception) {
            println("[!] Ошибка сохранения JSON: ${e.message}")
        }
    }

    // Сохраняет текущий список автомобилей в CSV-файл
    fun saveToCsv() {
        try {
            val sb = StringBuilder("id;plate;brand;driver;mileage;rate;status\n")
            cars.forEach { c ->
                sb.append("${c.id};${c.plate};${c.brand};${c.driver};${c.mileage};${c.rate};${c.status}\n")
            }
            File(csvFile).writeText(sb.toString())
            println("Данные сохранены в '$csvFile'.")
        } catch (e: Exception) {
            println("[!] Ошибка сохранения CSV: ${e.message}")
        }
    }

    // Считывает непустую строку от пользователя
    private fun readNonBlank(prompt: String): String {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim() ?: ""
            if (input.isNotEmpty()) return input
            println("[!] Поле не может быть пустым.")
        }
    }

    // Проверяет, что введенная строка является целым числом в заданном диапазоне
    private fun parseIntInRange(input: String, min: Int, max: Int): Int? {
        val n = input.toIntOrNull()
        return if (n != null && n in min..max) n else null
    }

    // Проверяет, что введенная строка является вещественным числом в заданном диапазоне
    private fun parseDoubleInRange(input: String, min: Double, max: Double): Double? {
        val n = input.toDoubleOrNull()
        return if (n != null && n in min..max) n else null
    }

    // Считывает целое число в заданном диапазоне
    private fun readInt(prompt: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim() ?: ""
            parseIntInRange(input, min, max)?.let { return it }
            println("[!] Введите целое число" + if (min != Int.MIN_VALUE || max != Int.MAX_VALUE) " ($min–$max)." else ".")
        }
    }

    // Считывает вещественное число не меньше заданного минимума
    private fun readDouble(
        prompt: String,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): Double {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim()?.replace(',', '.') ?: ""
            parseDoubleInRange(input, min, max)?.let { return it }
            println("[!] Введите число" + if (min != Double.MIN_VALUE || max != Double.MAX_VALUE) " ($min–$max)." else ".")
        }
    }

    // Считывает статус автомобиля из допустимого набора значений
    private fun readStatus(): String {
        while (true) {
            print("Статус (${statusOptions.joinToString("/")}): ")
            val input = readlnOrNull()?.trim()?.lowercase() ?: ""
            if (input in statusOptions) return input
            println("[!] Допустимые значения: ${statusOptions.joinToString(", ")}.")
        }
    }

    // Считывает новое текстовое значение или оставляет прежнее при пустом вводе
    private fun readOrKeep(prompt: String, default: String): String {
        print(prompt)
        val input = readlnOrNull()?.trim() ?: ""
        return if (input.isEmpty()) default else input
    }

    // Считывает новое целое число или оставляет прежнее при пустом вводе
    private fun readIntOrKeep(
        prompt: String,
        default: Int,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): Int {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim() ?: ""
            if (input.isEmpty()) return default
            parseIntInRange(input, min, max)?.let { return it }
            println("[!] Введите целое число" + if (min != Int.MIN_VALUE || max != Int.MAX_VALUE) " ($min–$max) или нажмите Enter." else " или нажмите Enter.")
        }
    }

    // Считывает новое вещественное число или оставляет прежнее при пустом вводе
    private fun readDoubleOrKeep(
        prompt: String,
        default: Double,
        min: Double = Double.MIN_VALUE,
        max: Double = Double.MAX_VALUE
    ): Double {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim()?.replace(',', '.') ?: ""
            if (input.isEmpty()) return default
            parseDoubleInRange(input, min, max)?.let { return it }
            println("[!] Введите число" + if (min != Double.MIN_VALUE || max != Double.MAX_VALUE) " ($min–$max) или нажмите Enter." else " или нажмите Enter.")
        }
    }

    // Считывает новый статус или оставляет прежний при пустом вводе
    private fun readStatusOrKeep(prompt: String, default: String): String {
        while (true) {
            print(prompt)
            val input = readlnOrNull()?.trim()?.lowercase() ?: ""
            if (input.isEmpty()) return default
            if (input in statusOptions) return input
            println("[!] Допустимые значения: ${statusOptions.joinToString(", ")}.")
        }
    }
}

// Главное меню
fun main() {
    System.setOut(PrintStream(System.out, true, "UTF-8"))
    System.setErr(PrintStream(System.err, true, "UTF-8"))

    val system = TaxiFleetSystem()

    println("============================================")
    println("     Система учёта автопарка такси          ")
    println("============================================")
    println("Загрузка данных...")
    system.loadData()

    while (true) {
        println()
        println("============ ГЛАВНОЕ МЕНЮ ============")
        println("1. Загрузить данные из JSON/CSV")
        println("2. Показать все записи")
        println("3. Добавить автомобиль")
        println("4. Редактировать запись по ID")
        println("5. Удалить запись")
        println("6. Поиск")
        println("7. Сортировка")
        println("8. Агрегированные показатели")
        println("9. Сохранить (JSON + CSV)")
        println("0. Выход")
        println("======================================")
        print("Выбор: ")

        when (readlnOrNull()?.trim()) {
            "1"  -> system.loadData()
            "2"  -> system.displayAll()
            "3"  -> system.addCar()
            "4"  -> system.editCar()
            "5"  -> system.deleteCar()
            "6"  -> system.searchCars()
            "7"  -> system.sortCars()
            "8"  -> system.showAggregates()
            "9"  -> system.saveAll()
            "0"  -> {
                println("Сохранение перед выходом...")
                system.saveAll()
                println("До свидания!")
                return
            }
            else -> println("[!] Неверный пункт меню. Введите число от 0 до 9.")
        }
    }
}
