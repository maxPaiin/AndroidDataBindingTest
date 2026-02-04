package Model.POJO;

/**
 * 情緒輸入數據模型
 * 儲存用戶輸入的情緒指數（0-100）
 */
public class EmotionInput {
    private int happy;
    private int sad;
    private int angry;
    private int disgust;
    private int fear;

    public EmotionInput() {}

    public EmotionInput(int happy, int sad, int angry, int disgust, int fear) {
        this.happy = happy;
        this.sad = sad;
        this.angry = angry;
        this.disgust = disgust;
        this.fear = fear;
    }

    public int getHappy() {
        return happy;
    }

    public void setHappy(int happy) {
        this.happy = happy;
    }

    public int getSad() {
        return sad;
    }

    public void setSad(int sad) {
        this.sad = sad;
    }

    public int getAngry() {
        return angry;
    }

    public void setAngry(int angry) {
        this.angry = angry;
    }

    public int getDisgust() {
        return disgust;
    }

    public void setDisgust(int disgust) {
        this.disgust = disgust;
    }

    public int getFear() {
        return fear;
    }

    public void setFear(int fear) {
        this.fear = fear;
    }

    /**
     * 構建發送給 Gemini 的 Prompt
     */
    public String buildPrompt() {
        return "Based on the following emotion indices (scale 0-100, higher means stronger emotion), " +
                "recommend 15 songs that match this emotional state.\n\n" +
                "Emotion Indices:\n" +
                "- Happy: " + happy + "\n" +
                "- Sad: " + sad + "\n" +
                "- Angry: " + angry + "\n" +
                "- Disgust: " + disgust + "\n" +
                "- Fear: " + fear + "\n\n" +
                "IMPORTANT: You MUST respond with ONLY a pure JSON array, no markdown, no explanation, no code blocks. " +
                "The response must start with '[' and end with ']'. " +
                "Each object must have exactly two fields: \"song_name\" and \"artist\". " +
                "Example format: [{\"song_name\":\"Song Title\",\"artist\":\"Artist Name\"}]";
    }
}
