package com.changan.common.constants;

import cn.hutool.core.lang.RegexPool;

public interface RegexConstants extends RegexPool {
    /**
     * 手机号正则
     */
    String PHONE_PATTERN = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";
    /**
     * 邮箱正则
     */
    String EMAIL_PATTERN = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    /**
     * 密码正则。6~32位的字母、数字、下划线
     */
    String PASSWORD_PATTERN = "^\\w{4,24}$";
    /**
     * 用户名正则。6~32位的字母、数字、下划线
     */
    String USERNAME_PATTERN = "^\\w{4,32}$";

    /**
     * 车牌号正则（支持普通车牌、新能源车牌、挂学警港澳）
     */
    String PLATE_NUMBER_PATTERN = "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁]" +
            "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$";
}
