package site.minnan.rental.application.provider.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.rental.application.provider.UtilityProviderService;
import site.minnan.rental.application.service.UtilityService;
import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.mapper.UtilityMapper;
import site.minnan.rental.infrastructure.enumerate.UtilityStatus;
import site.minnan.rental.userinterface.dto.NewRoomUtilityDTO;
import site.minnan.rental.userinterface.dto.utility.AddUtilityDTO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UtilityProviderServiceImpl implements UtilityProviderService {

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private UtilityService utilityService;

    /**
     * 获取房间当前水电度数记录id
     *
     * @param roomId 房间id
     * @return 当前水电度数记录id
     */
    @Override
    public Integer getCurrentUtility(Integer roomId) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        ;
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

    /**
     * 获取水电记录，获取前检查是否需要修改
     *
     * @param utilityId   水电记录id
     * @param water       水表行度
     * @param electricity 电表行度
     * @return
     */
    @Override
    @Transactional
    public Utility getOrUpdateUtility(Integer utilityId, BigDecimal water, BigDecimal electricity) {
        Utility utility = utilityMapper.selectById(utilityId);
        if (utility.getWater().compareTo(water) != 0 ||
                utility.getElectricity().compareTo(electricity) != 0) {
            JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            utility.setWater(water);
            utility.setElectricity(electricity);
            utility.setUpdateUser(jwtUser);
            utilityMapper.updateById(utility);
        }
        return utility;
    }

    @Override
    @Transactional
    public Utility addUtility(AddUtilityDTO dto) {
        return utilityService.addUtility(dto);
    }

    /**
     * 登记入住时登记水电
     *
     * @param dto
     */
    @Override
    public void updateUtility(AddUtilityDTO dto) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", dto.getRoomId())
                .eq("status", UtilityStatus.RECORDING);
        Utility utility = utilityMapper.selectOne(queryWrapper);
        Optional.ofNullable(dto.getWater()).ifPresent(utility::setWater);
        Optional.ofNullable(dto.getElectricity()).ifPresent(utility::setElectricity);
        utilityMapper.updateById(utility);
    }
}
