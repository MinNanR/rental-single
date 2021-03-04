package site.minnan.rental.domain.vo.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.minnan.rental.domain.aggregate.Room;
import site.minnan.rental.domain.aggregate.Utility;

import java.math.BigDecimal;

/**
 * @author Minnan on 2020/12/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomInfoVO {

    private Integer id;

    private Integer houseId;

    private String houseName;

    private String roomNumber;

    private Integer floor;

    private Integer price;

    private Integer deposit;

    private String status;

    private String statusCode;

    private BigDecimal water;

    private BigDecimal electricity;

    public static RoomInfoVO assemble(Room room){
        return RoomInfoVO.builder()
                .id(room.getId())
                .houseId(room.getHouseId())
                .houseName(room.getHouseName())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .price(room.getPrice())
                .status(room.getStatus().getStatus())
                .statusCode(room.getStatus().getValue())
                .build();
    }

    /**
     * 设置水电
     * @param utility 水电行度
     */
    public void setUtility(Utility utility){
        this.water = utility.getWater();
        this.electricity = utility.getElectricity();
    }
}
