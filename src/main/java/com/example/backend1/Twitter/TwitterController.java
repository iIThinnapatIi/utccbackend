package com.example.backend1.Twitter;

import com.example.backend1.Twitter.analysis.TweetAnalysis;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class TwitterController {

    private final TwitterService twitterService;

    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @GetMapping("/save")
    public String saveTweets(@RequestParam String keyword) {
        String json = twitterService.searchTweets(keyword);
        twitterService.saveTweetsToDB(json);
        return "Saved tweets to DB!";
    }

    @GetMapping("/twitters")
    public List<Tweet> getTweetsFromDB(@RequestParam(required = false) String keyword) {
        List<Tweet> tweets = twitterService.getTweetsFromDB(keyword);
        System.out.println("Tweets from DB: " + tweets.size());
        return tweets;
    }

    @GetMapping("/save-mock")
    public String saveMockTweets() {
        twitterService.saveMockTweets();
        return "Saved mock tweets to DB!";
    }

    // ✅ endpoint ดึงผลวิเคราะห์จากตาราง tweet_analysis
    @GetMapping("/analysis")
    public List<TweetAnalysis> getAllAnalysis() {
        return twitterService.getAllAnalysis();
    }
}
