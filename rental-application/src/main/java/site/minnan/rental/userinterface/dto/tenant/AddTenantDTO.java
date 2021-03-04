package site.minnan.rental.userinterface.dto.tenant;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * 添加房客参数
 *
 * @author Minnan on 2020/12/28
 */
@Data
public class AddTenantDTO {

    @NotEmpty(message = "房客姓名不能为空")
    private String name;

    @Pattern(regexp = "^1([3456789])\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20|(3\\d))\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号码格式不正确")
    private String identificationNumber;
}
