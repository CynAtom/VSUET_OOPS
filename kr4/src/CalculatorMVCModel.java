// start
// Java JDK 17
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

public class CalculatorMVCModel {

// Главный метод для работы программы
public static void main(String[] args) {
    CalculatorModel model;
    model = new CalculatorModel();

    CalculatorView view;
    view = new CalculatorView();

    CalculatorController controller;
    controller = new CalculatorController(model, view);

    controller.runApp();
}



static class CalculatorModel {

// Имя файла с историей
private static final String HISTORY_FILE_NAME = "calc_history.txt";
// Список для хранения истории вычислений в памяти
private final List<CalculationEntry> history;
// Карта для хранения приоритетов операторов
private static final Map<String, Integer> OPERATOR_PRECEDENCE_MAP = Map.of(
        "+", 1, "-", 1,
        "*", 2, "/", 2, "%", 2, "//", 2,
        "^", 3 // '^' и '**' будут иметь самый высокий приоритет
);

// При старте
public CalculatorModel() {
    // Создаем пустой список для истории
    this.history = new ArrayList<>();
    // Забираем историю из файла при запуске
    loadHistoryFromFile();
}

static class CalculationEntry {
    private final String expression;
    private final String result;

    public CalculationEntry(String expression, String result) {
        this.expression = expression;
        this.result = result;
    }

    @Override
    public String toString() {
        return this.expression + " = " + this.result;
    }
}

public String calculate(String expression) {
    try {
        // Преобразуем выражение в формат Reverse Polish Notation
        List<String> rpnExpression = formatToRpn(expression);
        // Вычисляем результат
        double resultValue = calcRpn(rpnExpression);

        // Убираем нули после точки у целых чисел
        String resultString;
        if (resultValue == (long) resultValue) {
            resultString = String.format("%d", (long) resultValue);
        }
        else {
            resultString = String.format("%s", resultValue);
        }

        // Сохраняем в историю и в файл
        history.add(new CalculationEntry(expression, resultString));
        saveHistoryToFile();

        return resultString;

    }
    catch (Exception e) {
        // Ошибка при проблеме
        return "Ошибка: " + e.getMessage();
    }
}

private List<String> formatToRpn(String expression) {
    // Заменяем двусимвольные операторы на односимвольные
    expression = expression.replace("**", "^");
    expression = expression.replace(",", ".");

    List<String> outputQueue = new ArrayList<>();
    Stack<String> operatorStack = new Stack<>();
    // Разбиваем строку на числа и операторы через RegEx
    String[] Item = expression.split("(?<=[-+*/%^()])|(?=[-+*/%^()])");

    // Проходимся по каждому кусочку выражения
    for (String expItem : Item) {
        // Чистим от пустот
        expItem = expItem.trim();
        if (expItem.isEmpty()) continue;

        // Проверяем на цифру
        if (isNumber(expItem)) {
            outputQueue.add(expItem);
        }
        // Работаем с выражением в скобке
        else if (expItem.equals("(")) {
            operatorStack.push(expItem);
        }
        else if (expItem.equals(")")) {
            while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                outputQueue.add(operatorStack.pop());
            }
            if (operatorStack.isEmpty()) throw new IllegalArgumentException("Несогласованные скобки");
            operatorStack.pop(); // Выкидываем открывающую скобку
        }
        else {
            while (!operatorStack.isEmpty() && isOperator(operatorStack.peek()) &&
                    OPERATOR_PRECEDENCE_MAP.get(operatorStack.peek()) >= OPERATOR_PRECEDENCE_MAP.get(expItem)) {
                outputQueue.add(operatorStack.pop());
            }
            operatorStack.push(expItem);
        }
    }

    while (!operatorStack.isEmpty()) {
        if (operatorStack.peek().equals("(")) throw new IllegalArgumentException("Несогласованные скобки");
        outputQueue.add(operatorStack.pop());
    }

    return outputQueue;
}

private double calcRpn(List<String> rpnItems) {
    Stack<Double> valueStack = new Stack<>();

    for (String token : rpnItems) {
        if (isNumber(token)) {
            valueStack.push(Double.parseDouble(token));
        }
        else if (isOperator(token)) {
            if (valueStack.size() < 2) throw new IllegalArgumentException("Недостаточно операндов для операции");
            double rightOperand = valueStack.pop();
            double leftOperand = valueStack.pop();
            double result = 0;

            switch (token) {
                case "+": result = leftOperand + rightOperand; break;
                case "-": result = leftOperand - rightOperand; break;
                case "*": result = leftOperand * rightOperand; break;
                case "/":
                    if (rightOperand == 0) throw new ArithmeticException("Деление на ноль");
                    result = leftOperand / rightOperand;
                    break;
                case "%": result = leftOperand % rightOperand; break;
                case "^": result = Math.pow(leftOperand, rightOperand); break;
                case "//":
                    if (rightOperand == 0) throw new ArithmeticException("Деление на ноль");
                    result = Math.floor(leftOperand / rightOperand);
                    break;
            }
            valueStack.push(result);
        }
    }
    if (valueStack.size() != 1) throw new IllegalArgumentException("Некорректное выражение");
    return valueStack.pop();
}

// Метод для проверки
private boolean isNumber(String token) {
    try {
        Double.parseDouble(token);
        return true;
    } catch (NumberFormatException e) {
        return false;
    }
}
private boolean isOperator(String token) {
    return OPERATOR_PRECEDENCE_MAP.containsKey(token);
}

public List<CalculationEntry> getHistory() {
    return this.history;
}

private void loadHistoryFromFile() {
    File historyFile = new File(HISTORY_FILE_NAME);
    // Отмена при отсутствии файла
    if (!historyFile.exists()) {
        return;
    }

    // Автоматически закроет чтение
    try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
            // Разделяем линию выражения "2 + 2 = 4" на две части по знаку " = "
            String[] parts = line.split(" = ", 2);
            if (parts.length == 2) {
                history.add(new CalculationEntry(parts[0], parts[1]));
            }
        }
    }
    catch (IOException e) {
        System.err.println("Не удалось загрузить историю: " + e.getMessage());
    }
}

private void saveHistoryToFile() {
    // Используем метод для записи, передавая ему путь по умолчанию и всю историю
    writeEntriesToFile(new File(HISTORY_FILE_NAME), this.history);
}

public String exportHistory(String userInput) {
    File targetFile = determineTargetFile(userInput);
    return writeEntriesToFile(targetFile, this.history);
}

public String exportSelectedHistory(List<Integer> indexes, String userInput) {
    File targetFile = determineTargetFile(userInput);

    // Собираются выбранные записи с помощью обычного цикла for
    List<CalculationEntry> selectedEntries = new ArrayList<>();
    for (int index : indexes) {
        if (index >= 0 && index < history.size()) {
            selectedEntries.add(history.get(index));
        }
    }

    if (selectedEntries.isEmpty()) {
        return "Не выбрано ни одной подходящей записи для экспорта.";
    }

    return writeEntriesToFile(targetFile, selectedEntries);
}

private File determineTargetFile(String userInput) {
    // По умолчанию при пустом вводе
    if (userInput == null || userInput.trim().isEmpty()) {
        return new File(HISTORY_FILE_NAME);
    }

    File file = new File(userInput);

    // Указан абсолютный путь с именем файла (например, C:\Users\hiss.txt)
    if (file.isAbsolute() && !file.isDirectory()) { return file; }

    // Указан путь к папке (например, C:\Users\ или my_logs/)
    if (file.isDirectory()) { return new File(file, "log.log"); }

    // Указано только имя файла или относительный путь
    return file;
}

private String writeEntriesToFile(File file, List<CalculationEntry> entries) {
    // Создание родительских папок, если их не существует
    File parentDir = file.getParentFile();
    if (parentDir != null) {
        parentDir.mkdirs();
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        for (CalculationEntry entry : entries) {
            writer.write(entry.toString());
            writer.newLine(); // Переход на новую строку
        }
        // Возвращает полный путь к файлу
        return "Файл сохранен: " + file.getAbsolutePath();
    } catch (IOException e) {
        return "Ошибка при сохранении файла: " + e.getMessage();
    }
}

} // static class CalculatorModel



static class CalculatorView {

// Scanner используется для чтения ввода пользователя из консоли.
private final Scanner scanner;
public CalculatorView() {
    this.scanner = new Scanner(System.in);
}

public void displayMenu() {
    System.out.println("\n---== Калькулятор ==---");
    System.out.println("1. Ввести выражение для расчета");
    System.out.println("2. Посмотреть историю");
    System.out.println("3. Сохранить всю историю в файл");
    System.out.println("4. Сохранить часть истории в файл");
    System.out.println("5. Выход");
    System.out.print("Ваш выбор: ");
}

public String getUserInput(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine();
}

public void showMessage(String message) {
    System.out.println(message);
}

public void showResult(String result) {
    System.out.println("Результат: " + result);
}

public void showHistory(List<CalculatorModel.CalculationEntry> history) {
    if (history.isEmpty()) {
        System.out.println("История вычислений пуста.");
    }
    else {
        System.out.println("\n---== История ==---");
        for (int i = 0; i < history.size(); i++) {
            System.out.println(i + ") " + history.get(i).toString());
        }
        System.out.println("----==========----");
    }
}

public List<Integer> getSelectedIndexesFromUser(int historySize) {
    List<Integer> selectedIndexes = new ArrayList<>();
    if (historySize == 0) {
        showMessage("История пуста.");
        return selectedIndexes;
    }

    showMessage("Введите номера записей для сохранения через запятую (например: 0, 2, 5)");
    String input = getUserInput("Номера: ");

    String[] parts = input.split(","); // Разделяем строку "0, 2, 5" на массив ["0", " 2", " 5"]

    for (String part : parts) {
        try {
            // Убираем пробелы и преобразуем строку в число
            int index = Integer.parseInt(part.trim());
            // Проверка номера
            if (index >= 0 && index < historySize) {
                selectedIndexes.add(index);
            }
            else {
                showMessage("Номер " + index + " вне диапазона истории. Пропущен.");
            }
        }
        catch (NumberFormatException e) {
            // Ошибка при вводе не числа
            showMessage("'" + part + "' не является числом. Пропущено.");
        }
    }
    return selectedIndexes;
}

} // static class CalculatorView



static class CalculatorController {

private final CalculatorModel model;
private final CalculatorView view;

public CalculatorController(CalculatorModel model, CalculatorView view) {
    this.model = model;
    this.view = view;
}

// Главный метод для работы калькулятора
public void runApp() {
    // Цикл кончается при выборе "Выход"
    boolean isRunning = true;
    while (isRunning) {
        view.displayMenu();
        String choice = view.getUserInput("");

        switch (choice) {
            case "1": calculate(); break;
            case "2": displayHistory(); break;
            case "3": exportAllHistory(); break;
            case "4": exportSelected(); break;
            case "5":
                isRunning = false; // Завершаем цикл
                view.showMessage("Программа завершена.");
                break;
            default:
                view.showMessage("Неверный ввод. Выберите цифру от 1 до 5.");
                break;
        }
    }
}

private void calculate() {
    String expression = view.getUserInput("Введите выражение: ");
    if (expression.trim().isEmpty()) {
        view.showMessage("Выражение не может быть пустым.");
        return;
    }
    String result = model.calculate(expression);
    view.showResult(result);
}

private void displayHistory() {
    view.showHistory(model.getHistory());
}

private void exportAllHistory() {
    view.showMessage("Экспорт всей истории.");
    view.showMessage("Оставьте поле пустым, чтобы увидеть путь к файлу истории по умолчанию.");
    String path = view.getUserInput("Введите имя файла или путь для сохранения: ");

    String message;
    if (path.trim().isEmpty()) {
        File defaultFile = new File(CalculatorModel.HISTORY_FILE_NAME);
        message = "Файл истории по умолчанию: " + defaultFile.getAbsolutePath();
    } else {
        message = model.exportHistory(path);
    }
    view.showMessage(message);
}

private void exportSelected() {
    view.showMessage("Экспорт выбранных записей.");
    displayHistory();

    List<Integer> indexes = view.getSelectedIndexesFromUser(model.getHistory().size());

    if (indexes.isEmpty()) {
        view.showMessage("Не выбрано ни одной записи. Экспорт отменен.");
        return;
    }

    String path = view.getUserInput("Введите имя файла или путь для сохранения: ");
    String message = model.exportSelectedHistory(indexes, path);
    view.showMessage(message);
}

} // static class CalculatorController

} // public class CalculatorMVCModel
// end