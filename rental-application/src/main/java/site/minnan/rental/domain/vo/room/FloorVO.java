package site.minnan.rental.domain.vo.room;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 每层楼的房间列表
 * @author Minnan on 2021/1/21
 */
@Data
@AllArgsConstructor
public class FloorVO {

    private Integer floor;

    private List<RoomInfoVO> roomList;
}
