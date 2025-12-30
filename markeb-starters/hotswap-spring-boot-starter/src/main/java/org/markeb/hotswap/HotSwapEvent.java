package org.markeb.hotswap;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 热更新事件
 * <p>
 * 在热更新完成后发布，可用于触发缓存刷新等操作。
 * </p>
 */
public class HotSwapEvent extends ApplicationEvent {

    private final List<HotSwapResult> results;
    private final String targetVersion;

    public HotSwapEvent(Object source, List<HotSwapResult> results, String targetVersion) {
        super(source);
        this.results = results;
        this.targetVersion = targetVersion;
    }

    /**
     * 获取热更新结果列表
     */
    public List<HotSwapResult> getResults() {
        return results;
    }

    /**
     * 获取目标版本
     */
    public String getTargetVersion() {
        return targetVersion;
    }

    /**
     * 获取成功数量
     */
    public long getSuccessCount() {
        return results.stream().filter(HotSwapResult::isSuccess).count();
    }

    /**
     * 获取失败数量
     */
    public long getFailureCount() {
        return results.stream().filter(r -> !r.isSuccess()).count();
    }

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return results.stream().allMatch(HotSwapResult::isSuccess);
    }

    /**
     * 检查指定类是否被热更新
     */
    public boolean isClassReloaded(String className) {
        return results.stream()
                .anyMatch(r -> r.getClassName().equals(className) && r.isSuccess());
    }

}

