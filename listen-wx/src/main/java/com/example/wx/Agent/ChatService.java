package com.example.wx.Agent;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.example.wx.Agent.dto.ChatRequest;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.mapper.UserTestReportMapper;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.Usertest;
import com.example.wx.pojo.UserTestReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${aliyun.test-analysis.app-id:}")
    private String appId;

    @Value("${aliyun.test-analysis.api-key:}")
    private String apiKey;

    private final Gson gson = new Gson();
    private final UsertestMapper usertestMapper;
    private final TestdetailMapper testdetailMapper;
    private final UserTestReportMapper userTestReportMapper;

    public Flux<String> streamChat(ChatRequest request) {
        if (request == null) {
            return Flux.just("请求参数不能为空");
        }

        enrichTestInfo(request);
        log.info("流式对话请求: content={}", request.getContent());

        try {
            System.out.println("请求内容: " + (request != null ? request.getContent() : "null"));

            Map<String, String> bizParamMap = new HashMap<>();
            Gson gsonWithLocalDate = createGsonWithLocalDateSupport();
            String contentJson = request != null ? gsonWithLocalDate.toJson(request) : "";
            bizParamMap.put("content", contentJson);

            JsonObject bizParams = new JsonObject();
            for (Map.Entry<String, String> entry : bizParamMap.entrySet()) {
                bizParams.addProperty(entry.getKey(), entry.getValue());
            }

            String prompt = request != null ? gsonWithLocalDate.toJson(request) : "";
            System.out.println("提示词: " + prompt);
            System.out.println("App ID: " + appId);
            System.out.println("API Key: " + (apiKey != null ? "已设置" : "未设置"));

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKey)
                    .appId(appId)
                    .prompt(prompt)
                    .bizParams(bizParams)
                    .incrementalOutput(true)
                    .hasThoughts(true)
                    .build();

            Application application = new Application();
            Flowable<ApplicationResult> resultFlowable;
            try {
                resultFlowable = application.streamCall(param);
                System.out.println("成功创建流式调用");
            } catch (NoApiKeyException | InputRequiredException e) {
                System.err.println("API调用异常: " + e.getMessage());
                throw new RuntimeException(e);
            }

            return Flux.from(resultFlowable)
                    .doOnNext(result -> System.out.println("收到结果: " + result))
                    .map(result -> {
                        ApplicationOutput output = result.getOutput();
                        if (output == null) {
                            return "";
                        }
                        if (output.getThoughts() != null) {
                            for (ApplicationOutput.Thought thought : output.getThoughts()) {
                                if (thought.getResponse() != null) {
                                    try {
                                        JsonObject responseJson = gson.fromJson(thought.getResponse(), JsonObject.class);
                                        String nodeName = responseJson.get("nodeName") != null ?
                                                responseJson.get("nodeName").getAsString() : "";
                                        String nodeType = responseJson.get("nodeType") != null ?
                                                responseJson.get("nodeType").getAsString() : "";
                                        if ("大模型1".equals(nodeName) && "LLM".equals(nodeType)) {
                                            String nodeResult = responseJson.get("nodeResult") != null ?
                                                    responseJson.get("nodeResult").getAsString() : "";
                                            if (!nodeResult.isEmpty()) {
                                                JsonObject nodeResultJson = gson.fromJson(nodeResult, JsonObject.class);
                                                String resultText = nodeResultJson.get("result") != null ?
                                                        nodeResultJson.get("result").getAsString() : "";
                                                if (!resultText.isEmpty()) {
                                                    System.out.println("流式文本: " + resultText);
                                                    return resultText;
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        // 忽略解析错误，继续处理其他节点
                                    }
                                }
                            }
                        }
                        String text = output.getText();
                        if (text != null && !text.equals("null") && !text.isEmpty()) {
                            System.out.println("流式文本: " + text);
                            return text;
                        }
                        return "";
                    })
                    .filter(s -> s != null && !s.isEmpty())
                    .doOnError(error -> System.err.println("流处理错误: " + error.getMessage()))
                    .doOnComplete(() -> System.out.println("流处理完成"))
                    .concatWith(Flux.just("[DONE]"));
        } catch (Exception e) {
            System.err.println("streamChat 方法异常: " + e.getMessage());
            e.printStackTrace();
            return Flux.error(e);
        }
    }

    /**
     * 每次都根据 userId 查询用户的最新测试记录，覆盖前端传入的 testInfo
     */
    private void enrichTestInfo(ChatRequest request) {
        String userId = extractUserId(request);
        if (userId == null || userId.isEmpty()) {
            return;
        }

        List<ChatRequest.TestInfo> testInfoList = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
            Usertest latestTest = usertestMapper.getLatestTestByUserId(userId);
            if (latestTest != null) {
                ChatRequest.TestInfo testInfo = new ChatRequest.TestInfo();
                testInfo.setUserId(userId);
                fillTestInfo(testInfo, latestTest, sdf);
                testInfoList.add(testInfo);
            }
        } catch (Exception e) {
            log.warn("查询用户最新测试记录失败: {}", e.getMessage());
        }

        request.setTestInfo(testInfoList.isEmpty() ? null : testInfoList);
    }

    private void fillTestInfo(ChatRequest.TestInfo testInfo, Usertest usertest, SimpleDateFormat sdf) {
        if (testInfo.getTestId() == null || testInfo.getTestId().isEmpty()) {
            testInfo.setTestId(usertest.getId());
        }
        if (testInfo.getTestTime() == null || testInfo.getTestTime().isEmpty()) {
            testInfo.setTestTime(usertest.getEndTime() != null
                    ? sdf.format(usertest.getEndTime())
                    : (usertest.getTestTime() != null ? sdf.format(usertest.getTestTime()) : ""));
        }
        if (testInfo.getAvgScore() == null) {
            testInfo.setAvgScore((float) usertest.getAvgScore());
        }
        if (testInfo.getTotalItems() == null) {
            testInfo.setTotalItems(usertest.getNum());
        }
        if (testInfo.getAnalysis() == null || testInfo.getAnalysis().isEmpty()) {
            testInfo.setAnalysis(usertest.getResultAnalysis());
        }

        // 查详情列表，汇总错误标签
        try {
            List<Testdetail> details = testdetailMapper.selectByTestId(usertest.getId());
            if (details != null && !details.isEmpty()) {
                testInfo.setTotalItems(details.size());
                Set<String> errorTagSet = new LinkedHashSet<>();
                List<Map<String, Object>> detailList = new ArrayList<>();
                for (Testdetail d : details) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("index", d.getIndex());
                    item.put("score", d.getScore());
                    item.put("userContent", d.getUserContent());
                    item.put("errorTags", d.getErrorTags());
                    item.put("resultAnalysis", d.getResultAnalysis());
                    detailList.add(item);

                    if (d.getErrorTags() != null && !d.getErrorTags().isEmpty()) {
                        for (String tag : d.getErrorTags().split(",")) {
                            errorTagSet.add(tag.trim());
                        }
                    }
                }
                if (!errorTagSet.isEmpty()) {
                    testInfo.setErrorTags(String.join(",", errorTagSet));
                }
                // 把详情列表也放进 testInfo 的扩展字段（通过 JSON 序列化带过去）
                testInfo.setAnalysis((testInfo.getAnalysis() != null ? testInfo.getAnalysis() + "\n" : "")
                        + "题目详情: " + gson.toJson(detailList));
            }

            // 查 AI 报告
            UserTestReport report = userTestReportMapper.selectByTestId(usertest.getId());
            if (report != null) {
                StringBuilder analysis = new StringBuilder();
                if (report.getAiAnalysis() != null) {
                    analysis.append("AI综合分析: ").append(report.getAiAnalysis());
                }
                if (report.getImprovementSuggestion() != null) {
                    if (analysis.length() > 0) analysis.append("\n");
                    analysis.append("改善建议: ").append(report.getImprovementSuggestion());
                }
                if (report.getErrorSummary() != null) {
                    if (analysis.length() > 0) analysis.append("\n");
                    analysis.append("错误汇总: ").append(report.getErrorSummary());
                }
                if (testInfo.getAnalysis() != null && !testInfo.getAnalysis().isEmpty()) {
                    analysis.insert(0, testInfo.getAnalysis() + "\n");
                }
                testInfo.setAnalysis(analysis.toString());
            }
        } catch (Exception e) {
            log.warn("查询测试详情或报告失败: {}", e.getMessage());
        }
    }

    private String extractUserId(ChatRequest request) {
        // 简单策略：优先从 testInfo 列表第一个元素取，其次从 history 最后一轮的 user 字段取
        List<ChatRequest.TestInfo> testInfoList = request.getTestInfo();
        if (testInfoList != null && !testInfoList.isEmpty()) {
            ChatRequest.TestInfo first = testInfoList.get(0);
            if (first != null && first.getUserId() != null) {
                return first.getUserId();
            }
        }
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            List<ChatRequest.Memory> history = request.getHistory();
            for (int i = history.size() - 1; i >= 0; i--) {
                String userMsg = history.get(i).getUser();
                if (userMsg != null && userMsg.length() < 50 && userMsg.matches("[\\w-]+")) {
                    return userMsg;
                }
            }
        }
        return null;
    }

    private String buildPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();

        List<ChatRequest.TestInfo> testInfoList = request.getTestInfo();
        if (testInfoList != null && !testInfoList.isEmpty()) {
            for (int i = 0; i < testInfoList.size(); i++) {
                ChatRequest.TestInfo t = testInfoList.get(i);
                sb.append("=== 测试记录 ").append(i + 1).append(" ===\n");
                sb.append("测试ID: ").append(nullSafe(t.getTestId())).append("\n");
                sb.append("用户ID: ").append(nullSafe(t.getUserId())).append("\n");
                sb.append("测试时间: ").append(nullSafe(t.getTestTime())).append("\n");
                sb.append("测试类型: ").append(nullSafe(t.getTestType())).append("\n");
                sb.append("平均分: ").append(t.getAvgScore()).append("\n");
                sb.append("总题数: ").append(t.getTotalItems()).append("\n");
                sb.append("错误标签: ").append(nullSafe(t.getErrorTags())).append("\n");
                sb.append("分析: ").append(nullSafe(t.getAnalysis())).append("\n");
                sb.append("\n");
            }
        }

        List<ChatRequest.Memory> history = request.getHistory();
        if (history != null && !history.isEmpty()) {
            sb.append("=== 对话历史 ===\n");
            for (ChatRequest.Memory m : history) {
                String userMsg = m.getUser();
                String assistantMsg = m.getAssistant();
                if (userMsg != null && !userMsg.isEmpty()) {
                    sb.append("用户: ").append(userMsg).append("\n");
                }
                if (assistantMsg != null && !assistantMsg.isEmpty()) {
                    sb.append("助手: ").append(assistantMsg).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("=== 本次提问 ===\n");
        sb.append(nullSafe(request.getContent()));

        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private Gson createGsonWithLocalDateSupport() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    private String extractText(ApplicationResult result) {
        try {
            ApplicationOutput output = result.getOutput();
            if (output == null) {
                return "";
            }
            String text = output.getText();
            if (text != null && !text.equals("null") && !text.isEmpty()) {
                log.debug("流式文本: {}", text);
                return text;
            }
            return "";
        } catch (Exception e) {
            log.warn("提取流式文本异常: {}", e.getMessage());
            return "";
        }
    }
}
