package site.minnan.rental.domain.vo;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.util.List;

/**
 * 楼层下拉框
 * @author Minnan on 2021/01/27
 */
@Data
public class FloorDropDown {

    private Integer houseId;

    private String houseName;

    private List<Integer> floorList;

    public FloorDropDown(Integer houseId, String houseName, List<Integer> floors){
        this.houseId = houseId;
        this.houseName = houseName;
        this.floorList = floors;
    }
}
