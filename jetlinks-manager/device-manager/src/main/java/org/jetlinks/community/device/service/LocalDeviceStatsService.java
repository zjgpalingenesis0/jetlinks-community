/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.utils.DeviceStatsCacheUtils;
import org.jetlinks.core.device.DeviceThingType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备统计服务实现
 * <p>
 * 负责处理设备数据的统计和趋势查询业务逻辑
 * <p>
 * 职责说明:
 * <ul>
 *   <li>接收Controller层的请求参数</li>
 *   <li>调用DeviceDataService查询设备数据</li>
 *   <li>处理和计算统计数据</li>
 *   <li>返回格式化的结果</li>
 * </ul>
 *
 * @author JetLinks
 * @since 1.0
 */
@Service
@Slf4j
public class LocalDeviceStatsService implements DeviceStatsService {

    private final DeviceDataService deviceDataService;

    public LocalDeviceStatsService(DeviceDataService deviceDataService) {
        this.deviceDataService = deviceDataService;
    }

    @Override
    public Mono<Map<String, Object>> getDeviceStatsSummary(String deviceId, int minutes) {
        // 参数验证
        if (minutes <= 0 || minutes > 1440) {
            return Mono.error(new IllegalArgumentException(
                "minutes must be between 1 and 1440"
            ));
        }

        // 使用缓存获取统计数据
        return DeviceStatsCacheUtils.getStatsOrLoad(deviceId, minutes, cacheKey -> {
            // 计算时间范围
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (minutes * 60 * 1000L);

            // 构建查询参数
            QueryParamEntity queryParam = new QueryParamEntity();
            queryParam.setPageIndex(0);
            queryParam.setPageSize(10000);
            // 设置时间范围和设备ID条件
            queryParam.toNestQuery(q -> q
                .gte("timestamp", new Date(startTime))
                .lte("timestamp", new Date(endTime))
                .is("deviceId", deviceId)
            );

            log.debug("Querying device stats: deviceId={}, startTime={}, endTime={}",
                     deviceId, new Date(startTime), new Date(endTime));

            // 查询设备属性数据并计算统计值
            return deviceDataService
                .queryProperty(deviceId, queryParam, "temperature", "humidity")
                .collectList()
                .map(dataList -> calculateStatsSummary(deviceId, startTime, endTime, minutes, dataList))
                .doOnError(error -> log.error("Error querying device stats for device: {}",
                                             deviceId, error));
        });
    }

    /**
     * 计算统计数据摘要
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param minutes   时间段(分钟)
     * @param dataList  设备属性数据列表
     * @return 统计数据摘要
     */
    private Map<String, Object> calculateStatsSummary(String deviceId,
                                                      long startTime,
                                                      long endTime,
                                                      int minutes,
                                                      List<DeviceProperty> dataList) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (dataList.isEmpty()) {
            log.debug("No data found for device: {} in time range", deviceId);
            result.put("deviceId", deviceId);
            result.put("message", "No data found in the specified time range");
            result.put("timeRange", Map.of(
                "start", startTime,
                "end", endTime,
                "minutes", minutes
            ));
            return result;
        }

        log.debug("Found {} data points for device: {}", dataList.size(), deviceId);

        // 计算统计数据
        DoubleSummaryStatistics tempStats = dataList.stream()
            .filter(prop -> "temperature".equals(prop.getProperty()))
            .mapToDouble(prop -> {
                Object value = prop.getValue();
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.NaN;
            })
            .filter(v -> !Double.isNaN(v))
            .summaryStatistics();

        DoubleSummaryStatistics humidityStats = dataList.stream()
            .filter(prop -> "humidity".equals(prop.getProperty()))
            .mapToDouble(prop -> {
                Object value = prop.getValue();
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.NaN;
            })
            .filter(v -> !Double.isNaN(v))
            .summaryStatistics();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("deviceId", deviceId);
        stats.put("timeRange", Map.of(
            "start", startTime,
            "end", endTime,
            "minutes", minutes
        ));

        Map<String, Object> properties = new LinkedHashMap<>();
        if (tempStats.getCount() > 0) {
            properties.put("temperature", Map.of(
                "max", tempStats.getMax(),
                "min", tempStats.getMin(),
                "avg", tempStats.getAverage(),
                "count", tempStats.getCount()
            ));
        }

        if (humidityStats.getCount() > 0) {
            properties.put("humidity", Map.of(
                "max", humidityStats.getMax(),
                "min", humidityStats.getMin(),
                "avg", humidityStats.getAverage(),
                "count", humidityStats.getCount()
            ));
        }

        properties.put("dataPoints", dataList.size());
        stats.put("stats", properties);

        return stats;
    }

    @Override
    public Mono<Map<String, Object>> getDeviceTrends(String deviceId, int interval, int hours) {
        // 参数验证
        if (interval <= 0 || interval > 60) {
            return Mono.error(new IllegalArgumentException(
                "interval must be between 1 and 60"
            ));
        }
        if (hours <= 0 || hours > 168) {
            return Mono.error(new IllegalArgumentException(
                "hours must be between 1 and 168"
            ));
        }

        // 使用缓存获取趋势数据
        return DeviceStatsCacheUtils.getTrendsOrLoad(deviceId, interval, hours, cacheKey -> {
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (hours * 60 * 60 * 1000L);

            QueryParamEntity queryParam = new QueryParamEntity();
            queryParam.setPageIndex(0);
            queryParam.setPageSize(100000);
            // 设置时间范围和设备ID条件
            queryParam.toNestQuery(q -> q
                .gte("timestamp", new Date(startTime))
                .lte("timestamp", new Date(endTime))
                .is("deviceId", deviceId)
            );

            log.debug("Querying device trends: deviceId={}, interval={}min, hours={}",
                     deviceId, interval, hours);

            return deviceDataService
                .queryProperty(deviceId, queryParam, "temperature", "humidity")
                .collectList()
                .map(dataList -> calculateTrendsData(deviceId, interval, hours, dataList))
                .doOnError(error -> log.error("Error querying device trends for device: {}",
                                             deviceId, error));
        });
    }

    /**
     * 计算趋势数据
     *
     * @param deviceId 设备ID
     * @param interval 时间间隔(分钟)
     * @param hours    时间范围(小时)
     * @param dataList 设备属性数据列表
     * @return 趋势数据
     */
    private Map<String, Object> calculateTrendsData(String deviceId,
                                                     int interval,
                                                     int hours,
                                                     List<DeviceProperty> dataList) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (dataList.isEmpty()) {
            log.debug("No trend data found for device: {}", deviceId);
            result.put("deviceId", deviceId);
            result.put("message", "No data found in the specified time range");
            result.put("interval", interval);
            result.put("hours", hours);
            return result;
        }

        log.debug("Found {} trend data points for device: {}", dataList.size(), deviceId);

        // 按时间间隔分桶
        long bucketSize = interval * 60 * 1000L;
        Map<Long, List<DeviceProperty>> buckets = dataList.stream()
            .collect(Collectors.groupingBy(data -> {
                Long timestamp = data.getTimestamp();
                return (timestamp / bucketSize) * bucketSize;
            }));

        log.debug("Grouped data into {} buckets", buckets.size());

        // 计算每个桶的平均值
        List<Map<String, Object>> trendData = buckets.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                long bucketTime = entry.getKey();
                List<DeviceProperty> bucketData = entry.getValue();

                DoubleSummaryStatistics tempStats = bucketData.stream()
                    .filter(prop -> "temperature".equals(prop.getProperty()))
                    .mapToDouble(prop -> {
                        Object value = prop.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        }
                        return Double.NaN;
                    })
                    .filter(v -> !Double.isNaN(v))
                    .summaryStatistics();

                DoubleSummaryStatistics humidityStats = bucketData.stream()
                    .filter(prop -> "humidity".equals(prop.getProperty()))
                    .mapToDouble(prop -> {
                        Object value = prop.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        }
                        return Double.NaN;
                    })
                    .filter(v -> !Double.isNaN(v))
                    .summaryStatistics();

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("timestamp", bucketTime);
                point.put("dataPoints", bucketData.size());

                if (tempStats.getCount() > 0) {
                    point.put("avgTemperature", tempStats.getAverage());
                    point.put("maxTemperature", tempStats.getMax());
                    point.put("minTemperature", tempStats.getMin());
                }

                if (humidityStats.getCount() > 0) {
                    point.put("avgHumidity", humidityStats.getAverage());
                    point.put("maxHumidity", humidityStats.getMax());
                    point.put("minHumidity", humidityStats.getMin());
                }

                return point;
            })
            .collect(Collectors.toList());

        result.put("deviceId", deviceId);
        result.put("interval", interval);
        result.put("hours", hours);
        result.put("data", trendData);
        result.put("totalBuckets", trendData.size());

        return result;
    }

    /**
     * 批量获取多个设备的统计数据摘要
     * <p>
     * 优化点:
     * <ul>
     *   <li>并行查询多个设备的数据</li>
     *   <li>利用缓存机制,相同设备的查询会命中缓存</li>
     *   <li>使用 Reactor 的并行操作提升性能</li>
     * </ul>
     *
     * @param deviceIds 设备ID列表
     * @param minutes   时间段(分钟)
     * @return 设备统计数据列表
     */
    public Flux<Map<String, Object>> getDeviceStatsSummaryBatch(List<String> deviceIds, int minutes) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Flux.empty();
        }

        // 参数验证
        if (minutes <= 0 || minutes > 1440) {
            return Flux.error(new IllegalArgumentException(
                "minutes must be between 1 and 1440"
            ));
        }

        log.debug("Batch querying device stats for {} devices, minutes={}", deviceIds.size(), minutes);

        // 并行查询多个设备的统计数据
        return Flux.fromIterable(deviceIds)
            .flatMap(deviceId ->
                getDeviceStatsSummary(deviceId, minutes)
                    .onErrorResume(error -> {
                        log.error("Error querying stats for device: {}", deviceId, error);
                        // 返回错误信息,不影响其他设备的查询
                        return Mono.just(Map.of(
                            "deviceId", deviceId,
                            "error", error.getMessage()
                        ));
                    }),
                16  // 并发度为16,同时查询16个设备
            );
    }

    /**
     * 批量获取多个设备的趋势数据
     * <p>
     * 优化点:
     * <ul>
     *   <li>并行查询多个设备的趋势数据</li>
     *   <li>利用缓存机制,相同设备的查询会命中缓存</li>
     *   <li>使用 Reactor 的并行操作提升性能</li>
     * </ul>
     *
     * @param deviceIds 设备ID列表
     * @param interval  时间间隔(分钟)
     * @param hours     时间范围(小时)
     * @return 设备趋势数据列表
     */
    public Flux<Map<String, Object>> getDeviceTrendsBatch(List<String> deviceIds, int interval, int hours) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Flux.empty();
        }

        // 参数验证
        if (interval <= 0 || interval > 60) {
            return Flux.error(new IllegalArgumentException(
                "interval must be between 1 and 60"
            ));
        }
        if (hours <= 0 || hours > 168) {
            return Flux.error(new IllegalArgumentException(
                "hours must be between 1 and 168"
            ));
        }

        log.debug("Batch querying device trends for {} devices, interval={}min, hours={}",
                 deviceIds.size(), interval, hours);

        // 并行查询多个设备的趋势数据
        return Flux.fromIterable(deviceIds)
            .flatMap(deviceId ->
                getDeviceTrends(deviceId, interval, hours)
                    .onErrorResume(error -> {
                        log.error("Error querying trends for device: {}", deviceId, error);
                        // 返回错误信息,不影响其他设备的查询
                        return Mono.just(Map.of(
                            "deviceId", deviceId,
                            "error", error.getMessage()
                        ));
                    }),
                8   // 趋势数据查询数据量大,并发度设为8
            );
    }
}
