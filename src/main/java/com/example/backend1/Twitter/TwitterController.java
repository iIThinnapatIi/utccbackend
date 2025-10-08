package com.example.backend1.Twitter;

import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TwitterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class TwitterController {

    private final TwitterService twitterService;

    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    // ดึงและบันทึก tweets
    @GetMapping("/save")
    public String saveTweets(@RequestParam String keyword) {
        String json = twitterService.searchTweets(keyword); // ดึงจาก Twitter API
        twitterService.saveTweetsToDB(json); // บันทึกลง DB
        return "Saved tweets to DB!";
    }

    // ดึง tweets จาก DB
    @GetMapping("/twitters")
    public List<Tweet> getTweetsFromDB(@RequestParam(required = false) String keyword) {
        List<Tweet> tweets = twitterService.getTweetsFromDB(keyword);
        System.out.println("Tweets from DB: " + tweets.size()); // debug
        return tweets;
    }

    //ทดลองtw อย่าใส่ใจ
    @GetMapping("/save-mock")
    public String saveMockTweets() {
        twitterService.saveMockTweets();
        return "Saved mock tweets to DB!";
    }
}

