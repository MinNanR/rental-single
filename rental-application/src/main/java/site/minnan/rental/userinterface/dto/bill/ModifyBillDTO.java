package site.minnan.rental.userinterface.dto.bill;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 修改账单数据参数
 *
 * @author Minnan on 2021/3/8
 */
@Data
public class ModifyBillDTO {

    @NotNull(message = "未指定账单")
    private Integer billId;

    private BigDecimal waterStart;

    private BigDecimal waterEnd;

    private BigDecimal electricityStart;

    private BigDecimal electricityEnd;

    private String startDate;

    private String endDate;
}
