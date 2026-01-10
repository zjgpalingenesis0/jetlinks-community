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

package org.jetlinks.community.device.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 设备统计缓存工具类
 * <p>
 * 提供设备统计数据的本地缓存功能,使用 Caffeine 缓存框架
 * <p>
 * 缓存策略:
 * <ul>
 *   <li>统计数据缓存: 5分钟过期,适合短期重复查询</li>
 *   <li>趋势数据缓存: 3分钟过期,数据量大避免内存占用</li>
 *   <li>缓存Key: 基于设备ID和查询参数生成</li>
 * </ul>
 *
 * @author JetLinks
 * @since 1.0
 */
@Slf4j
public class DeviceStatsCacheUtils {

    /**
     * 统计数据缓存
     * <p>
     * 缓存策略:
     * <ul>
     *   <li>基于写入时间过期: 5分钟</li>
     *   <li>最大缓存数量: 1000条</li>
     *   <li>软引用: 内存不足时自动释放</li>
     * </ul>
     */
    private static final Cache<String, Mono<Map<String, Object>>> STATS_CACHE = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(1000)
        .softValues()
        .build();

    /**
     * 趋势数据缓存
     * <p>
     * 缓存策略:
     * <ul>
     *   <li>基于写入时间过期: 3分钟</li>
     *   <li>最大缓存数量: 500条(数据量较大)</li>
     *   <li>软引用: 内存不足时自动释放</li>
     * </ul>
     */
    private static final Cache<String, Mono<Map<String, Object>>> TRENDS_CACHE = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(3))
        .maximumSize(500)
        .softValues()
        .build();

    /**
     * 生成统计数据缓存Key
     *
     * @param deviceId 设备ID
     * @param minutes  时间段(分钟)
     * @return 缓存Key
     */
    public static String getStatsCacheKey(String deviceId, int minutes) {
        return String.format("stats:%s:%d", deviceId, minutes);
    }

    /**
     * 生成趋势数据缓存Key
     *
     * @param deviceId 设备ID
     * @param interval 时间间隔(分钟)
     * @param hours    时间范围(小时)
     * @return 缓存Key
     */
    public static String getTrendsCacheKey(String deviceId, int interval, int hours) {
        return String.format("trends:%s:%d:%d", deviceId, interval, hours);
    }

    /**
     * 获取或加载统计数据
     * <p>
     * 如果缓存中存在,则直接返回缓存的数据
     * 如果缓存中不存在,则通过loader加载数据并缓存
     *
     * @param deviceId 设备ID
     * @param minutes  时间段(分钟)
     * @param loader   数据加载函数
     * @return 统计数据的Mono
     */
    public static Mono<Map<String, Object>> getStatsOrLoad(
        String deviceId,
        int minutes,
        Function<String, Mono<Map<String, Object>>> loader) {

        String cacheKey = getStatsCacheKey(deviceId, minutes);

        return Mono.defer(() -> {
            Mono<Map<String, Object>> cached = STATS_CACHE.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("Stats cache hit for key: {}", cacheKey);
                return cached;
            }

            log.debug("Stats cache miss for key: {}, loading data...", cacheKey);
            Mono<Map<String, Object>> loaded = loader.apply(cacheKey)
                .cache(
                    val -> Duration.ofMinutes(5),  // 成功时缓存5分钟
                    err -> Duration.ZERO,           // 失败时立即过期
                    () -> Duration.ofMinutes(2)    // 完成时缓存2分钟
                );

            STATS_CACHE.put(cacheKey, loaded);
            return loaded;
        });
    }

    /**
     * 获取或加载趋势数据
     * <p>
     * 如果缓存中存在,则直接返回缓存的数据
     * 如果缓存中不存在,则通过loader加载数据并缓存
     *
     * @param deviceId 设备ID
     * @param interval 时间间隔(分钟)
     * @param hours    时间范围(小时)
     * @param loader   数据加载函数
     * @return 趋势数据的Mono
     */
    public static Mono<Map<String, Object>> getTrendsOrLoad(
        String deviceId,
        int interval,
        int hours,
        Function<String, Mono<Map<String, Object>>> loader) {

        String cacheKey = getTrendsCacheKey(deviceId, interval, hours);

        return Mono.defer(() -> {
            Mono<Map<String, Object>> cached = TRENDS_CACHE.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("Trends cache hit for key: {}", cacheKey);
                return cached;
            }

            log.debug("Trends cache miss for key: {}, loading data...", cacheKey);
            Mono<Map<String, Object>> loaded = loader.apply(cacheKey)
                .cache(
                    val -> Duration.ofMinutes(3),  // 成功时缓存3分钟
                    err -> Duration.ZERO,           // 失败时立即过期
                    () -> Duration.ofMinutes(1)    // 完成时缓存1分钟
                );

            TRENDS_CACHE.put(cacheKey, loaded);
            return loaded;
        });
    }

    /**
     * 清除指定设备的所有缓存
     *
     * @param deviceId 设备ID
     */
    public static void evictDevice(String deviceId) {
        log.debug("Evicting cache for device: {}", deviceId);
        STATS_CACHE.asMap().entrySet().removeIf(entry -> entry.getKey().contains("stats:" + deviceId));
        TRENDS_CACHE.asMap().entrySet().removeIf(entry -> entry.getKey().contains("trends:" + deviceId));
    }

    /**
     * 清除所有缓存
     */
    public static void evictAll() {
        log.debug("Evicting all cache");
        STATS_CACHE.invalidateAll();
        TRENDS_CACHE.invalidateAll();
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public static Map<String, Object> getCacheStats() {
        return Map.of(
            "statsCacheSize", STATS_CACHE.estimatedSize(),
            "trendsCacheSize", TRENDS_CACHE.estimatedSize(),
            "statsCacheStats", STATS_CACHE.stats(),
            "trendsCacheStats", TRENDS_CACHE.stats()
        );
    }
}
