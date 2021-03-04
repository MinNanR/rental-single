package site.minnan.rental.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录后返回的信息
 *
 * @author Minnan on 2020/12/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginVO {

    private String token;

    private String role;

    public LoginVO(String token) {
        this.token = token;
    }
}
