package site.minnan.rental.userinterface.dto.user;

import lombok.Data;
import site.minnan.rental.userinterface.dto.ListQueryDTO;

/**
 * 查询用户列表参数
 * @author Minnan on 2020/12/16
 */
@Data
public class GetUserListDTO extends ListQueryDTO {

    private String realName;

    private String phoneNumber;
}
