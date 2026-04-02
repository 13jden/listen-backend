package com.example.common.api;

import com.aliyun.bailian20231229.Client;
import com.aliyun.bailian20231229.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;

/**
 * 阿里云百炼知识库API服务
 */
@Slf4j
@Component
public class BailianKnowledgeService {

    @Value("${aliyun.bailian.workspace-id:}")
    private String workspaceId;

    @Value("${aliyun.bailian.api-key:}")
    private String apiKey;

    private Client bailianClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.setAccessKeyId(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID"));
            config.setAccessKeySecret(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET"));
            config.endpoint = "bailian.cn-beijing.aliyuncs.com";

            bailianClient = new Client(config);
            log.info("阿里云百炼客户端初始化完成，workspaceId: {}", workspaceId);
        } catch (Exception e) {
            log.error("阿里云百炼客户端初始化失败", e);
        }
    }

    /**
     * 申请文件上传租约
     */
    private ApplyFileUploadLeaseResponse applyLease(String categoryId, String fileName, 
            String fileMd5, String fileSize, String wsId) throws Exception {
        
        log.info("申请文件上传租约，文件名: {}, MD5: {}, 大小: {}字节", fileName, fileMd5, fileSize);

        Map<String, String> headers = new HashMap<>();
        ApplyFileUploadLeaseRequest request = new ApplyFileUploadLeaseRequest();
        request.setFileName(fileName);
        request.setMd5(fileMd5);
        request.setSizeInBytes(fileSize);
        
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.applyFileUploadLeaseWithOptions(categoryId, wsId, request, headers, runtime);
    }

    /**
     * 上传文件到临时存储
     */
    private void uploadFile(String preSignedUrl, Map<String, String> headers, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是普通文件: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            URL url = new URL(preSignedUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);

            conn.setRequestProperty("X-bailian-extra", headers.get("X-bailian-extra"));
            conn.setRequestProperty("Content-Type", headers.get("Content-Type"));

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                conn.getOutputStream().write(buffer, 0, bytesRead);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("上传失败: " + responseCode);
            }
            log.info("文件上传成功: {}", filePath);
        }
    }

    /**
     * 添加文件到类目
     */
    private AddFileResponse addFile(String leaseId, String parser, String categoryId, String wsId) throws Exception {
        log.info("添加文件到知识库，leaseId: {}", leaseId);

        Map<String, String> headers = new HashMap<>();
        AddFileRequest request = new AddFileRequest();
        request.setLeaseId(leaseId);
        request.setParser(parser);
        request.setCategoryId(categoryId);
        
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.addFileWithOptions(wsId, request, headers, runtime);
    }

    /**
     * 查询文件基本信息
     */
    private DescribeFileResponse describeFile(String wsId, String fileId) throws Exception {
        Map<String, String> headers = new HashMap<>();
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.describeFileWithOptions(wsId, fileId, headers, runtime);
    }

    /**
     * 创建知识库
     */
    private CreateIndexResponse createIndex(String wsId, String fileId, String name, 
            String structureType, String sourceType, String sinkType) throws Exception {
        
        log.info("创建知识库: {}", name);

        Map<String, String> headers = new HashMap<>();
        CreateIndexRequest request = new CreateIndexRequest();
        request.setStructureType(structureType);
        request.setName(name);
        request.setSourceType(sourceType);
        request.setSinkType(sinkType);
        request.setDocumentIds(Collections.singletonList(fileId));
        
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.createIndexWithOptions(wsId, request, headers, runtime);
    }

    /**
     * 提交索引任务
     */
    private SubmitIndexJobResponse submitIndex(String wsId, String indexId) throws Exception {
        Map<String, String> headers = new HashMap<>();
        SubmitIndexJobRequest request = new SubmitIndexJobRequest();
        request.setIndexId(indexId);
        
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.submitIndexJobWithOptions(wsId, request, headers, runtime);
    }

    /**
     * 获取索引任务状态
     */
    private GetIndexJobStatusResponse getIndexJobStatus(String wsId, String jobId, String indexId) throws Exception {
        Map<String, String> headers = new HashMap<>();
        GetIndexJobStatusRequest request = new GetIndexJobStatusRequest();
        request.setIndexId(indexId);
        request.setJobId(jobId);
        
        RuntimeOptions runtime = new RuntimeOptions();
        return bailianClient.getIndexJobStatusWithOptions(wsId, request, headers, runtime);
    }

    /**
     * 创建知识库完整流程
     * @param filePath 文件路径
     * @param kbName 知识库名称
     * @return 知识库ID，失败返回null
     */
    public String createKnowledgeBase(String filePath, String kbName) {
        String categoryId = "default";
        String parser = "DASHSCOPE_DOCMIND";
        String sourceType = "DATA_CENTER_FILE";
        String structureType = "unstructured";
        String sinkType = "DEFAULT";
        
        try {
            log.info("步骤1：初始化Client");
            
            log.info("步骤2：准备文件信息");
            File file = new File(filePath);
            String fileName = file.getName();
            String fileMd5 = calculateMD5(filePath);
            String fileSize = getFileSize(filePath);

            log.info("步骤3：申请上传租约");
            ApplyFileUploadLeaseResponse leaseResponse = applyLease(categoryId, fileName, fileMd5, fileSize, workspaceId);
            
            String leaseId = leaseResponse.getBody().getData().getFileUploadLeaseId();
            String uploadUrl = leaseResponse.getBody().getData().getParam().getUrl();
            Object uploadHeadersObj = leaseResponse.getBody().getData().getParam().getHeaders();

            @SuppressWarnings("unchecked")
            Map<String, String> uploadHeaders = objectMapper.readValue(
                    objectMapper.writeValueAsString(uploadHeadersObj), Map.class);

            log.info("步骤4：上传文件");
            uploadFile(uploadUrl, uploadHeaders, filePath);

            log.info("步骤5：添加文件到服务器");
            AddFileResponse addResponse = addFile(leaseId, parser, categoryId, workspaceId);
            String fileId = addResponse.getBody().getData().getFileId();

            log.info("步骤6：检查文件状态");
            while (true) {
                DescribeFileResponse describeResponse = describeFile(workspaceId, fileId);
                String status = describeResponse.getBody().getData().getStatus();
                log.info("文件状态: {}", status);

                if ("PARSE_SUCCESS".equals(status)) {
                    break;
                } else if ("INIT".equals(status) || "PARSING".equals(status)) {
                    Thread.sleep(5000);
                } else {
                    log.error("文件状态异常: {}", status);
                    return null;
                }
            }

            log.info("步骤7：创建知识库");
            CreateIndexResponse indexResponse = createIndex(workspaceId, fileId, kbName, 
                    structureType, sourceType, sinkType);
            String indexId = indexResponse.getBody().getData().getId();

            log.info("步骤8：提交索引任务");
            SubmitIndexJobResponse submitResponse = submitIndex(workspaceId, indexId);
            String jobId = submitResponse.getBody().getData().getId();

            log.info("步骤9：等待索引任务完成");
            while (true) {
                GetIndexJobStatusResponse statusResponse = getIndexJobStatus(workspaceId, jobId, indexId);
                String status = statusResponse.getBody().getData().getStatus();
                log.info("索引任务状态: {}", status);

                if ("COMPLETED".equals(status)) {
                    break;
                }
                Thread.sleep(5000);
            }

            log.info("知识库创建成功！ID: {}", indexId);
            return indexId;

        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return null;
        }
    }

    /**
     * 上传文本内容到知识库（先生成临时文件）
     */
    public String uploadTextToKnowledgeBase(String title, String content) {
        try {
            File tempFile = createTempFile(title, content);
            if (tempFile == null) {
                return null;
            }
            String kbName = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            return createKnowledgeBase(tempFile.getAbsolutePath(), kbName);
        } catch (Exception e) {
            log.error("上传文本到知识库失败", e);
            return null;
        }
    }

    /**
     * 生成HTML报告
     */
    private String generateHtmlReport(String title, String content) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>").append(title).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; line-height: 1.6; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }\n");
        html.append("h2 { color: #555; margin-top: 30px; }\n");
        html.append("p { text-indent: 2em; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #4CAF50; color: white; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<h1>").append(title).append("</h1>\n");
        html.append(content.replace("\n", "<br>\n"));
        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * 上传报告到知识库
     */
    public String uploadReportToKnowledgeBase(String reportTitle, String reportContent) {
        String htmlContent = generateHtmlReport(reportTitle, reportContent);
        return uploadTextToKnowledgeBase(reportTitle, htmlContent);
    }

    /**
     * 完整上传流程（包含知识库创建）
     */
    public String uploadFileToKnowledgeBase(String title, String content) {
        return uploadReportToKnowledgeBase(title, content);
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(String title, String content) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".html";
            File tempFile = new File(tempDir, fileName);

            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            return tempFile;
        } catch (IOException e) {
            log.error("创建临时文件失败", e);
            return null;
        }
    }

    /**
     * 计算文件MD5
     */
    private String calculateMD5(String filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 获取文件大小
     */
    private String getFileSize(String filePath) {
        File file = new File(filePath);
        return String.valueOf(file.length());
    }
}
