package site.minnan.rental.domain.entity;

import cn.hutool.core.math.MathUtil;
import lombok.Data;
import site.minnan.rental.domain.aggregate.Bill;

import java.math.BigDecimal;

/**
 * 账单所有信息
 *
 * @author Minnan on 2021/01/30
 */
@Data
public class BillDetails extends Bill {

    private BigDecimal waterStart;

    private BigDecimal waterEnd;

    private BigDecimal electricityStart;

    private BigDecimal electricityEnd;

    public void settleWater(BigDecimal price) {
        BigDecimal waterEnd = this.waterEnd.compareTo(waterStart) < 0 ?
                this.waterEnd.add(BigDecimal.valueOf(1000)) :
                this.waterEnd;
        super.settleWater(waterStart, waterEnd, price);
    }

    public void settleElectricity(BigDecimal price) {
        BigDecimal electricityEnd = this.electricityEnd.compareTo(electricityStart) < 0 ?
                this.electricityEnd.add(BigDecimal.valueOf(1000)) :
                this.electricityEnd;
        super.settleElectricity(electricityStart, electricityEnd, price);
    }
}
