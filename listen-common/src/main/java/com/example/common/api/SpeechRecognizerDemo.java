package com.example.common.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeechRecognizerDemo {
    private String appKey;
    private String id;
    private String secret;
    private String baseUrl;
    private NlsClient client;
    private String finalResult; // 用于存储最终识别结果

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognizerDemo.class);

    public SpeechRecognizerDemo(String appKey, String id, String secret, String baseUrl) {
        this.appKey = appKey;
        this.id = id;
        this.secret = secret;
        this.baseUrl = baseUrl;
        initializeClient();
    }

    private void initializeClient() {
        AccessToken accessToken = new AccessToken(id, secret);
        try {
            accessToken.apply();
            logger.info("get token: " + accessToken.getToken() + ", expire time: " + accessToken.getExpireTime());
            if (baseUrl.isEmpty()) {
                client = new NlsClient(accessToken.getToken());
            } else {
                client = new NlsClient(baseUrl, accessToken.getToken());
            }
        } catch (IOException e) {
            logger.error("Failed to initialize client", e);
        }
    }

    private SpeechRecognizerListener getRecognizerListener() {
        return new SpeechRecognizerListener() {
            @Override
            public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                logger.info("Intermediate result: " + response.getRecognizedText());
            }

            @Override
            public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                finalResult = response.getRecognizedText(); // 存储最终识别结果
                logger.info("Final result: " + finalResult);
            }

            @Override
            public void onStarted(SpeechRecognizerResponse response) {
                logger.info("Recognition started, task_id: " + response.getTaskId());
            }

            @Override
            public void onFail(SpeechRecognizerResponse response) {
                logger.error("Recognition failed, task_id: " + response.getTaskId() + ", status: " + response.getStatus());
            }
        };
    }

    public String recognizeSpeech(String filepath, int sampleRate) {
        SpeechRecognizer recognizer = null;
        try {
            SpeechRecognizerListener listener = getRecognizerListener();
            recognizer = new SpeechRecognizer(client, listener);
            recognizer.setAppKey(appKey);
            recognizer.setFormat(InputFormatEnum.PCM);
            recognizer.setSampleRate(sampleRate == 16000 ? SampleRateEnum.SAMPLE_RATE_16K : SampleRateEnum.SAMPLE_RATE_8K);
            recognizer.setEnableIntermediateResult(true);
            recognizer.addCustomedParam("enable_voice_detection", true);

            recognizer.start();
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            byte[] b = new byte[3200];
            int len;
            while ((len = fis.read(b)) > 0) {
                recognizer.send(b, len);
                Thread.sleep(getSleepDelta(len, sampleRate));
            }
            recognizer.stop();
            fis.close();
        } catch (Exception e) {
            logger.error("Error during speech recognition", e);
        } finally {
            if (recognizer != null) {
                recognizer.close();
            }
        }
        return finalResult; // 返回最终识别结果
    }

    private int getSleepDelta(int dataSize, int sampleRate) {
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }

    public void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }
}