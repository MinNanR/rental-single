package site.minnan.rental.application.provider.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.minnan.rental.application.provider.UtilityProviderService;
import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.mapper.UtilityMapper;
import site.minnan.rental.domain.vo.SettleQueryVO;
import site.minnan.rental.infrastructure.enumerate.UtilityStatus;
import site.minnan.rental.userinterface.dto.NewRoomUtilityDTO;
import site.minnan.rental.userinterface.dto.SettleQueryDTO;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UtilityProviderServiceImpl implements UtilityProviderService {

    @Autowired
    private UtilityMapper utilityMapper;


    /**
     * 查询水电
     *
     * @param dtoList
     * @return
     */
    @Override
    public Map<Integer, SettleQueryVO> getUtility(List<SettleQueryDTO> dtoList) {
        //结算时使用的水电单
        QueryWrapper<Utility> endQueryWrapper = new QueryWrapper<>();
        List<Integer> roomIds = dtoList.stream().map(SettleQueryDTO::getRoomId).collect(Collectors.toList());
        endQueryWrapper.select("id", "room_id", "water", "electricity", "create_time")
                .eq("status", UtilityStatus.RECORDING)
                .in("room_id", roomIds);
        List<Utility> endUtilityList = utilityMapper.selectList(endQueryWrapper);
        //账单开始时使用的水电单
        QueryWrapper<Utility> startQueryWrapper = new QueryWrapper<>();
        List<Integer> utilityIds = dtoList.stream().map(SettleQueryDTO::getStartUtilityId).collect(Collectors.toList());
        startQueryWrapper.select("id", "room_id", "water", "electricity", "create_time")
                .in("id", utilityIds);
        List<Utility> startUtilityList = utilityMapper.selectList(startQueryWrapper);
        return Stream.concat(endUtilityList.stream(), startUtilityList.stream())
                .collect(Collectors.groupingBy(Utility::getRoomId, Collectors.collectingAndThen(Collectors.toList(),
                        (e -> {
                            e.sort(Comparator.comparing(Utility::getCreateTime));
                            Utility start = e.get(0);
                            Utility end = e.get(1);
                            return new SettleQueryVO(new JSONObject(start), new JSONObject(end));
                        }))));

    }

    /**
     * 获取房间当前水电度数记录id
     *
     * @param roomId 房间id
     * @return 当前水电度数记录id
     */
    @Override
    public Integer getCurrentUtility(Integer roomId) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();;
        queryWrapper.select("id").eq("room_id", roomId).eq("status", UtilityStatus.RECORDING);
        Utility utility = utilityMapper.selectOne(queryWrapper);
        return utility.getId();
    }

    /**
     * 根据房间id获取当前水电行度
     *
     * @param houseId
     * @return key:房间id，value：当前行度
     */
    @Override
    public Map<Integer, Utility> getCurrentUtilityByHouse(Integer houseId) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("room_id", "water", "electricity")
                .eq("status", UtilityStatus.RECORDING)
                .eq("house_id", houseId);
        List<Utility> utilityList = utilityMapper.selectList(queryWrapper);
        return utilityList.stream().collect(Collectors.toMap(Utility::getRoomId, e -> e));
    }

    /**
     * 查询水电
     *
     * @param dto
     * @return
     */
    @Override
    public SettleQueryVO getUtility(SettleQueryDTO dto) {
        QueryWrapper<Utility> endQueryWrapper = new QueryWrapper<>();
        endQueryWrapper.select("id", "room_id", "water", "electricity", "create_time")
                .eq("status", UtilityStatus.RECORDING)
                .in("room_id", dto.getRoomId());
        Utility end = utilityMapper.selectOne(endQueryWrapper);
        QueryWrapper<Utility> startQueryWrapper = new QueryWrapper<>();
        startQueryWrapper.select("id", "room_id", "water", "electricity", "create_time")
                .eq("id", dto.getStartUtilityId());
        Utility start = utilityMapper.selectOne(startQueryWrapper);
        return new SettleQueryVO(new JSONObject(start), new JSONObject(end));
    }

    @Override
    public void addUtility(NewRoomUtilityDTO dto) {
        JwtUser user = dto.getUser();
        Timestamp current = new Timestamp(System.currentTimeMillis());
        Utility utility = Utility.builder()
                .houseId(dto.getHouseId())
                .houseName(dto.getHouseName())
                .roomId(dto.getRoomId())
                .roomNumber(dto.getRoomNumber())
                .water(dto.getWater())
                .electricity(dto.getElectricity())
                .status(UtilityStatus.RECORDING)
                .createUserId(user.getId())
                .createUserName(user.getRealName())
                .createTime(current)
                .updateUserId(user.getId())
                .updateUserName(user.getRealName())
                .updateTime(current)
                .build();
        utilityMapper.insert(utility);

    }
}
