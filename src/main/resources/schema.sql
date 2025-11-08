CREATE TABLE IF NOT EXISTS tweet_analysis (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              tweet_id VARCHAR(100) NOT NULL,
    lang VARCHAR(8),
    sentiment_label VARCHAR(8),
    sentiment_score DECIMAL(6,4),
    nsfw_label VARCHAR(16),
    nsfw_score DECIMAL(6,4),
    toxicity_score DECIMAL(6,4),
    hate_speech_score DECIMAL(6,4),
    faculty VARCHAR(64),
    topics TEXT,
    confidence_overall DECIMAL(6,4),
    version INT,
    analyzed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tweet_analysis_tweet_id (tweet_id),
    KEY idx_tweet_analysis_sentiment (sentiment_label),
    KEY idx_tweet_analysis_nsfw (nsfw_label)
    );
