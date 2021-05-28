package site.minnan.rental.domain.vo.bill;

import cn.hutool.core.util.NumberUtil;
import lombok.Data;
import site.minnan.rental.domain.vo.house.HouseDropDown;

import java.math.BigDecimal;

/**
 * 月度总额
 *
 */
@Data
public class MonthTotalVO {

    private String houseName;

    private String monthTotal;

    public MonthTotalVO(HouseDropDown house, BigDecimal total){
        this.houseName = house.getHouseName();
        this.monthTotal = NumberUtil.decimalFormat(",###", total);
    }
}
