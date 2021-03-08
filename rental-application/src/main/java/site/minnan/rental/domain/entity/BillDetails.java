package site.minnan.rental.domain.entity;

import cn.hutool.core.math.MathUtil;
import lombok.Data;
import site.minnan.rental.domain.aggregate.Bill;
import site.minnan.rental.domain.aggregate.Utility;

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

    private static final BigDecimal WATER_MAX = new BigDecimal(1000);

    private static final BigDecimal ELECTRICITY_MAX = new BigDecimal(10000);

    public void settleWater(BigDecimal price) {
        BigDecimal waterEnd = this.waterEnd.compareTo(waterStart) < 0 ?
                this.waterEnd.add(WATER_MAX) :
                this.waterEnd;
        super.settleWater(waterStart, waterEnd, price);
    }

    public void settleElectricity(BigDecimal price) {
        BigDecimal electricityEnd = this.electricityEnd.compareTo(electricityStart) < 0 ?
                this.electricityEnd.add(ELECTRICITY_MAX) :
                this.electricityEnd;
        super.settleElectricity(electricityStart, electricityEnd, price);
    }

    public void setUtilityStart(Utility utilityStart) {
        this.waterStart = utilityStart.getWater();
        this.electricityStart = utilityStart.getElectricity();
    }

    public void setUtilityEnd(Utility utilityEnd) {
        this.waterEnd = utilityEnd.getWater();
        this.electricityEnd = utilityEnd.getElectricity();
    }
}
