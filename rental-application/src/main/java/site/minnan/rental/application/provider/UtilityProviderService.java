package site.minnan.rental.application.provider;

import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.vo.SettleQueryVO;
import site.minnan.rental.userinterface.dto.NewRoomUtilityDTO;
import site.minnan.rental.userinterface.dto.SettleQueryDTO;
import site.minnan.rental.userinterface.dto.utility.AddUtilityDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 水电服务
 *
 * @author Minnan on 2021/1/22
 */
public interface UtilityProviderService {


    /**
     * 获取房间当前水电度数记录id
     *
     * @param roomId 房间id
     * @return 当前水电度数记录id
     */
    Integer getCurrentUtility(Integer roomId);

    /**
     * 根据房屋id获取当前水电行度
     *
     * @param houseId 房屋id
     * @return key:房间id，value：当前行度
     */
    Map<Integer, Utility> getCurrentUtilityByHouse(Integer houseId);

    /**
     * 获取水电记录，获取前检查是否需要修改
     *
     * @param utilityId   水电记录id
     * @param water       水表行度
     * @param electricity 电表行度
     * @return
     */
    Utility getOrUpdateUtility(Integer utilityId, BigDecimal water, BigDecimal electricity);

    /**
     * 添加水电记录
     *
     * @param dto
     * @return
     */
    Utility addUtility(AddUtilityDTO dto);

    /**
     * 添加房间时添加水电记录
     *
     * @param dto
     */
    void addUtility(NewRoomUtilityDTO dto);

    /**
     * 登记入住时登记水电
     *
     * @param dto
     */
    void updateUtility(AddUtilityDTO dto);
}
