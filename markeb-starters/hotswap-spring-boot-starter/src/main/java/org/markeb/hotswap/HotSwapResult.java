package org.markeb.hotswap;

import lombok.Data;

/**
 * 热更新结果
 */
@Data
public class HotSwapResult {

    /**
     * 类名
     */
    private String className;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    private long costMs;

    public static HotSwapResult success(String className) {
        HotSwapResult result = new HotSwapResult();
        result.setClassName(className);
        result.setSuccess(true);
        return result;
    }

    public static HotSwapResult failure(String className, String errorMessage) {
        HotSwapResult result = new HotSwapResult();
        result.setClassName(className);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

}

