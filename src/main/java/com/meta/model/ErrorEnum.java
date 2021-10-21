package com.meta.model;

public enum ErrorEnum {

    //-----------------------------------------------------------------------------------------------------------------
    // 客户端异常，“ C ”开头
    //-----------------------------------------------------------------------------------------------------------------
    网络异常("C-00001", "Network anomaly"),
    Token过期或已失效("C-00002", "The token has expired or become invalid"),
    没有文件操作权限("C-00003", "没有文件操作权限"),
    不是文件夹类型("C-00004", "不是文件夹类型"),
    文件已删除("C-00005", "文件已删除"),
    文件不存在("C-00006", "文件不存在"),
    文件不存在有效地址("C-00007", "文件不存在有效地址"),
    下载失败("C-00008", "下载失败"),
    //-----------------------------------------------------------------------------------------------------------------
    // 服务端异常，“ B ”开头
    //-----------------------------------------------------------------------------------------------------------------
    参数不正确("B-00001", "Parameter error"),
    //-----------------------------------------------------------------------------------------------------------------
    // 其它异常（第三方服务调用失败），“ O ”开头
    //-----------------------------------------------------------------------------------------------------------------
    第三方调用失败("O-00001", "Third party request failed"),
    ;

    public final String code;

    public final String message;

    ErrorEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String messageOf(String message) {
        for (ErrorEnum e : ErrorEnum.values()) {
            if (e.message.equalsIgnoreCase(message)) {
                return e.code;
            }
        }
        return "B-00000";
    }
}
