package org.jetlinks.community.device.web;

import org.hswebframework.web.authorization.annotation.QueryAction;
import org.jetlinks.community.device.service.DeviceStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 设备统计接口
 * <p>
 * 提供设备数据的统计分析功能,包括:
 * <ul>
 *   <li>设备统计数据查询(最大值、最小值、平均值等)</li>
 *   <li>设备历史趋势数据查询</li>
 *   <li>批量查询多个设备的统计数据(优化后的新功能)</li>
 * </ul>
 * <p>
 * 架构说明:
 * <ul>
 *   <li>Controller层: 负责接收HTTP请求,参数验证,返回响应</li>
 *   <li>Service层(DeviceStatsService): 负责业务逻辑处理,数据计算</li>
 *   <li>Manager层(ThingsDataManager): 负责数据访问</li>
 * </ul>
 * <p>
 * 性能优化(Week 05):
 * <ul>
 *   <li>实现本地缓存机制(Caffeine),减少重复查询</li>
 *   <li>支持批量查询,并行处理多个设备</li>
 *   <li>统计数据缓存5分钟,趋势数据缓存3分钟</li>
 *   <li>批量查询并发度为16(统计)和8(趋势)</li>
 * </ul>
 * <p>
 * 符合分层架构原则,Controller不直接访问数据库,所有业务逻辑都在Service层处理
 *
 * @author JetLinks
 * @since 1.0
 */
@RestController
@RequestMapping("/device/stats")
public class DeviceStatsController {

    @Autowired
    private DeviceStatsService deviceStatsService;

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
     * 性能优化:
     * <ul>
     *   <li>使用 Caffeine 本地缓存,缓存5分钟</li>
     *   <li>相同设备和时间范围的重复查询直接返回缓存数据</li>
     * </ul>
     *
     * @param deviceId 设备ID
     * @param minutes  时间段(分钟),默认10分钟,最大1440分钟(24小时)
     * @return 统计数据,包含设备ID、时间范围和统计结果
     * @apiNote GET /device/stats/{deviceId}/summary?minutes=10
     */
    @GetMapping("/{deviceId}/summary")
    @QueryAction
    public Mono<Map<String, Object>> getDeviceStatsSummary(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "10") int minutes) {

        return deviceStatsService.getDeviceStatsSummary(deviceId, minutes);
    }

    /**
     * 查询设备的历史趋势数据
     * <p>
     * 按指定时间间隔对数据进行分桶统计,计算每个桶的平均值
     * <p>
     * 性能优化:
     * <ul>
     *   <li>使用 Caffeine 本地缓存,缓存3分钟</li>
     *   <li>相同设备和时间范围的重复查询直接返回缓存数据</li>
     * </ul>
     *
     * @param deviceId 设备ID
     * @param interval 时间间隔(分钟),默认10分钟,最大60分钟
     * @param hours    查询时间范围(小时),默认24小时,最大168小时(7天)
     * @return 趋势数据,包含时间序列和每个时间点的统计值
     * @apiNote GET /device/stats/{deviceId}/trends?interval=10&hours=24
     */
    @GetMapping("/{deviceId}/trends")
    @QueryAction
    public Mono<Map<String, Object>> getDeviceTrends(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "10") int interval,
            @RequestParam(defaultValue = "24") int hours) {

        return deviceStatsService.getDeviceTrends(deviceId, interval, hours);
    }

    /**
     * 批量查询多个设备的统计数据
     * <p>
     * 性能优化:
     * <ul>
     *   <li>并行查询多个设备,并发度为16</li>
     *   <li>利用缓存机制,相同设备的查询命中缓存</li>
     *   <li>单个设备查询失败不影响其他设备</li>
     * </ul>
     * <p>
     * 使用场景:
     * <ul>
     *   <li>设备列表页需要显示多个设备的统计数据</li>
     *   <li>仪表盘需要展示多个设备的关键指标</li>
     * </ul>
     *
     * @param deviceIds 设备ID列表,通过查询参数或请求体传递
     * @param minutes   时间段(分钟),默认10分钟,最大1440分钟(24小时)
     * @return 设备统计数据列表
     * @apiNote POST /device/stats/_batch/summary
     * <pre>
     * {
     *   "deviceIds": ["device1", "device2", "device3"],
     *   "minutes": 10
     * }
     * </pre>
     */
    @PostMapping("/_batch/summary")
    @QueryAction
    public Flux<Map<String, Object>> getDeviceStatsSummaryBatch(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> deviceIds = (List<String>) request.get("deviceIds");
        Integer minutes = (Integer) request.getOrDefault("minutes", 10);

        return deviceStatsService.getDeviceStatsSummaryBatch(deviceIds, minutes);
    }

    /**
     * 批量查询多个设备的趋势数据
     * <p>
     * 性能优化:
     * <ul>
     *   <li>并行查询多个设备,并发度为8</li>
     *   <li>利用缓存机制,相同设备的查询命中缓存</li>
     *   <li>单个设备查询失败不影响其他设备</li>
     * </ul>
     * <p>
     * 使用场景:
     * <ul>
     *   <li>对比多个设备的历史趋势</li>
     *   <li>设备组监控页面</li>
     * </ul>
     *
     * @param request 请求体,包含设备ID列表、时间间隔和查询范围
     * @return 设备趋势数据列表
     * @apiNote POST /device/stats/_batch/trends
     * <pre>
     * {
     *   "deviceIds": ["device1", "device2", "device3"],
     *   "interval": 10,
     *   "hours": 24
     * }
     * </pre>
     */
    @PostMapping("/_batch/trends")
    @QueryAction
    public Flux<Map<String, Object>> getDeviceTrendsBatch(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> deviceIds = (List<String>) request.get("deviceIds");
        Integer interval = (Integer) request.getOrDefault("interval", 10);
        Integer hours = (Integer) request.getOrDefault("hours", 24);

        return deviceStatsService.getDeviceTrendsBatch(deviceIds, interval, hours);
    }
}
