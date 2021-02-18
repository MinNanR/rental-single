package site.minnan.rental.userinterface.dto;

import lombok.Builder;
import lombok.Data;
import site.minnan.rental.domain.entity.JwtUser;

import java.math.BigDecimal;

/**
 * 添加房间时添加新的水电记录
 * @author Minnan on 2021/02/17
 */
@Data
@Builder
public class NewRoomUtilityDTO {

    private Integer houseId;

    private String houseName;

    private Integer roomId;

    private String roomNumber;

    private BigDecimal water;

    private BigDecimal electricity;

    private JwtUser user;

}
