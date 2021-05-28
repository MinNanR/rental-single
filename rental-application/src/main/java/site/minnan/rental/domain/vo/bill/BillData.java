package site.minnan.rental.domain.vo.bill;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;

/**
 * 填写月度账单时所需要的数据
 * @author Minnan on 2021/03/04
 */
@Data
public class BillData {

    private Integer id;

    private Integer waterPrice;

    private Integer electricityPrice;

    private Integer waterStart;

    private Integer electricityStart;

    private Integer price;

    @JsonFormat(pattern = "M月dd日",timezone = "GMT+08:00")
    private Timestamp utilityStartDate;

    private String utilityStartId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+08:00")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+08:00")
    private Date endDate;
}
