package com.example.graduationproject;

import org.junit.Test;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ExampleUnitTest {
    @Test
    public void testGeminiAPIConnect() throws IOException {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        OkHttpClient okHttpClient = new OkHttpClient();
        String baseURL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="+apiKey;
        String jsonText = "{\n" +
                "  \"contents\": [{\"parts\": [{\"text\": \"Hello Gemini, please say hi.\"}]}]\n" +
                "}";

        RequestBody body = RequestBody.create(
                jsonText,
                MediaType.get("application/json; charset=utf-8")
        );
        Request request = new Request.Builder()
                .url(baseURL)
                .post(body)
                .build();

        Response response = okHttpClient.newCall(request).execute();

        String responseBody = response.body().string();
        System.out.println("Gemini 回應：\n" + responseBody);

        assert response.isSuccessful();
    }

}