package com.example.backend1.Pantip;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PantipService {

    public List<PantipPost> searchPosts(String keyword) {
        List<PantipPost> results = new ArrayList<>();
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // รันแบบไม่เปิด Chrome UI
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);

            // เข้า URL Pantip search
            String url = "https://pantip.com/search?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pt-list-item__title")));

            // หารายการกระทู้ทั้งหมด
            List<WebElement> postContainers = driver.findElements(By.cssSelector(".pt-list-item"));

            for (WebElement container : postContainers) {
                try {
                    // ดึง title + url
                    WebElement titleElement = container.findElement(By.cssSelector(".pt-list-item__title a"));
                    String title = titleElement.getText().trim();
                    String link = titleElement.getAttribute("href");

                    // --- ดึง preview ---
                    String preview = "";

                    try {
                        // 1) ลองหา preview จากคอมเมนต์ (คห.1)
                        WebElement commentPreview = container.findElement(By.cssSelector(".pt-list-item__sr__comment__inner"));
                        preview = commentPreview.getText().trim();
                    } catch (NoSuchElementException e1) {
                        try {
                            // 2) ถ้าไม่มีคอมเมนต์ → หา preview จากเนื้อหากระทู้แทน
                            WebElement contentPreview = container.findElement(By.cssSelector(".pt-list-item__sr__content__inner"));
                            preview = contentPreview.getText().trim();
                        } catch (NoSuchElementException e2) {
                            preview = "";
                        }
                    }

                    results.add(new PantipPost(title, link, preview));

                } catch (NoSuchElementException ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }

        return results;
    }
}
