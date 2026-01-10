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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 设备统计服务接口
 * <p>
 * 提供设备数据的统计分析功能,包括:
 * <ul>
 *   <li>设备统计数据查询(最大值、最小值、平均值等)</li>
 *   <li>设备历史趋势数据查询</li>
 *   <li>批量查询多个设备的统计数据(性能优化)</li>
 * </ul>
 * <p>
 * 架构说明:
 * <ul>
 *   <li>Controller层: 负责接收HTTP请求,参数验证,返回响应</li>
 *   <li>Service层: 负责业务逻辑处理,数据计算</li>
 *   <li>Repository/Manager层: 负责数据访问(ThingsDataManager)</li>
 * </ul>
 * <p>
 * 性能优化(Week 05):
 * <ul>
 *   <li>实现本地缓存机制,减少重复查询</li>
 *   <li>支持批量查询,并行处理多个设备</li>
 * </ul>
 *
 * @author JetLinks
 * @since 1.0
 */
public interface DeviceStatsService {

    /**
     * 查询设备在指定时间段内的统计数据
     * <p>
     * 统计数据包括:
     * <ul>
     *   <li>最大值(max)</li>
     *   <li>最小值(min)</li>
     *   <li>平均值(avg)</li>
     *   <li>数据点数量(count)</li>
     * </ul>
     * <p>
     * 性能优化: 使用本地缓存,相同设备的重复查询命中缓存后响应时间显著降低
     *
     * @param deviceId 设备ID
     * @param minutes  时间段(分钟)
     * @return 统计数据,包含设备ID、时间范围和统计结果
     */
    Mono<Map<String, Object>> getDeviceStatsSummary(String deviceId, int minutes);

    /**
     * 查询设备的历史趋势数据
     * <p>
     * 按指定时间间隔对数据进行分桶统计,计算每个桶的平均值
     * <p>
     * 性能优化: 使用本地缓存,相同设备的重复查询命中缓存后响应时间显著降低
     *
     * @param deviceId 设备ID
     * @param interval 时间间隔(分钟)
     * @param hours    查询时间范围(小时)
     * @return 趋势数据,包含时间序列和每个时间点的统计值
     */
    Mono<Map<String, Object>> getDeviceTrends(String deviceId, int interval, int hours);

    /**
     * 批量查询多个设备的统计数据
     * <p>
     * 性能优化:
     * <ul>
     *   <li>并行查询多个设备,并发度为16</li>
     *   <li>利用缓存机制,相同设备的查询命中缓存</li>
     *   <li>单个设备查询失败不影响其他设备</li>
     * </ul>
     *
     * @param deviceIds 设备ID列表
     * @param minutes   时间段(分钟)
     * @return 设备统计数据列表
     */
    Flux<Map<String, Object>> getDeviceStatsSummaryBatch(List<String> deviceIds, int minutes);

    /**
     * 批量查询多个设备的趋势数据
     * <p>
     * 性能优化:
     * <ul>
     *   <li>并行查询多个设备,并发度为8</li>
     *   <li>利用缓存机制,相同设备的查询命中缓存</li>
     *   <li>单个设备查询失败不影响其他设备</li>
     * </ul>
     *
     * @param deviceIds 设备ID列表
     * @param interval  时间间隔(分钟)
     * @param hours     时间范围(小时)
     * @return 设备趋势数据列表
     */
    Flux<Map<String, Object>> getDeviceTrendsBatch(List<String> deviceIds, int interval, int hours);
}
