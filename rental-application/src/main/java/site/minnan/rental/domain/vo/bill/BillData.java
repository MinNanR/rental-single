package site.minnan.rental.domain.vo.bill;

import lombok.Data;

/**
 * 填写月度账单时所需要的数据
 * @author Minnan on 2021/03/04
 */
@Data
public class BillData {

    private Integer waterPrice;

    private Integer electricityPrice;

    private Integer waterStart;

    private Integer electricityStart;

    private Integer price;

    private String utilityStartDate;

    private String utilityStartId;
}
