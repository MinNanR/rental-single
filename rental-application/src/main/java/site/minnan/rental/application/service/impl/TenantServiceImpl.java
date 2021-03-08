package site.minnan.rental.application.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinEngine;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.rental.application.provider.BillProviderService;
import site.minnan.rental.application.provider.RoomProviderService;
import site.minnan.rental.application.provider.TenantProviderService;
import site.minnan.rental.application.provider.UtilityProviderService;
import site.minnan.rental.application.service.TenantService;
import site.minnan.rental.domain.aggregate.Tenant;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.mapper.TenantMapper;
import site.minnan.rental.domain.vo.*;
import site.minnan.rental.domain.vo.tenant.*;
import site.minnan.rental.infrastructure.enumerate.Gender;
import site.minnan.rental.infrastructure.enumerate.RoomStatus;
import site.minnan.rental.infrastructure.enumerate.TenantStatus;
import site.minnan.rental.infrastructure.exception.EntityNotExistException;
import site.minnan.rental.infrastructure.utils.RedisUtil;
import site.minnan.rental.userinterface.dto.*;
import site.minnan.rental.userinterface.dto.room.AllSurrenderDTO;
import site.minnan.rental.userinterface.dto.tenant.*;
import site.minnan.rental.userinterface.dto.utility.AddUtilityDTO;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TenantServiceImpl implements TenantService {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private PinyinEngine pinyinEngine;

    @Autowired
    private TenantProviderService tenantProviderService;

//    @Reference(check = false)
//    private UserProviderService userProviderService;

    @Autowired
    private RoomProviderService roomProviderService;

    @Autowired
    private BillProviderService billProviderService;

    @Autowired
    private UtilityProviderService utilityProviderService;

    /**
     * 添加房客
     *
     * @param dto
     */
    @Override
    @Transactional
    public void addTenant(RegisterAddTenantDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Tenant> newTenantList = new ArrayList<>();
        for (AddTenantDTO tenant : dto.getTenantList()) {
            //构建新房客对象
            String identificationNumber = tenant.getIdentificationNumber();
            DateTime birthday = IdcardUtil.getBirthDate(identificationNumber);
            int gender = IdcardUtil.getGenderByIdCard(identificationNumber);
            List<String> region = Optional.ofNullable((List<String>) redisUtil.getHashValue("region",
                    identificationNumber.substring(0, 6)))
                    .orElseGet(() -> CollectionUtil.newArrayList("", ""));
            Tenant newTenant = Tenant.builder()
                    .name(tenant.getName())
                    .gender(Gender.values()[gender])
                    .phone(tenant.getPhone())
                    .hometownProvince(region.get(0))
                    .hometownCity(region.get(1))
                    .identificationNumber(identificationNumber)
                    .birthday(birthday)
                    .houseId(dto.getHouseId())
                    .houseName(dto.getHouseName())
                    .roomId(dto.getRoomId())
                    .roomNumber(dto.getRoomNumber())
                    .status(TenantStatus.LIVING)
                    .build();
            newTenant.setCreateUser(jwtUser);
            newTenantList.add(newTenant);
        }
        addTenant(newTenantList);
//        //更新房屋状态
//        UpdateRoomStatusDTO updateRoomStatusDTO = UpdateRoomStatusDTO.builder()
//                .id(dto.getRoomId())
//                .status(RoomStatus.ON_RENT.getValue())
//                .userId(jwtUser.getId())
//                .userName(jwtUser.getRealName())
//                .build();
//        JSONObject room = roomProviderService.updateRoomStatus(updateRoomStatusDTO);
//        //检查房屋原始状态
//        if (RoomStatus.VACANT.getValue().equals(room.getStr("status"))) {
//            List<Integer> tenantIdList = newTenantList.stream().map(Tenant::getId).collect(Collectors.toList());
//            //原始状态为空闲则新增账单
//            CreateBillDTO createBillDTO = CreateBillDTO.builder()
//                    .roomId(dto.getRoomId())
//                    .tenantIdList(tenantIdList)
//                    .userId(jwtUser.getId())
//                    .userName(jwtUser.getRealName())
//                    .build();
//            billProviderService.createBill(createBillDTO);
//        }
    }

    /**
     * 列表查询房客
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<TenantVO> getTenantList(GetTenantListDTO dto) {
        QueryWrapper<Tenant> wrapper = new QueryWrapper<>();
        Optional.ofNullable(dto.getName()).ifPresent(s -> wrapper.like("name", s));
        Optional.ofNullable(dto.getHouseId()).ifPresent(s -> wrapper.eq("house_id", s));
        Optional.ofNullable(dto.getRoomNumber()).ifPresent(s -> wrapper.like("room_number", s));
        Optional.ofNullable(dto.getRoomId()).ifPresent(s -> wrapper.eq("room_id", s));
        Optional.ofNullable(dto.getHometownProvince()).ifPresent(s -> wrapper.eq("hometown_province", s));
        Optional.ofNullable(dto.getHometownCity()).ifPresent(s -> wrapper.eq("hometown_city", s));
        wrapper.ne("status", TenantStatus.DELETED.getValue());
        wrapper.orderByDesc("update_time");
        Page<Tenant> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        IPage<Tenant> page = tenantMapper.selectPage(queryPage, wrapper);
        List<TenantVO> voList = page.getRecords().stream().map(TenantVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(voList, page.getTotal());
    }

    /**
     * 查询该房间所住的房客
     *
     * @param dto
     * @return
     */
    @Override
    public List<TenantVO> getTenantByRoom(GetTenantByRoomDTO dto) {
        QueryWrapper<Tenant> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", dto.getRoomId())
                .eq("status", TenantStatus.LIVING);
        List<Tenant> tenantList = tenantMapper.selectList(wrapper);
        return tenantList.stream().map(TenantVO::assemble).collect(Collectors.toList());
    }

    /**
     * 查询房客详情
     *
     * @param dto
     * @return
     */
    @Override
    public TenantInfoVO getTenantInfo(DetailsQueryDTO dto) {
        Tenant tenant = tenantMapper.selectById(dto.getId());
        if (tenant == null) {
            throw new EntityNotExistException("房客不存在");
        }
        return TenantInfoVO.assemble(tenant);
    }

    /**
     * 获取房客下拉框
     *
     * @param dto
     * @return
     */
    @Override
    public List<TenantDropDownVO> getTenantDropDown(GetTenantDropDownDTO dto) {
        QueryWrapper<Tenant> wrapper = new QueryWrapper<>();
        wrapper.like("name", dto.getName());
        wrapper.ne("room_id", dto.getRoomId());
        wrapper.or().eq("status", TenantStatus.LEFT);
        List<Tenant> tenantList = tenantMapper.selectList(wrapper);
        return tenantList.stream().map(TenantDropDownVO::assemble).collect(Collectors.toList());
    }

    /**
     * 房客迁移房间
     *
     * @param dto
     */
    @Override
    public void tenantMove(TenantMoveDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer checkRoomOnRent = tenantMapper.checkRoomOnRentByRoomId(dto.getRoomId());
        List<Integer> tenantIdList = dto.getTenantIdList();
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", tenantIdList);
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        Set<Integer> roomIdList =
                tenantList.stream().map(Tenant::getRoomId).collect(Collectors.toSet());
        UpdateWrapper<Tenant> updateWrapper = new UpdateWrapper<>();

        updateWrapper.set("house_id", dto.getHouseId())
                .set("house_name", dto.getHouseName())
                .set("room_id", dto.getRoomId())
                .set("room_number", dto.getRoomNumber())
                .set("status", TenantStatus.LIVING)
                .set("update_user_id", jwtUser.getId())
                .set("update_user_name", jwtUser.getRealName())
                .set("update_time", new Timestamp(System.currentTimeMillis()))
                .in("id", tenantIdList);
        tenantMapper.update(null, updateWrapper);

        //检查房客原房间是否仍有人居住，无人居住则将房间状态改为空闲
        List<UpdateRoomStatusDTO> updateRoomStatusDTOList = new ArrayList<>();
        for (Integer id : roomIdList) {
            Integer check = tenantMapper.checkRoomOnRentByRoomId(id);
            if (check == null) {
                UpdateRoomStatusDTO roomStatusDTO = UpdateRoomStatusDTO.builder()
                        .id(id)
                        .status(RoomStatus.VACANT.getValue())
                        .userId(jwtUser.getId())
                        .userName(jwtUser.getRealName())
                        .build();
                updateRoomStatusDTOList.add(roomStatusDTO);
            }
        }
        //当前房间修改为在租
        UpdateRoomStatusDTO roomStatusDTO =
                UpdateRoomStatusDTO.builder()
                        .id(dto.getRoomId())
                        .status(RoomStatus.ON_RENT.getValue())
                        .userId(jwtUser.getId())
                        .userName(jwtUser.getRealName())
                        .build();
        updateRoomStatusDTOList.add(roomStatusDTO);
        roomProviderService.updateRoomStatusBatch(updateRoomStatusDTOList);

        //检查迁移的房客是否有离开再租的房客
        List<Integer> leftUserList = tenantList.stream()
                .filter(e -> TenantStatus.LEFT.equals(e.getStatus()))
                .map(Tenant::getUserId)
                .collect(Collectors.toList());
//        if (CollectionUtil.isNotEmpty(leftUserList)) {
//            EnableTenantUserBatchDTO enableTenantUserBatchDTO = EnableTenantUserBatchDTO.builder()
//                    .userIdList(leftUserList)
//                    .userId(jwtUser.getId())
//                    .userName(jwtUser.getRealName())
//                    .build();
//            userProviderService.enableTenantUserBatch(enableTenantUserBatchDTO);
//        }

//        //房间原本为空闲状态则创建账单
//        if (checkRoomOnRent == null) {
//            CreateBillDTO createBillDTO = CreateBillDTO.builder()
//                    .roomId(dto.getRoomId())
//                    .userId(jwtUser.getId())
//                    .userName(jwtUser.getRealName())
//                    .build();
//            billProviderService.createBill(createBillDTO);
//        }
    }

    /**
     * 修改房客信息
     *
     * @param dto
     */
    @Override
    public void updateTenant(UpdateTenantDTO dto) {
        Tenant tenant = tenantMapper.selectById(dto.getId());
        if (tenant == null) {
            throw new EntityNotExistException("房客不存在");
        }
        JwtUser user = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UpdateWrapper<Tenant> wrapper = new UpdateWrapper<>();
        Optional.ofNullable(dto.getName()).ifPresent(s -> wrapper.set("name", s));
        Optional.ofNullable(dto.getPhone()).ifPresent(s -> wrapper.set("phone", s));
        if (dto.getIdentificationNumber() != null) {
            String identificationNumber = dto.getIdentificationNumber();
            DateTime birthday = IdcardUtil.getBirthDate(identificationNumber);
            int gender = IdcardUtil.getGenderByIdCard(identificationNumber);
            List<String> region = Optional.ofNullable((List<String>) redisUtil.getHashValue("region",
                    identificationNumber.substring(0, 6)))
                    .orElseGet(() -> CollectionUtil.newArrayList("", ""));
            wrapper.set("identification_number", identificationNumber)
                    .set("birthday", birthday)
                    .set("gender", Gender.values()[gender])
                    .set("hometown_province", region.get(0))
                    .set("hometown_city", region.get(1));
        }
        wrapper.set("update_user_id", user.getId());
        wrapper.set("update_user_name", user.getRealName());
        wrapper.set("update_time", new Timestamp(System.currentTimeMillis()));
        wrapper.eq("id", dto.getId());
        tenantMapper.update(null, wrapper);
    }

    /**
     * 房客退租
     *
     * @param dto
     */
    @Override
    public void surrender(SurrenderDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Tenant tenant = tenantMapper.selectById(dto.getId());
        if (tenant == null) {
            throw new EntityNotExistException("房客不存在");
        }
        UpdateWrapper<Tenant> wrapper = new UpdateWrapper<>();
        wrapper.set("status", TenantStatus.LEFT)
                .set("update_user_id", jwtUser.getId())
                .set("update_user_name", jwtUser.getRealName())
                .set("update_time", new Timestamp(System.currentTimeMillis()))
                .eq("id", dto.getId());
        tenantMapper.update(null, wrapper);
        Integer check = tenantMapper.checkRoomOnRentByRoomId(tenant.getRoomId());
        if (check == null) {
            UpdateRoomStatusDTO updateRoomStatusDTO = UpdateRoomStatusDTO.builder()
                    .id(tenant.getRoomId())
                    .status(RoomStatus.VACANT.getValue())
                    .userId(jwtUser.getId())
                    .userName(jwtUser.getRealName())
                    .build();
            roomProviderService.updateRoomStatus(updateRoomStatusDTO);
            billProviderService.completeBillWithSurrender(tenant.getRoomId());
        }
//        DisableTenantUserDTO disableTenantUserDTO = DisableTenantUserDTO.builder()
//                .userId(tenant.getUserId())
//                .updateUserId(jwtUser.getId())
//                .updateUserName(jwtUser.getRealName())
//                .build();
//        userProviderService.disableTenantUser(disableTenantUserDTO);
    }

    /**
     * 检查身份证号码是否存在
     *
     * @param dto
     * @return
     */
    @Override
    public Boolean checkIdentificationNumberExist(CheckIdentificationNumberDTO dto) {
        Integer id = tenantMapper.checkTenantExistByIdentificationNumber(dto.getIdentificationNumber());
        return id != null;
    }


    /**
     * 获取房客列表
     *
     * @return
     */
    @Override
    public List<TenantPinyinVO> getTenantList() {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "house_name", "room_number", "phone")
                .eq("status", TenantStatus.LIVING);
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        Map<Character, List<TenantAppVO>> groupByPinyin = tenantList.stream()
                .collect(Collectors.groupingBy(e -> pinyinEngine.getFirstLetter(e.getName().charAt(0)),
                        Collectors.collectingAndThen(Collectors.toList(), (e -> e.stream()
                                .sorted(Comparator.comparing(t1 -> pinyinEngine.getPinyin(t1.getName(), "")))
                                .map(TenantAppVO::assemble)
                                .collect(Collectors.toList())))));
        return groupByPinyin.entrySet().stream()
                .map(e -> new TenantPinyinVO((char) (e.getKey() - 32), e.getValue()))
                .sorted(Comparator.comparing(TenantPinyinVO::getKey))
                .collect(Collectors.toList());
    }

    /**
     * 全部退租
     *
     * @param dto
     */
    @Override
    @Transactional
    public void surrenderAll(AllSurrenderDTO dto) {
        List<Tenant> tenantList = tenantMapper.selectBatchIds(dto.getIdList());
        if (CollectionUtil.isEmpty(tenantList) || tenantList.size() < dto.getIdList().size()) {
            throw new EntityNotExistException("房客不存在");
        }
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UpdateWrapper<Tenant> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", TenantStatus.LEFT)
                .set("update_user_id", jwtUser.getId())
                .set("update_user_name", jwtUser.getRealName())
                .set("update_time", new Timestamp(System.currentTimeMillis()))
                .in("id", dto.getIdList());
        tenantMapper.update(null, updateWrapper);
        Integer roomId = tenantList.get(0).getRoomId();
        UpdateRoomStatusDTO updateRoomStatusDTO = UpdateRoomStatusDTO.builder()
                .id(roomId)
                .status(RoomStatus.VACANT.getValue())
                .userId(jwtUser.getId())
                .userName(jwtUser.getRealName())
                .build();
        roomProviderService.updateRoomStatus(updateRoomStatusDTO);
        billProviderService.completeBillWithSurrender(roomId);
        List<Integer> userIdList = tenantList.stream().map(Tenant::getUserId).collect(Collectors.toList());
//        BatchDisableUserDTO batchDisableUserDTO = new BatchDisableUserDTO(jwtUser.getId(), jwtUser.getRealName(),
//                userIdList);
//        userProviderService.disableTenantUserBatch(batchDisableUserDTO);
    }

    /**
     * 登记入住
     *
     * @param dto
     */
    @Override
    @Transactional
    public void checkIn(CheckInDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Tenant> newTenantList = new ArrayList<>();
        for (AddTenantDTO tenant : dto.getTenantList()) {
            //构建新房客对象
            Tenant.TenantBuilder tenantBuilder = Tenant.builder()
                    .name(tenant.getName())
                    .phone(tenant.getPhone())
                    .houseId(dto.getHouseId())
                    .houseName(dto.getHouseName())
                    .roomId(dto.getRoomId())
                    .roomNumber(dto.getRoomNumber())
                    .status(TenantStatus.LIVING);

            if (StrUtil.isNotBlank(tenant.getIdentificationNumber())) {
                String identificationNumber = tenant.getIdentificationNumber();
                DateTime birthday = IdcardUtil.getBirthDate(identificationNumber);
                int gender = IdcardUtil.getGenderByIdCard(identificationNumber);
                List<String> region = Optional.ofNullable((List<String>) redisUtil.getHashValue("region",
                        identificationNumber.substring(0, 6)))
                        .orElseGet(() -> CollectionUtil.newArrayList("", ""));
                tenantBuilder.gender(Gender.values()[gender])
                        .hometownProvince(region.get(0))
                        .hometownCity(region.get(1))
                        .identificationNumber(identificationNumber)
                        .birthday(birthday);
            }
            Tenant newTenant = tenantBuilder.build();
            newTenant.setCreateUser(jwtUser);
            newTenantList.add(newTenant);
        }
        addTenant(newTenantList);

        //更新房间状态
        UpdateRoomStatusDTO updateRoomStatusDTO = UpdateRoomStatusDTO.builder()
                .id(dto.getRoomId())
                .status(RoomStatus.ON_RENT.getValue())
                .userId(jwtUser.getId())
                .userName(jwtUser.getRealName())
                .build();
        roomProviderService.updateRoomStatus(updateRoomStatusDTO);

        //如果填写了水电则更新水电
        if (dto.getWater() != null || dto.getElectricity() != null) {
            AddUtilityDTO addUtilityDTO = AddUtilityDTO.builder()
                    .roomId(dto.getRoomId())
                    .water(dto.getWater())
                    .electricity(dto.getElectricity())
                    .build();
            utilityProviderService.updateUtility(addUtilityDTO);
        }

        //添加账单
        List<Integer> tenantIdList = newTenantList.stream().map(Tenant::getId).collect(Collectors.toList());
        CreateBillDTO createBillDTO = CreateBillDTO.builder()
                .roomId(dto.getRoomId())
                .cardQuantity(dto.getCardQuantity())
                .deposit(dto.getDeposit())
                .remark(dto.getRemark())
                .checkInDate(dto.getCheckInDate())
                .payMethod(dto.getPayMethod())
                .tenantIdList(tenantIdList)
                .userId(jwtUser.getId())
                .userName(jwtUser.getRealName())
                .build();
        billProviderService.createBill(createBillDTO);
    }

    /**
     * 房客用户获取基本信息
     *
     * @return
     */
    @Override
    public TenantBaseInfoVO getTenantBaseInfo() {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Tenant tenant = tenantProviderService.getTenantByUserId(jwtUser.getId());
        String name = tenant.getName();
        TenantBaseInfoVO vo;
        if (StrUtil.isNotBlank(name)) {
            char firstLetter = pinyinEngine.getFirstLetter(name.charAt(0));
            vo = new TenantBaseInfoVO(tenant, String.valueOf(firstLetter).toUpperCase());
        } else {
            vo = new TenantBaseInfoVO(tenant, "");
        }
        return vo;
    }


    /**
     * 添加房客
     *
     * @param tenantList
     */
    private void addTenant(List<Tenant> tenantList) {
        //TODO 暂时不创建租客用户，完成用户端小程序时放开注释

//        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        List<AddTenantUserDTO> addUserDTO = new ArrayList<>();
//        for (Tenant tenant : tenantList) {
//            //添加房客用户
//            AddTenantUserDTO tenantUserDTO = AddTenantUserDTO.builder()
//                    .phone(tenant.getPhone())
//                    .realName(tenant.getName())
//                    .userId(jwtUser.getId())
//                    .userName(jwtUser.getRealName())
//                    .build();
//            addUserDTO.add(tenantUserDTO);
//        }
//        List<Integer> userIdList = userProviderService.createTenantUserBatch(addUserDTO);
//        Iterator<Integer> idIterator = userIdList.iterator();
        tenantMapper.addTenantBatch(tenantList);
    }
}
