package com.example.backend1.ingest.pantip;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Service สำหรับค้นหากระทู้ Pantip ตาม keyword โดยใช้ Selenium (headless Chrome) */
@Service
public class PantipService {

    private static final Logger log = LoggerFactory.getLogger(PantipService.class);

    /**
     * ค้นหากระทู้ Pantip ด้วย keyword ที่กำหนด
     * @param keyword คำค้น (เช่น "UTCC", "เรียนการตลาด")
     * @return รายการโพสต์ที่พบ
     */
    public List<PantipPost> searchPosts(String keyword) {
        List<PantipPost> results = new ArrayList<>();
        WebDriver driver = null;

        try {
            // ✅ เตรียม ChromeDriver
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");       // headless รุ่นใหม่
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--lang=th-TH");
            options.addArguments(
                    "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);

            // ✅ เข้า Pantip Search URL
            String url = "https://pantip.com/search?q=" +
                    URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            log.info("🌐 Opening: {}", url);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".pt-list-item__title")));

            // ✅ ดึงรายการกระทู้ทั้งหมด
            List<WebElement> postContainers = driver.findElements(By.cssSelector(".pt-list-item"));

            for (WebElement container : postContainers) {
                try {
                    // Title + URL
                    WebElement titleElement = container.findElement(
                            By.cssSelector(".pt-list-item__title a"));
                    String title = titleElement.getText().trim();
                    String link  = titleElement.getAttribute("href");

                    // Preview (ถ้ามี)
                    String preview = "";
                    try {
                        WebElement commentPreview = container.findElement(
                                By.cssSelector(".pt-list-item__sr__comment__inner"));
                        preview = commentPreview.getText().trim();
                    } catch (NoSuchElementException e1) {
                        try {
                            WebElement contentPreview = container.findElement(
                                    By.cssSelector(".pt-list-item__sr__content__inner"));
                            preview = contentPreview.getText().trim();
                        } catch (NoSuchElementException e2) {
                            preview = "";
                        }
                    }

                    // ✅ ใช้คอนสตรัคเตอร์ 3 พารามิเตอร์ (ทางเลือก A)
                    PantipPost post = new PantipPost(title, link, preview);
                    // ใส่ content เริ่มต้นเป็น preview ไปก่อน (ถ้าอยาก)
                    post.setContent(preview);
                    // sentiment ยังไม่วิเคราะห์
                    post.setSentiment(null);

                    results.add(post);
                } catch (NoSuchElementException ignored) {
                    // ข้ามโพสต์ที่ไม่สมบูรณ์
                }
            }

        } catch (Exception e) {
            log.error("❌ Pantip search error", e);
        } finally {
            if (driver != null) driver.quit();
        }

        log.info("✅ ดึงโพสต์สำเร็จ {} รายการ จากคำค้น '{}'", results.size(), keyword);
        return results;
    }
}
