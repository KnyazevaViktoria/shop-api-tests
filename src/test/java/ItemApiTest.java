import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ItemApiTest {

    private static final String ITEM_URL = "http://shop.bugred.ru/api/items/get/"; //константа (final в Java =  "константа")
    private static final String URL_CREATE = "http://shop.bugred.ru/api/items/create/";
    private static final String URL_UPDATE = "http://shop.bugred.ru/api/items/update/";
    private static final String URL_DELETE = "http://shop.bugred.ru/api/items/delete/";
    private static final String URL_SEARCH = "http://shop.bugred.ru/api/items/search/";


    // ШАБЛОН ДЛЯ ЖСОН "\"название_поля\": \"" + значение + "\""
    @Test
    @DisplayName("1. Получение существующего товара по ID")
    public void getItemTest() {
        String body = "{ \"id\": 68 }";
        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()
        .when()
                .post(ITEM_URL)
        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("result.id", equalTo("68"));
    }

    @Test
    @DisplayName("2. Обработка ошибки при попытке получить товар по несуществующему ID")
    public void getItemTestNegative(){
        String body = "{ \"id\": 100 }";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()

        .when()
                .post(ITEM_URL)

        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("error"))
                .body("error", equalTo("item_with_id_not_found"));  // просто проверяем, что поле ошибки есть


    }

    @Test
    @DisplayName("3. Создание товара успешное")
    public void createItem(){
        String body = "{"
                + "\"name\": \"Тестовый товар\","
                + "\"section\": \"Платья\","
                + "\"description\": \"Создан автотестом\","
                + "\"color\": \"RED\","
                + "\"size\": \"42\","
                + "\"price\": 999.99"
                + "}";
        String response = given() // вот здесь мы создаем переменную, в которую ответ от сервера запишем
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()
        .when()
                .post(URL_CREATE)

        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("result.id", notNullValue())
                .extract() // берём ответ
                .asString(); // превращаем в строку

        int id_CreateItem = JsonPath.from(response).getInt("result.id"); // А ЗДЕСЬ МЫ УЖЕ ИСПОЛЬЗУЕМ ЭТУ ПЕРЕМЕННУЮ
        System.out.println("Создан товар с id: " + id_CreateItem);


    }

    // зесь арх-та теста следующая: обновления товар, затем вызываем снова товар через гет и
    @Test
    @DisplayName("4. Обновление товара с существующим ID")
    public void updateItem(){
        // 1. СОЗДАЁМ уникальный товар с timestamp
        long timestamp = System.currentTimeMillis();
        String uniqueName = "Тестовый товар_" + timestamp;

        String createBody = "{"
                + "\"name\": \"" + uniqueName + "\","
                + "\"section\": \"Платья\","
                + "\"description\": \"Создан автотестом_" + timestamp + "\","
                + "\"color\": \"RED\","
                + "\"size\": \"42\","
                + "\"price\": 999.99"
                + "}";

        String createResponse = given()
                .header("Content-Type", "application/json")
                .body(createBody)
                .log().all()
                .when()
                .post(URL_CREATE)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .extract()
                .asString();

        int itemId = JsonPath.from(createResponse).getInt("result.id");

        // 2. ОБНОВЛЯЕМ этот товар (меняем цвет на BLUE)
        String newColor = "BLUE";
        String body = "{"
                + "\"id\": " + itemId + ","
                + "\"name\": \"Тестовый товар\","
                + "\"section\": \"Платья\","
                + "\"description\": \"Создан автотестом\","
                + "\"color\": \"" + newColor + "\""
                + "}";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()

        .when()
                .post(URL_UPDATE)
        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("result", equalTo("Товар обновлен!"));

        // 3. ПРОВЕРЯЕМ, что цвет действительно изменился (через GET)
        String getBody = "{ \"id\": " + itemId + " }";
        given()
                .header("Content-Type", "application/json")
                .body(getBody)
                .log().ifValidationFails()
        .when()
                .post(ITEM_URL)
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("result.color", equalTo(newColor));


    }

    @Test
    @DisplayName("5. Удаление товара с существующим ID")
    public void deleteItem(){
        long timestamp = System.currentTimeMillis();
        String uniqueName = "Тестовый товар_" + timestamp;

        String createBody = "{"
                + "\"name\": \"" + uniqueName + "\","
                + "\"section\": \"Платья\","
                + "\"description\": \"Создан автотестом_" + timestamp + "\","
                + "\"color\": \"RED\","
                + "\"size\": \"42\","
                + "\"price\": 999.99"
                + "}";

        String createResponse = given()
                .header("Content-Type", "application/json")
                .body(createBody)
                .log().all()
        .when()
                .post(URL_CREATE)
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .extract()
                .asString();

        int itemId = JsonPath.from(createResponse).getInt("result.id");

        String body = "{"
                + "\"id\": \"" + itemId + "\" "
                + "}";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()
        .when()
                .post(URL_DELETE)
        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("ok"))
                .body("result", equalTo("Товар с ID " + itemId + " успешно удален"));



        //3.проверяем, что товара больше нет
        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()
        .when()
                .post(ITEM_URL)
        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("error"))
                .body("message", equalTo("Товар с ID " + itemId + " не найден!"));


    }


    //если я хочу чтобы тест поочередно запустил несколько вариантов в д.случае одежды, то добавляем параметизированные тесты.
    // если у нас @ParameterizedTest ,  то  @Test НЕ ИСПОЛЬЗУЕМ. JUnit путается, какой движок использовать, и выдаёт предупреждение.
    @ParameterizedTest
    @ValueSource(strings = {"Платья", "Юбки", "Шорты", "Шапки"})
    @DisplayName("6. Поиск товара по категории")
    public void itemSearch(String section){
        String body  = "{ \"query\": \"" + section + "\" }";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .log().all()
        .when()
                .post(URL_SEARCH)
        .then()
                .log().all()
                .statusCode(200)
                .body("status", equalTo("ok"));
                //.body("result[0].name", equalTo("Шорты-юбка"));
    }
}