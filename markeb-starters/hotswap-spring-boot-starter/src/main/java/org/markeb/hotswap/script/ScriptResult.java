package org.markeb.hotswap.script;

import lombok.Data;

/**
 * 脚本执行结果
 */
@Data
public class ScriptResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 返回值
     */
    private Object result;

    /**
     * 返回值类型
     */
    private String resultType;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long costMs;

    public static ScriptResult success(Object result, long costMs) {
        ScriptResult r = new ScriptResult();
        r.setSuccess(true);
        r.setResult(result);
        r.setResultType(result != null ? result.getClass().getName() : "null");
        r.setCostMs(costMs);
        return r;
    }

    public static ScriptResult failure(String errorMessage, long costMs) {
        ScriptResult r = new ScriptResult();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        r.setCostMs(costMs);
        return r;
    }

}

