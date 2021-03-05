package site.minnan.rental.userinterface.dto.bill;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 添加账单参数
 * @author Minnan on 2021/3/5
 */
@Data
public class FillBillDTO {

    private Integer billId;

    private Integer utilityStartId;

    private String startDate;

    private String endDate;

    private BigDecimal waterStart;

    private BigDecimal waterEnd;

    private BigDecimal electricityStart;

    private BigDecimal electricityEnd;
}
