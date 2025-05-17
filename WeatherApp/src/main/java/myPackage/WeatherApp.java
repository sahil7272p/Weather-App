package myPackage;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/WeatherApp")
public class WeatherApp extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public WeatherApp() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // API key for OpenWeatherMap
        String apiKey = "5a756cb772095bce2d12c2dab882b9a7";
        String city = request.getParameter("city");

        // Validate the city input
        if (city == null || city.trim().isEmpty()) {
            response.getWriter().write("City name cannot be empty.");
            return;
        }

        // Encode city name for URL
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + apiKey;

        HttpURLConnection connection = null;
        Scanner scanner = null;

        try {
            // Create URL object and open connection
            @SuppressWarnings("deprecation")
			URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Check for successful response
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                response.getWriter().write("Failed to fetch weather data. Please check the city name.");
                return;
            }

            // Read response from API
            scanner = new Scanner(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseContent = new StringBuilder();
            while (scanner.hasNextLine()) {
                responseContent.append(scanner.nextLine());
            }

            // Parse the JSON response
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(responseContent.toString(), JsonObject.class);

            // Extract weather data
            long dateTimestamp = jsonObject.get("dt").getAsLong() * 1000; // Convert seconds to milliseconds

            // Format the date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("EE, MMMM dd, yyyy HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getDefault());
            String formattedDate = dateFormat.format(new Date(dateTimestamp));

            double temperatureKelvin = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();
            int temperatureCelsius = (int) (temperatureKelvin - 273.15);

            int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();

            double windSpeed = jsonObject.getAsJsonObject("wind").get("speed").getAsDouble();

            String weatherCondition = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();

            // Set attributes for JSP rendering
            request.setAttribute("date", formattedDate);
            request.setAttribute("city", city);
            request.setAttribute("temperature", temperatureCelsius);
            request.setAttribute("weatherCondition", weatherCondition);
            request.setAttribute("humidity", humidity);
            request.setAttribute("windSpeed", windSpeed);

            // Forward to JSP page
            request.getRequestDispatcher("index.jsp").forward(request, response);

        } catch (IOException e) {
            response.getWriter().write("Error: " + e.getMessage());
        } finally {
            // Close resources
            if (scanner != null) scanner.close();
            if (connection != null) connection.disconnect();
        }
    }
}
