package site.minnan.rental.userinterface.dto.tenant;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 登记入住参数
 * @author Minnan on 2021/2/1
 */
@Data
public class CheckInDTO {

    private Integer houseId;

    private String houseName;

    private Integer roomId;

    private String roomNumber;

    private Integer deposit;

    private Integer cardQuantity;

    private String checkInDate;

    private String payMethod;

    private String remark;

    private BigDecimal water;

    private BigDecimal electricity;

    List<AddTenantDTO> tenantList;
}
