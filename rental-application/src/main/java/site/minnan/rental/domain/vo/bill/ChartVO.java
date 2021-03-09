package site.minnan.rental.domain.vo.bill;

import cn.hutool.core.date.DateUtil;
import lombok.Builder;
import lombok.Data;
import site.minnan.rental.domain.aggregate.Bill;

import java.math.BigDecimal;

@Data
@Builder
public class ChartVO {

    private BigDecimal water;

    private BigDecimal electricity;

    private String time;

    public static ChartVO assemble(Bill bill){
        return ChartVO.builder()
                .water(bill.getWaterUsage())
                .electricity(bill.getElectricityUsage())
                .time(DateUtil.format(bill.getStartDate(), "M月"))
                .build();
    }
}
