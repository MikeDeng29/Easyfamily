package com.easyfamily.report.service;

import com.easyfamily.query.service.QueryRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ReportReadService {

    private final QueryRecordService queryRecordService;
    private final ReportMetricStoreService metricStoreService;
    private final boolean readFromMetricTable;
    private final boolean backfillFromMemoryOnEmpty;

    public ReportReadService(
            QueryRecordService queryRecordService,
            ReportMetricStoreService metricStoreService,
            @Value("${easyfamily.report.read-from-metric-table:true}") boolean readFromMetricTable,
            @Value("${easyfamily.report.backfill-from-memory-on-empty:true}") boolean backfillFromMemoryOnEmpty
    ) {
        this.queryRecordService = queryRecordService;
        this.metricStoreService = metricStoreService;
        this.readFromMetricTable = readFromMetricTable;
        this.backfillFromMemoryOnEmpty = backfillFromMemoryOnEmpty;
    }

    public Map<LocalDate, Integer> dauSnapshot() {
        if (!readFromMetricTable) {
            return queryRecordService.dauSnapshot();
        }
        ensureMetricDataAvailability();
        metricStoreService.refreshDailyMetrics(LocalDate.now());
        Map<LocalDate, Integer> rows = metricStoreService.loadDauSnapshot();
        return rows.isEmpty() ? queryRecordService.dauSnapshot() : rows;
    }

    public Map<String, Integer> featurePvSnapshot() {
        if (!readFromMetricTable) {
            return queryRecordService.featurePvSnapshot();
        }
        ensureMetricDataAvailability();
        metricStoreService.refreshDailyMetrics(LocalDate.now());
        Map<String, Integer> rows = metricStoreService.loadFeaturePvSnapshot();
        return rows.isEmpty() ? queryRecordService.featurePvSnapshot() : rows;
    }

    public ReportMetricStoreService.OverviewMetrics overviewTotals() {
        if (!readFromMetricTable) {
            return new ReportMetricStoreService.OverviewMetrics(
                    queryRecordService.totalQueryCount(),
                    queryRecordService.cacheHitCount()
            );
        }
        ensureMetricDataAvailability();
        metricStoreService.refreshDailyMetrics(LocalDate.now());
        ReportMetricStoreService.OverviewMetrics metrics = metricStoreService.loadOverviewTotals();
        if (metrics.totalQueryCount() == 0 && queryRecordService.totalQueryCount() > 0) {
            return new ReportMetricStoreService.OverviewMetrics(
                    queryRecordService.totalQueryCount(),
                    queryRecordService.cacheHitCount()
            );
        }
        return metrics;
    }

    private void ensureMetricDataAvailability() {
        if (!backfillFromMemoryOnEmpty || metricStoreService.hasMetricData()) {
            return;
        }
        metricStoreService.backfillFromMemorySnapshots(
                queryRecordService.dauSnapshot(),
                queryRecordService.featurePvSnapshot(),
                queryRecordService.totalQueryCount(),
                queryRecordService.cacheHitCount()
        );
    }
}
