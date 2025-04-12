package top.flyingjack.auth.account.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Zumin Li
 * @date 2025/4/20 12:49
 */
public enum PrincipalType {
    @Schema(description = "用户名")
    USERNAME("username"),

    @Schema(description = "手机号")
    PHONE("phone"),

    @Schema(description = "邮件")
    EMAIL("email"),;

    private final String value;
    PrincipalType(String value) {
        this.value = value;
    }

    // 序列化时使用小写
    @JsonValue
    public String getValue() {
        return value;
    }

    // 反序列化时忽略大小写
    @JsonCreator
    public static PrincipalType fromValue(String value) {
        for (PrincipalType type : PrincipalType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown login type: " + value);
    }
}
