package com.example.backend1.Pantip;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PantipScraperService {

    private final PantipPostRepository postRepo;
    private final PantipCommentRepository commentRepo;

    // TEMP เก็บโพสต์ชั่วคราวก่อนบันทึกจริง
    private List<PantipPost> tempPosts = new ArrayList<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public PantipScraperService(PantipPostRepository postRepo, PantipCommentRepository commentRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    /* ---------------------------------------------------------
     * 1) โหมด TEMP — ใช้ตอนค้นหาโพสต์จากหน้า Keywords
     * --------------------------------------------------------- */
    public List<PantipPost> scrapePantipTemp(String keyword) {

        tempPosts.clear(); // ล้าง temp ก่อนใช้งานใหม่

        final int MAX_PAGES = 2;
        final int MAX_POSTS = 20;

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");

        WebDriver driver = new ChromeDriver(options);
        By TITLE_SELECTOR = By.cssSelector(".pt-list-item__title a");

        try {
            int page = 1;

            while (true) {

                if (page > MAX_PAGES) break;
                if (tempPosts.size() >= MAX_POSTS) break;

                String searchUrl =
                        "https://pantip.com/search?q=" +
                                URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                                "&page=" + page;

                System.out.println(" เปิดหน้า search: " + searchUrl);
                driver.get(searchUrl);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                List<WebElement> titleElements =
                        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(TITLE_SELECTOR));

                if (titleElements == null || titleElements.isEmpty()) break;

                List<String> urls = new ArrayList<>();
                List<String> titles = new ArrayList<>();

                for (WebElement el : titleElements) {
                    if (tempPosts.size() >= MAX_POSTS) break;

                    try {
                        String title = el.getText().trim();
                        String url = el.getAttribute("href");

                        if (title == null || url == null || title.isBlank() || url.isBlank()) continue;

                        titles.add(title);
                        urls.add(url);

                    } catch (Exception ignored) {}
                }

                // เข้าไปอ่านรายละเอียดแต่ละโพสต์
                for (int i = 0; i < urls.size(); i++) {
                    if (tempPosts.size() >= MAX_POSTS) break;

                    String url = urls.get(i);
                    String title = titles.get(i);

                    try {
                        driver.get(url);

                        String author = safeGetText(driver, ".display-post-name");
                        String content = safeGetText(driver, ".display-post-story");
                        String postTime = safeGetText(driver, ".display-post-timestamp");

                        PantipPost post = new PantipPost();
                        post.setTitle(title);
                        post.setUrl(url);
                        post.setPreview("");
                        post.setAuthor(author);
                        post.setContent(content);
                        post.setPostTime(postTime);

                        List<PantipComment> commentList = new ArrayList<>();
                        List<WebElement> commentEls = driver.findElements(
                                By.cssSelector(".display-post-wrapper.section-comment")
                        );

                        for (WebElement cEl : commentEls) {
                            String text = safeChildText(cEl, ".display-post-story");
                            if (text.isEmpty()) continue;

                            PantipComment c = new PantipComment();
                            c.setText(text);
                            c.setAuthor("");
                            c.setCommentedAt("");

                            commentList.add(c);
                        }
                        post.setComments(commentList);

                        tempPosts.add(post);

                    } catch (Exception ex) {
                        System.out.println(" อ่านโพสต์ล้มเหลว: " + url);
                    }
                }

                page++;
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            driver.quit();
        }

        return tempPosts;
    }

    /* ---------------------------------------------------------
     * 2) บันทึกจาก TEMP → DB จริง
     * --------------------------------------------------------- */
    public int saveTempToDB() {

        int savedCount = 0;

        for (PantipPost p : tempPosts) {
            try {
                if (postRepo.existsByUrl(p.getUrl())) {
                    System.out.println("โพสต์นี้มีอยู่แล้ว: " + p.getUrl());
                    continue;
                }

                PantipPost savedPost = postRepo.save(p);

                if (p.getComments() != null) {
                    for (PantipComment c : p.getComments()) {
                        c.setPost(savedPost);  // FK
                        commentRepo.save(c);
                    }
                }

                savedCount++;

            } catch (Exception e) {
                System.out.println("บันทึกโพสต์ล้มเหลว: " + p.getUrl());
            }
        }

        tempPosts.clear();
        return savedCount;
    }

    public void clearTemp() {
        tempPosts.clear();
    }

    public List<PantipPost> getTemp() {
        return tempPosts;
    }


    //  ห้ามลบห้ามลบห้ามลบScheduler
    public void scrapePantip(String keyword) {
        System.out.println(" [AUTO] ดึง Pantip ด้วยคำว่า: " + keyword);

        scrapePantipTemp(keyword);   // ดึงโพสต์มาลง temp
        int saved = saveTempToDB();  // บันทึกจริง

        System.out.println(" [AUTO] บันทึกสำเร็จ: " + saved + " โพสต์");
    }

    /* ---------------------------------------------------------
     * Helpers
     * --------------------------------------------------------- */
    private String safeGetText(WebDriver driver, String selector) {
        try {
            return driver.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) { return ""; }
    }

    private String safeChildText(WebElement parent, String selector) {
        try {
            return parent.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) { return ""; }
    }

    public void resetPantipData() {
        // เว้นไว้สำหรับลบข้อมูลทั้งหมด
    }
}