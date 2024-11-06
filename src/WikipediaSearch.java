import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class WikipediaSearch
{
    WikipediaApi api = new WikipediaApi();
    Scanner scanner = new Scanner(System.in);

    public void run()
    {
        try
        {
            System.out.print("Введите запрос для поиска на Википедии: ");
            String query = scanner.nextLine().trim();

            if (query.isEmpty())
            {
                System.out.println("Запрос не должен быть пустым. Пожалуйста, попробуйте снова.");
                return;
            }

            List<WikipediaSearchResult> results = api.search(query);
            if (results.isEmpty())
            {
                System.out.println("Результаты не найдены.");
                return;
            }

            System.out.println("Результаты поиска:");
            for (int i = 0; i < results.size(); i++)
            {
                System.out.printf("%d. %s\n", i + 1, results.get(i).getTitle());
            }

            System.out.print("Выберите номер статьи для открытия: ");
            int choice = scanner.nextInt();
            if (choice < 1 || choice > results.size())
            {
                System.out.println("Неверный выбор. Попробуйте снова.");
                return;
            }

            WikipediaSearchResult selectedArticle = results.get(choice - 1);
            String articleText = api.getArticleText(selectedArticle.getPageId());
            System.out.println("\nТекст статьи:");
            System.out.println(articleText);
            WikipediaBrowser.openArticle(selectedArticle.getPageId());

        }
        catch (Exception e)
        {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
        finally
        {
            scanner.close();
        }
    }

    public static void main(String[] args)
    {
        new WikipediaSearch().run();
    }
}

class WikipediaApi
{
    private static final String BASE_URL = "https://ru.wikipedia.org/w/api.php";
    private static final Gson gson = new Gson();

    public List<WikipediaSearchResult> search(String query) throws Exception
    {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = BASE_URL + "?action=query&list=search&utf8=&format=json&srsearch=" + encodedQuery;
        JsonObject response = sendRequest(urlString);
        JsonArray searchResults = response.getAsJsonObject("query").getAsJsonArray("search");
        List<WikipediaSearchResult> results = new ArrayList<>();
        for (int i = 0; i < searchResults.size(); i++)
        {
            JsonObject result = searchResults.get(i).getAsJsonObject();
            int pageId = result.get("pageid").getAsInt();
            String title = result.get("title").getAsString();
            results.add(new WikipediaSearchResult(pageId, title));
        }
        return results;
    }

    public String getArticleText(int pageId) throws Exception
    {
        String urlString = BASE_URL + "?action=query&prop=extracts&explaintext&format=json&pageids=" + pageId;
        JsonObject response = sendRequest(urlString);
        JsonObject pages = response.getAsJsonObject("query").getAsJsonObject("pages");
        JsonObject article = pages.getAsJsonObject(String.valueOf(pageId));
        return article.get("extract").getAsString();
    }

    private JsonObject sendRequest(String urlString) throws Exception
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null)
        {
            response.append(line);
        }
        in.close();
        return gson.fromJson(response.toString(), JsonObject.class);
    }
}

// Класс модели для хранения результата поиска
class WikipediaSearchResult
{
    private int pageId;
    private String title;

    public WikipediaSearchResult(int pageId, String title)
    {
        this.pageId = pageId;
        this.title = title;
    }

    public int getPageId()
    {
        return pageId;
    }

    public String getTitle()
    {
        return title;
    }
}

class WikipediaBrowser
{
    public static void openArticle(int pageId)
    {
        String pageUrl = "https://ru.wikipedia.org/w/index.php?curid=" + pageId;
        try
        {
            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(new URI(pageUrl));
                System.out.println("Открыта страница: " + pageUrl);
            }
            else
            {
                System.out.println("Откройте страницу сами :)");
            }
        }
        catch (Exception e)
        {
            System.out.println("Не удалось открыть страницу: " + e.getMessage());
        }
    }
}