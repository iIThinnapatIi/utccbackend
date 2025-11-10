package com.example.backend1.ingest.twitter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingest/twitter")
public class TwitterController {

    private final TwitterService twitterService;

    // ✅ เขียน constructor เอง (แทน @RequiredArgsConstructor)
    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    /** เรียก recent search จาก Twitter API v2 (คืน JSON เป็น String) */
    @GetMapping("/search")
    public String search(@RequestParam("q") String keyword) {
        return twitterService.searchTweets(keyword);
    }

    /** บันทึก mock_tweets.json จาก resources ลง DB (สำหรับทดสอบเร็ว) */
    @PostMapping("/save-mock")
    public void saveMock() {
        twitterService.saveMockTweets();
    }

    /** อ่านทวีตจาก DB (ฟิลเตอร์ด้วยคำค้นแบบ contains ได้) */
    @GetMapping("/db")
    public List<Tweet> listFromDb(@RequestParam(value = "q", required = false) String keyword) {
        return twitterService.getTweetsFromDB(keyword);
    }
}
