package com.example.wx.elasticsearch.service;

import com.example.wx.elasticsearch.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.Cardinality;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * ES聚合查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchAggregationService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    static final String TEST_INDEX = "tests";
    static final String TEST_ITEM_INDEX = "test_items";

    /**
     * 1. 测试年龄分布
     */
    public AgeGroupDistributionVO getAgeGroupDistribution(String startDate, String endDate) {
        AgeGroupDistributionVO vo = new AgeGroupDistributionVO();
        List<AgeGroupDistributionVO.AgeGroupItem> items = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = buildDateRangeQuery(startDate, endDate);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(0);

            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_age_group").field("age")
                            .subAggregation(AggregationBuilders.max("max_score").field("totalScore"))
                            .subAggregation(AggregationBuilders.min("min_score").field("totalScore"))
                            .subAggregation(AggregationBuilders.avg("avg_score").field("totalScore"))
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Map<String, Long> ageGroupCount = new HashMap<>();
            Map<String, Double> ageGroupMaxScore = new HashMap<>();
            Map<String, Double> ageGroupMinScore = new HashMap<>();
            Map<String, Double> ageGroupSumScore = new HashMap<>();

            String[] ageGroupNames = {"<55", "55-60", "60-65", "65-70", ">=70"};
            for (String name : ageGroupNames) {
                ageGroupCount.put(name, 0L);
                ageGroupMaxScore.put(name, 0.0);
                ageGroupMinScore.put(name, 100.0);
                ageGroupSumScore.put(name, 0.0);
            }

            Aggregation ageAgg = response.getAggregations().get("by_age_group");
            if (ageAgg instanceof Terms) {
                Terms terms = (Terms) ageAgg;
                for (Terms.Bucket bucket : terms.getBuckets()) {
                    String ageStr = bucket.getKeyAsString();
                    Integer age = null;
                    try {
                        age = (int) Double.parseDouble(ageStr);
                    } catch (Exception e) {
                        continue;
                    }

                    String groupName = getAgeGroup(age);
                    long count = bucket.getDocCount();
                    
                    Max maxAgg = bucket.getAggregations().get("max_score");
                    Min minAgg = bucket.getAggregations().get("min_score");
                    Avg avgAgg = bucket.getAggregations().get("avg_score");
                    
                    double maxScore = maxAgg != null ? maxAgg.getValue() : 0.0;
                    double minScore = minAgg != null ? minAgg.getValue() : 0.0;
                    double avgScore = avgAgg != null ? avgAgg.getValue() : 0.0;

                    ageGroupCount.put(groupName, ageGroupCount.get(groupName) + count);
                    if (maxScore > ageGroupMaxScore.get(groupName)) {
                        ageGroupMaxScore.put(groupName, maxScore);
                    }
                    if (minScore > 0 && minScore < ageGroupMinScore.get(groupName)) {
                        ageGroupMinScore.put(groupName, minScore);
                    }
                    ageGroupSumScore.put(groupName, ageGroupSumScore.get(groupName) + avgScore * count);
                }
            }

            for (String name : ageGroupNames) {
                if (ageGroupCount.get(name) > 0) {
                    AgeGroupDistributionVO.AgeGroupItem item = new AgeGroupDistributionVO.AgeGroupItem();
                    item.setAgeGroup(name);
                    item.setCount(ageGroupCount.get(name));
                    item.setMaxScore(ageGroupMaxScore.get(name));
                    double minVal = ageGroupMinScore.get(name);
                    item.setMinScore(minVal < 100 ? minVal : 0.0);
                    item.setAvgScore(ageGroupSumScore.get(name) / ageGroupCount.get(name));
                    items.add(item);
                }
            }

        } catch (IOException e) {
            log.error("年龄分布查询失败", e);
        }

        vo.setData(items);
        return vo;
    }

    private String getAgeGroup(Integer age) {
        if (age == null) return ">=70";
        if (age < 55) return "<55";
        if (age < 60) return "55-60";
        if (age < 65) return "60-65";
        if (age < 70) return "65-70";
        return ">=70";
    }

    /**
     * 2. 月度测试趋势
     */
    public MonthlyTrendVO getMonthlyTrend(String startDate, String endDate) {
        MonthlyTrendVO vo = new MonthlyTrendVO();
        List<String> months = new ArrayList<>();
        List<Long> testCounts = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = buildDateRangeQuery(startDate, endDate);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(0);

            // testDate 是 keyword 类型，用 terms 聚合按月份字符串分组
            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_month")
                            .field("testDate")
                            .subAggregation(AggregationBuilders.avg("avg_score").field("totalScore"))
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation agg = response.getAggregations().get("by_month");
            if (agg instanceof Terms) {
                Terms terms = (Terms) agg;
                List<Terms.Bucket> bucketList = new ArrayList<>();
                terms.getBuckets().forEach(bucketList::add);
                bucketList.sort(Comparator.comparing(b -> b.getKeyAsString()));
                
                for (Terms.Bucket bucket : bucketList) {
                    String key = bucket.getKeyAsString();
                    if (key != null && key.length() >= 7) {
                        months.add(key.substring(0, 7));
                    } else {
                        months.add(key);
                    }
                    testCounts.add(bucket.getDocCount());
                    Avg avgAgg = bucket.getAggregations().get("avg_score");
                    avgScores.add(avgAgg != null ? avgAgg.getValue() : 0.0);
                }
            }

        } catch (IOException e) {
            log.error("月度趋势查询失败", e);
        }

        vo.setMonths(months);
        vo.setTestCounts(testCounts);
        vo.setAvgScores(avgScores);
        return vo;
    }

    /**
     * 3. 测试完成状态
     */
    public CompletionStatusVO getCompletionStatus(String startDate, String endDate) {
        CompletionStatusVO vo = new CompletionStatusVO();
        long completed = 0, inProgress = 0, notStarted = 0;

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(buildDateRangeQuery(startDate, endDate));
            sourceBuilder.size(0);

            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_status").field("completionStatus")
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation agg = response.getAggregations().get("by_status");
            if (agg instanceof Terms) {
                Terms terms = (Terms) agg;
                for (Terms.Bucket bucket : terms.getBuckets()) {
                    String status = bucket.getKeyAsString();
                    long count = bucket.getDocCount();
                    if ("completed".equals(status)) {
                        completed = count;
                    } else if ("in_progress".equals(status)) {
                        inProgress = count;
                    } else {
                        notStarted = count;
                    }
                }
            }

        } catch (IOException e) {
            log.error("完成状态查询失败", e);
        }

        vo.setCompleted(completed);
        vo.setInProgress(inProgress);
        vo.setNotStarted(notStarted);
        return vo;
    }

    /**
     * 4. 错误类型分布
     */
    public ErrorTypeDistributionVO getErrorTypeDistribution(String startDate, String endDate) {
        ErrorTypeDistributionVO vo = new ErrorTypeDistributionVO();
        List<ErrorTypeDistributionVO.ErrorTypeItem> items = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_ITEM_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(buildDateRangeQueryForItem(startDate, endDate));
            sourceBuilder.size(0);

            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_error_type").field("errorTags").size(20)
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation agg = response.getAggregations().get("by_error_type");
            if (agg instanceof Terms) {
                Terms terms = (Terms) agg;
                for (Terms.Bucket bucket : terms.getBuckets()) {
                    ErrorTypeDistributionVO.ErrorTypeItem item = new ErrorTypeDistributionVO.ErrorTypeItem();
                    item.setErrorType(bucket.getKeyAsString());
                    item.setCount(bucket.getDocCount());
                    items.add(item);
                }
            }

        } catch (IOException e) {
            log.error("错误类型查询失败", e);
        }

        vo.setData(items);
        return vo;
    }

    /**
     * 5. 医院统计
     */
    public HospitalStatsVO getHospitalStats(String startDate, String endDate) {
        HospitalStatsVO vo = new HospitalStatsVO();
        List<HospitalStatsVO.HospitalItem> items = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(buildDateRangeQuery(startDate, endDate));
            sourceBuilder.size(0);

            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_hospital").field("hospitalName")
                            .subAggregation(AggregationBuilders.cardinality("user_count").field("userId"))
                            .subAggregation(AggregationBuilders.avg("avg_score").field("totalScore"))
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation agg = response.getAggregations().get("by_hospital");
            if (agg instanceof Terms) {
                Terms terms = (Terms) agg;
                for (Terms.Bucket bucket : terms.getBuckets()) {
                    HospitalStatsVO.HospitalItem item = new HospitalStatsVO.HospitalItem();
                    item.setHospitalName(bucket.getKeyAsString());
                    item.setTestCount(bucket.getDocCount());

                    Cardinality cardinality = bucket.getAggregations().get("user_count");
                    item.setUserCount(cardinality != null ? cardinality.getValue() : 0);

                    Avg avgAgg = bucket.getAggregations().get("avg_score");
                    item.setAvgScore(avgAgg != null ? avgAgg.getValue() : 0.0);
                    items.add(item);
                }
            }

        } catch (IOException e) {
            log.error("医院统计查询失败", e);
        }

        vo.setData(items);
        return vo;
    }

    /**
     * 6. 得分分布
     */
    public ScoreDistributionVO getScoreDistribution(String startDate, String endDate) {
        ScoreDistributionVO vo = new ScoreDistributionVO();
        List<String> ranges = Arrays.asList("0-20", "20-40", "40-60", "60-80", "80-100");
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            counts.add(0L);
        }

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(buildDateRangeQuery(startDate, endDate));
            sourceBuilder.size(0);

            RangeAggregationBuilder rangeAgg = AggregationBuilders.range("score_range")
                    .field("totalScore");
            rangeAgg.addRange("0-20", 0, 20);
            rangeAgg.addRange("20-40", 20, 40);
            rangeAgg.addRange("40-60", 40, 60);
            rangeAgg.addRange("60-80", 60, 80);
            rangeAgg.addRange("80-100", 80, 100);

            sourceBuilder.aggregation(rangeAgg);

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation rangeAggResult = response.getAggregations().get("score_range");
            if (rangeAggResult != null) {
                try {
                    java.lang.reflect.Method getBucketsMethod = rangeAggResult.getClass().getMethod("getBuckets");
                    Iterable<?> buckets = (Iterable<?>) getBucketsMethod.invoke(rangeAggResult);
                    for (Object bucket : buckets) {
                        java.lang.reflect.Method getKeyMethod = bucket.getClass().getMethod("getKey");
                        java.lang.reflect.Method getDocCountMethod = bucket.getClass().getMethod("getDocCount");
                        String key = (String) getKeyMethod.invoke(bucket);
                        long count = (Long) getDocCountMethod.invoke(bucket);
                        int idx = ranges.indexOf(key);
                        if (idx >= 0) {
                            counts.set(idx, count);
                        }
                    }
                } catch (Exception e) {
                    log.warn("得分分布解析失败", e);
                }
            }

        } catch (IOException e) {
            log.error("得分分布查询失败", e);
        }

        vo.setRanges(ranges);
        vo.setCounts(counts);
        return vo;
    }

    /**
     * 7. 每日报表
     */
    public DailyReportVO getDailyReport(String startDate, String endDate) {
        DailyReportVO vo = new DailyReportVO();
        List<DailyReportVO.DailyReportItem> items = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(buildDateRangeQuery(startDate, endDate));
            sourceBuilder.size(0);

            // testDate 是 keyword 类型，用 terms 聚合按日期字符串分组
            sourceBuilder.aggregation(
                    AggregationBuilders.terms("by_date")
                            .field("testDate")
                            .subAggregation(AggregationBuilders.cardinality("user_count").field("userId"))
                            .subAggregation(AggregationBuilders.avg("avg_score").field("totalScore"))
                            .subAggregation(AggregationBuilders.terms("by_status").field("completionStatus"))
            );

            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Aggregation agg = response.getAggregations().get("by_date");
            if (agg instanceof Terms) {
                Terms terms = (Terms) agg;
                List<Terms.Bucket> bucketList = new ArrayList<>();
                terms.getBuckets().forEach(bucketList::add);
                bucketList.sort(Comparator.comparing(b -> b.getKeyAsString()));
                
                for (Terms.Bucket bucket : bucketList) {
                    DailyReportVO.DailyReportItem item = new DailyReportVO.DailyReportItem();
                    item.setDate(bucket.getKeyAsString());
                    item.setTestCount(bucket.getDocCount());
                    
                    Cardinality userCountAgg = bucket.getAggregations().get("user_count");
                    item.setUserCount(userCountAgg != null ? userCountAgg.getValue() : 0);
                    
                    Avg avgAgg = bucket.getAggregations().get("avg_score");
                    item.setAvgScore(avgAgg != null ? avgAgg.getValue() : 0.0);
                    
                    Terms statusTerms = bucket.getAggregations().get("by_status");
                    long completed = 0, total = item.getTestCount();
                    if (statusTerms != null) {
                        for (Terms.Bucket statusBucket : statusTerms.getBuckets()) {
                            if ("completed".equals(statusBucket.getKeyAsString())) {
                                completed = statusBucket.getDocCount();
                            }
                        }
                    }
                    item.setCompletionRate(total > 0 ? (double) completed / total * 100 : 0.0);
                    items.add(item);
                }
            }

        } catch (IOException e) {
            log.error("每日报表查询失败", e);
        }

        vo.setData(items);
        return vo;
    }

    /**
     * 8. 统计总计
     */
    public SummaryStatsVO getSummaryStats() {
        SummaryStatsVO vo = new SummaryStatsVO();

        try {
            // 总用户数
            SearchRequest userRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder userSource = new SearchSourceBuilder();
            userSource.size(0);
            userSource.aggregation(AggregationBuilders.cardinality("total_users").field("userId"));
            userRequest.source(userSource);
            SearchResponse userResponse = restHighLevelClient.search(userRequest, RequestOptions.DEFAULT);
            Cardinality cardinality = userResponse.getAggregations().get("total_users");
            vo.setTotalUsers(cardinality != null ? cardinality.getValue() : 0);

            // 总测试数
            SearchRequest countRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder countSource = new SearchSourceBuilder();
            countSource.size(0);
            countSource.query(QueryBuilders.matchAllQuery());
            countRequest.source(countSource);
            SearchResponse countResponse = restHighLevelClient.search(countRequest, RequestOptions.DEFAULT);
            vo.setTotalTests(countResponse.getHits().getTotalHits().value);

            // 平均分
            SearchRequest avgRequest = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder avgSource = new SearchSourceBuilder();
            avgSource.size(0);
            avgSource.aggregation(AggregationBuilders.avg("avg_score").field("totalScore"));
            avgRequest.source(avgSource);
            SearchResponse avgResponse = restHighLevelClient.search(avgRequest, RequestOptions.DEFAULT);
            Avg avgAgg = avgResponse.getAggregations().get("avg_score");
            vo.setAvgScore(avgAgg != null ? avgAgg.getValue() : 0.0);

            // 每日分数提升
            vo.setScoreImprovement(calculateDailyScoreImprovement());

        } catch (IOException e) {
            log.error("统计总计查询失败", e);
        }

        return vo;
    }

    private double calculateDailyScoreImprovement() {
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterday = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

            Double todayAvg = getDayAvgScore(today);
            Double yesterdayAvg = getDayAvgScore(yesterday);

            if (todayAvg != null && yesterdayAvg != null && yesterdayAvg > 0) {
                return todayAvg - yesterdayAvg;
            }
        } catch (Exception e) {
            log.error("计算每日分数提升失败", e);
        }
        return 0.0;
    }

    private Double getDayAvgScore(String date) {
        try {
            SearchRequest request = new SearchRequest(TEST_INDEX);
            SearchSourceBuilder source = new SearchSourceBuilder();
            source.size(0);
            source.query(termQuery("testDate", date));
            source.aggregation(AggregationBuilders.avg("avg_score").field("totalScore"));
            request.source(source);
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            Avg avgAgg = response.getAggregations().get("avg_score");
            return avgAgg != null ? avgAgg.getValue() : null;
        } catch (Exception e) {
            log.error("获取日均分失败", e);
        }
        return null;
    }

    private BoolQueryBuilder buildDateRangeQuery(String startDate, String endDate) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (startDate != null && !startDate.isEmpty()) {
            boolQuery.must(rangeQuery("testDate").gte(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            boolQuery.must(rangeQuery("testDate").lte(endDate));
        }
        return boolQuery;
    }

    private BoolQueryBuilder buildDateRangeQueryForItem(String startDate, String endDate) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (startDate != null && !startDate.isEmpty()) {
            boolQuery.must(rangeQuery("createdAt").gte(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            boolQuery.must(rangeQuery("createdAt").lte(endDate));
        }
        return boolQuery;
    }
}
