package site.minnan.rental.domain.vo.room;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class RentingInfoVO {

    /**
     * 押金
     */
    private Integer deposit;

    /**
     * 入住日期
     */
    private Timestamp checkInDate;
}
