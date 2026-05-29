import java.io.PrintStream


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
