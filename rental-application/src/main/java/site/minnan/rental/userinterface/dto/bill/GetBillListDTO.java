package site.minnan.rental.userinterface.dto.bill;

import lombok.Data;
import site.minnan.rental.infrastructure.enumerate.BillStatus;
import site.minnan.rental.userinterface.dto.ListQueryDTO;

/**
 * 获取账单列表参数
 * @author Minnan on 2021/1/12
 */
@Data
public class GetBillListDTO extends ListQueryDTO {

    private Integer houseId;

    private String roomNumber;

    private Integer roomId;

    private Integer year;

    private Integer month;

    private BillStatus status;
}
