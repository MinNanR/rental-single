package site.minnan.rental.userinterface.dto;

import lombok.Data;
import site.minnan.rental.domain.vo.ListQueryVO;

import java.awt.*;

/**
 * 管理平台账单列表查询参数
 * @author Minnan on 2021/02/18
 */
@Data
public class GetBillsDTO extends ListQueryDTO {

    private Integer houseId;

    private String roomNumber;

    private String status;
}
