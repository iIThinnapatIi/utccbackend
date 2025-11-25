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

    //  TEMP เก็บโพสต์ชั่วคราวก่อนบันทึกจริง
    private List<PantipPost> tempPosts = new ArrayList<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public PantipScraperService(PantipPostRepository postRepo, PantipCommentRepository commentRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    /*
     *   1) โหมด TEMP — ใช้ตอนค้นหาโพสต์จากหน้า Keywords
     *     - ดึงโพสต์จากผลค้นหา Pantip
     *     - ยังไม่บันทึก DB
     *  */
    public List<PantipPost> scrapePantipTemp(String keyword) {

        tempPosts.clear(); // ล้าง temp ก่อนใช้งานใหม่

        // กำหนด LIMIT ตรงนี้ให้ไม่อืดเกินไป
        final int MAX_PAGES = 2;        // ดึงไม่เกิน 2 หน้า search
        final int MAX_POSTS = 20;       // รวมแล้วไม่เกิน 20 โพสต์

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");

        WebDriver driver = new ChromeDriver(options);
        By TITLE_SELECTOR = By.cssSelector(".pt-list-item__title a");

        try {
            int page = 1;

            while (true) {

                if (page > MAX_PAGES) {
                    System.out.println(" ครบจำนวนหน้าที่กำหนด MAX_PAGES แล้ว หยุดดึงเพิ่ม");
                    break;
                }
                if (tempPosts.size() >= MAX_POSTS) {
                    System.out.println("ครบจำนวนโพสต์ MAX_POSTS แล้ว หยุดดึงเพิ่ม");
                    break;
                }

                String searchUrl =
                        "https://pantip.com/search?q=" +
                                URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                                "&page=" + page;

                System.out.println(" เปิดหน้า search: " + searchUrl);
                driver.get(searchUrl);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                List<WebElement> titleElements = wait.until(
                        ExpectedConditions.presenceOfAllElementsLocatedBy(TITLE_SELECTOR)
                );

                if (titleElements == null || titleElements.isEmpty()) {
                    System.out.println("ไม่พบผลลัพธ์ในหน้า page " + page + " จบการค้นหา");
                    break;
                }

                // เก็บ title + url
                List<String> urls = new ArrayList<>();
                List<String> titles = new ArrayList<>();

                for (WebElement el : titleElements) {
                    if (tempPosts.size() >= MAX_POSTS) break;

                    try {
                        String title = el.getText().trim();
                        String url = el.getAttribute("href");

                        if (title == null || url == null || title.isBlank() || url.isBlank()) {
                            System.out.println("️ ข้ามผลลัพธ์หนึ่งรายการ เพราะ title/url ว่าง");
                            continue;
                        }

                        titles.add(title);
                        urls.add(url);

                    } catch (Exception ex) {
                        System.out.println("ข้ามผลลัพธ์หนึ่งรายการ เพราะอ่าน title/url ไม่ได้");
                    }
                }

                // เข้าไปอ่านรายละเอียดแต่ละโพสต์
                for (int i = 0; i < urls.size(); i++) {
                    if (tempPosts.size() >= MAX_POSTS) break;

                    String url = urls.get(i);
                    String title = titles.get(i);

                    try {
                        System.out.println(" เข้าอ่านโพสต์: " + url);
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
                        ex.printStackTrace();
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
     *  2) Save จาก TEMP → DB จริง
     * --------------------------------------------------------- */
    public int saveTempToDB() {

        int savedCount = 0;

        for (PantipPost p : tempPosts) {
            try {
                // ถ้าโพสต์นี้มีใน DB แล้ว → แจ้งว่าเคยวิเคราะห์ไปแล้ว
                if (postRepo.existsByUrl(p.getUrl())) {
                    System.out.println(" โพสต์นี้เคยถูกวิเคราะห์แล้ว: " + p.getUrl());
                    continue;   // ข้าม ไม่ต้อง insert ซ้ำ
                }

                // 1) Save post
                PantipPost savedPost = postRepo.save(p);

                // 2) Save comments
                if (p.getComments() != null) {
                    for (PantipComment c : p.getComments()) {
                        c.setPost(savedPost);  // FK
                        commentRepo.save(c);
                    }
                }

                savedCount++;

            } catch (DataIntegrityViolationException e) {
                // กันเคส constraint ซ้ำที่หลุดมาอีกชั้น
                System.out.println(" บันทึกโพสต์ล้มเหลว (constraint ซ้ำ): " + p.getUrl());
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(" บันทึกโพสต์ล้มเหลว: " + p.getUrl());
                e.printStackTrace();
            }
        }

        tempPosts.clear();
        return savedCount;
    }

    /*
     *     Clear TEMP (ใช้ตอนกดยกเลิก)
     *  */
    public void clearTemp() {
        tempPosts.clear();
    }

    public List<PantipPost> getTemp() {
        return tempPosts;
    }

    /* ---------------------------------------------------------
     * Helpers — ป้องกัน error
     * --------------------------------------------------------- */

    // กัน NoSuchElement เวลา selector ไม่เจอ
    private String safeGetText(WebDriver driver, String selector) {
        try {
            return driver.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeChildText(WebElement parent, String selector) {
        try {
            return parent.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // เมธอดหลักเก่า (ยังเว้นไว้ให้ต่อยอด)
    public void scrapePantip(String keyword) {
        // ถ้าจะใช้โหมด “บันทึกตรงลง DB เลย” ค่อยมาเติมเพิ่มได้
    }

    public void resetPantipData() {
        // ใส่ logic ลบข้อมูล + reset id ที่นี่ ถ้าต้องการใช้
    }
}