package site.minnan.rental.application.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.rental.application.provider.RoomProviderService;
import site.minnan.rental.application.provider.TenantProviderService;
import site.minnan.rental.application.provider.UtilityProviderService;
import site.minnan.rental.application.service.BillService;
import site.minnan.rental.domain.aggregate.Bill;
import site.minnan.rental.domain.aggregate.Room;
import site.minnan.rental.domain.aggregate.Tenant;
import site.minnan.rental.domain.entity.BillDetails;
import site.minnan.rental.domain.entity.BillTenantEntity;
import site.minnan.rental.domain.entity.BillTenantRelevance;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.mapper.BillMapper;
import site.minnan.rental.domain.mapper.BillTenantRelevanceMapper;
import site.minnan.rental.domain.vo.bill.BillInfoVO;
import site.minnan.rental.domain.vo.bill.BillVO;
import site.minnan.rental.domain.vo.ListQueryVO;
import site.minnan.rental.domain.vo.utility.UtilityPrice;
import site.minnan.rental.infrastructure.enumerate.BillStatus;
import site.minnan.rental.infrastructure.enumerate.BillType;
import site.minnan.rental.infrastructure.enumerate.PaymentMethod;
import site.minnan.rental.infrastructure.enumerate.RoomStatus;
import site.minnan.rental.infrastructure.exception.EntityNotExistException;
import site.minnan.rental.infrastructure.exception.UnmodifiableException;
import site.minnan.rental.infrastructure.utils.ReceiptUtils;
import site.minnan.rental.infrastructure.utils.RedisUtil;
import site.minnan.rental.userinterface.dto.*;
import site.minnan.rental.userinterface.dto.bill.BillPaidDTO;
import site.minnan.rental.userinterface.dto.bill.GetBillListDTO;
import site.minnan.rental.userinterface.dto.bill.GetBillsDTO;
import site.minnan.rental.userinterface.dto.bill.SettleBillDTO;
import site.minnan.rental.userinterface.dto.utility.SetUtilityPriceDTO;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BillServiceImpl implements BillService {

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ReceiptUtils receiptUtils;

    @Autowired
    private RoomProviderService roomProviderService;

    @Autowired
    private TenantProviderService tenantProviderService;

    @Autowired
    private UtilityProviderService utilityProviderService;

    @Autowired
    private BillTenantRelevanceMapper billTenantRelevanceMapper;

    /**
     * 结算账单
     *
     * @param dto
     */
    @Override
    public void settleBill(SettleBillDTO dto) {
        List<Bill> billList = billMapper.selectBatchIds(dto.getBillIdList());
        BigDecimal waterPrice = (BigDecimal) redisUtil.getValue("water_price");
        BigDecimal electricPrice = (BigDecimal) redisUtil.getValue("electricity_price");
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        billList.forEach(bill -> {
            bill.settle(waterPrice, electricPrice);
            bill.setUpdateUser(jwtUser);
        });
        billMapper.settleBatch(billList);
    }

    /**
     * 设置水电单价
     *
     * @param dto
     */
    @Override
    public void setUtilityPrice(SetUtilityPriceDTO dto) {
        Optional.ofNullable(dto.getWaterPrice()).ifPresent(s -> redisUtil.valueSet("water_price", s));
        Optional.ofNullable(dto.getElectricityPrice()).ifPresent(s -> redisUtil.valueSet("electricity_price", s));
        Optional.ofNullable(dto.getAccessCardPrice()).ifPresent(s -> {
            s.forEach((key, value) -> redisUtil.hashPut("access_card_price", key, value));
        });
    }

    /**
     * 获取水电单价
     *
     * @return
     */
    @Override
    public UtilityPrice getUtilityPrice() {
        BigDecimal waterPrice = (BigDecimal) redisUtil.getValue("water_price");
        BigDecimal electricPrice = (BigDecimal) redisUtil.getValue("electricity_price");
        Map<String, Integer> accessCardPrice = redisUtil.getHash("access_card_price");
        return new UtilityPrice(waterPrice, electricPrice, accessCardPrice);
    }

    /**
     * 将到期账单设置为等待登记水电
     */
    @Override
    @Transactional
    public void setBillUnconfirmed() {
        //将到期账单状态设置为等待登记水电
        DateTime now = DateTime.now();
        Timestamp currentTime = new Timestamp(now.getTime());
        List<BillDetails> initBillList = billMapper.getInitBillList(BillStatus.INIT, BillType.MONTHLY);
        List<BillDetails> initializedBillList = initBillList.stream()
                .filter(e -> DateUtil.isSameDay(e.getCompletedDate(), now))
                .collect(Collectors.toList());
        Map<Integer, Integer> utilityMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(initializedBillList)) {
            //获取水电情况
            UtilityPrice price = getUtilityPrice();
            //结算水电
            for (BillDetails bill : initializedBillList) {
                bill.settleWater(price.getWaterPrice());
                bill.settleElectricity(price.getElectricityPrice());
                bill.setUpdateUser(JwtUser.builder().id(0).realName("系统").build());
                bill.unconfirmed();
//                try {
//                    receiptUtils.generateReceipt(bill);
//                } catch (IOException e) {
//                    log.error("生成收据失败，账单id={}", bill.getId());
//                }
                utilityMap.put(bill.getRoomId(), bill.getUtilityEndId());
            }
            //更新
            billMapper.settleBatch(initializedBillList);

            //生成新的账单
            List<Integer> roomIdList = initializedBillList.stream().map(Bill::getRoomId).collect(Collectors.toList());
            List<Room> roomInfoList = roomProviderService.getRoomInfoBatch(roomIdList);
            DateTime oneMonthLater = now.offsetNew(DateField.MONTH, 1);
            DateTime twoMonthLater = oneMonthLater.offsetNew(DateField.MONTH, 2);
            List<Bill> newBillList = new ArrayList<>();
            for (int i = 0; i < roomInfoList.size(); i++) {
                Room roomInfo = roomInfoList.get(i);
                if (Objects.equals(roomInfo.getStatus(), RoomStatus.ON_RENT)) {
                    Bill newBill = Bill.builder()
                            .year(now.year())
                            .month(now.month())
                            .houseId(roomInfo.getHouseId())
                            .houseName(roomInfo.getHouseName())
                            .roomId(roomInfo.getId())
                            .roomNumber(roomInfo.getRoomNumber())
                            .rent(roomInfo.getPrice())
                            .completedDate(oneMonthLater)
                            .startDate(oneMonthLater)
                            .endDate(twoMonthLater)
                            .utilityStartId(utilityMap.get(roomInfo.getId()))
                            .status(BillStatus.INIT)
                            .type(BillType.MONTHLY)
                            .build();
                    newBill.setCreateUser(0, "系统", currentTime);
                    newBillList.add(newBill);
                }
            }
            billMapper.insertBatch(newBillList);
            Map<Integer, List<Integer>> tenantIdMap = tenantProviderService.getTenantIdByRoomId(roomIdList);
            List<BillTenantRelevance> relevanceList = newBillList.stream()
                    .flatMap(e -> tenantIdMap.get(e.getRoomId()).stream().map(e1 -> BillTenantRelevance.of(e.getId(),
                            e1)))
                    .collect(Collectors.toList());
            billTenantRelevanceMapper.insertBatch(relevanceList);
        }
    }

    /**
     * 获取账单列表
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<BillVO> getBillList(GetBillListDTO dto) {
        Long count = billMapper.countBill(dto.getStatus());
        if (count == 0) {
            return new ListQueryVO<>(new ArrayList<>(), 0L);
        }
        Integer pageIndex = dto.getPageIndex();
        Integer pageSize = dto.getPageSize();
        Integer start = (pageIndex - 1) * pageSize;
        List<BillTenantEntity> list = billMapper.getBillList(dto.getStatus(), start, pageSize);
        Collection<BillVO> collection = list.stream().collect(Collectors.groupingBy(Bill::getId,
                Collectors.collectingAndThen(Collectors.toList(), e -> {
                    BillTenantEntity entity = e.stream().findFirst().get();
                    String name = e.stream().map(BillTenantEntity::getName).collect(Collectors.joining("、"));
                    BillVO vo = BillVO.assemble(entity);
                    vo.setTenantInfo(name, entity.getPhone());
                    return vo;
                })))
                .values();
        ArrayList<BillVO> vo = ListUtil.toList(collection);
        return new ListQueryVO<>(vo, count);
    }

    /**
     * 获取本月总额
     *
     * @return
     */
    @Override
    public BigDecimal getMonthTotal() {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "water_charge", "electricity_charge", "rent", "type", "deposit", "access_card_charge")
                .eq("month(pay_time)", DateUtil.month(DateTime.now()) + 1)
                .eq("status", BillStatus.PAID);
        List<Bill> bills = billMapper.selectList(queryWrapper);
        Optional<BigDecimal> total = Optional.empty();
        if (CollectionUtil.isNotEmpty(bills)) {
            BigDecimal totalValue = bills.stream()
                    .map(Bill::totalCharge)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = Optional.of(totalValue);
        }
        return total.orElse(BigDecimal.ZERO);
    }

    /**
     * 获取账单详情
     *
     * @param dto
     * @return
     */
    @Override
    public BillInfoVO getBillInfo(DetailsQueryDTO dto) {
//        Bill bill = billMapper.selectById(dto.getId());
        BillDetails bill = billMapper.getBillDetails(dto.getId());
        BillInfoVO vo = BillInfoVO.assemble(bill);
        return vo;
    }


    /**
     * 获取账单列表
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<BillVO> getBillList(ListQueryDTO dto) {
        Page<Bill> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", BillStatus.INIT).orderByDesc("update_time");
        IPage<Bill> page = billMapper.selectPage(queryPage, queryWrapper);
        List<BillVO> list = page.getRecords().stream().map(BillVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(list, page.getTotal());
    }

    /**
     * 获取房间账单列表
     *
     * @param dto
     */
    @Override
    public ListQueryVO<BillVO> getRoomBillList(GetBillListDTO dto) {
        Page<Bill> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", dto.getRoomId())
                .ne("status", BillStatus.INIT)
                .orderByDesc("end_date");
        IPage<Bill> page = billMapper.selectPage(queryPage, queryWrapper);
        List<BillVO> list = page.getRecords().stream().map(BillVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(list, page.getTotal());
    }

    /**
     * 查询账单列表（管理平台）
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<BillInfoVO> getBills(GetBillsDTO dto) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        Optional.ofNullable(dto.getHouseId()).ifPresent(s -> queryWrapper.eq("house_id", s));
        Optional.ofNullable(dto.getRoomNumber()).ifPresent(s -> queryWrapper.eq("room_number", s));
        Optional.ofNullable(dto.getStatus()).ifPresent(s -> queryWrapper.eq("status", s));
        queryWrapper.ne("status", BillStatus.INIT);
        Page<Bill> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        IPage<Bill> page = billMapper.selectPage(queryPage, queryWrapper);
        List<BillInfoVO> list = page.getRecords().stream().map(BillInfoVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(list, page.getTotal());
    }

    /**
     * 生成收据
     *
     * @param dto
     */
    @Override
    public String createReceipt(DetailsQueryDTO dto) throws IOException {
        BillDetails bill = billMapper.getBillDetails(dto.getId());
        if (bill == null) {
            throw new EntityNotExistException("账单不存在");
        }
        receiptUtils.generateReceipt(bill);
        UpdateWrapper<Bill> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("receipt_url", bill.getReceiptUrl())
                .eq("id", bill.getId());
        return bill.getReceiptUrl();
    }

    /**
     * 矫正水电读数
     *
     * @param dto
     */
    @Override
    @Transactional
    public void correctUtility(DetailsQueryDTO dto) {
        Bill bill = billMapper.selectById(dto.getId());
        if (bill == null) {
            throw new EntityNotExistException("账单不存在");
        }
        //获取当前水电行度记录id
        Integer currentUtilityId = utilityProviderService.getCurrentUtility(bill.getRoomId());
        //更新当前未结算账单关联的结束水电记录id
        UpdateWrapper<Bill> unconfirmedUpdateWrapper = new UpdateWrapper<>();
        if (BillType.CHECK_IN.equals(bill.getType())) {
            unconfirmedUpdateWrapper.set("utility_start_id", currentUtilityId).eq("id", dto.getId());
        }else{
            unconfirmedUpdateWrapper.set("utility_end_id", currentUtilityId).eq("id", dto.getId());
            //获取更新后的当前账单记录
            UtilityPrice price = getUtilityPrice();
            //结算各项记录
            BillDetails billDetails = billMapper.getBillDetails(dto.getId());
            billDetails.settleWater(price.getWaterPrice());
            billDetails.settleElectricity(price.getElectricityPrice());
            billDetails.setUpdateUser(JwtUser.builder().id(0).realName("系统").build());
            UpdateWrapper<Bill> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("water_usage", billDetails.getWaterUsage())
                    .set("water_charge", billDetails.getWaterCharge())
                    .set("electricity_usage", billDetails.getElectricityUsage())
                    .set("electricity_charge", billDetails.getElectricityCharge())
                    .eq("id", bill.getId());
            billMapper.update(null, updateWrapper);
            UpdateWrapper<Bill> initUpdateWrapper = new UpdateWrapper<>();
            initUpdateWrapper.set("utility_start_id", currentUtilityId)
                    .eq("room_id", bill.getRoomId())
                    .eq("status", BillStatus.INIT);
            billMapper.update(null, initUpdateWrapper);
        }
        billMapper.update(null, unconfirmedUpdateWrapper);
        //更新当前使用的账单关联的开始水电记录id
//        try {
//            receiptUtils.generateReceipt(billDetails);
//        } catch (IOException e) {
//            log.error("生成收据失败，账单id={}", bill.getId());
//        }

    }

    /**
     * 确认账单无误
     *
     * @param dto
     */
    @Override
    public void confirmBill(DetailsQueryDTO dto) {
        BillDetails bill = billMapper.getBillDetails(dto.getId());
        if (bill == null) {
            throw new EntityNotExistException("账单不存在");
        }
        UpdateWrapper<Bill> updateWrapper = new UpdateWrapper<>();
        if(BillType.CHECK_IN.equals(bill.getType())){
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            updateWrapper.set("status", BillStatus.PAID).set("pay_time", timestamp);
        }else{
            updateWrapper.set("status", BillStatus.UNPAID);
        }
        updateWrapper.eq("id", dto.getId());
        if (StrUtil.isBlank(bill.getReceiptUrl())) {
            try {
                receiptUtils.generateReceipt(bill);
                updateWrapper.set("receipt_url", bill.getReceiptUrl());
            } catch (IOException e) {
                log.error("生成收据IO异常，id={}", bill.getId());
            }
        }
        billMapper.update(null, updateWrapper);
    }

    /**
     * 房客已支付
     *
     * @param dto
     */
    @Override
    public void billPaid(BillPaidDTO dto) {
        Bill bill = billMapper.selectById(dto.getId());
        if (bill == null) {
            throw new EntityNotExistException("账单不存在");
        }
        PaymentMethod paymentMethod = PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase());
        UpdateWrapper<Bill> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", BillStatus.PAID)
                .set("payment_method", paymentMethod)
                .set("pay_time", new Timestamp(System.currentTimeMillis()))
                .eq("id", dto.getId());
        billMapper.update(null, updateWrapper);
    }

    /**
     * 触发结算账单
     *
     * @param dto 房间id
     */
    @Override
    public void triggerSetBillUnconfirmed(DetailsQueryDTO dto) {
        BillDetails bill = billMapper.getBillDetailsByRoomId(dto.getId());
        if (bill != null && bill.getCompletedDate().before(DateTime.now())) {
            JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UtilityPrice price = getUtilityPrice();
            bill.settleWater(price.getWaterPrice());
            bill.settleElectricity(price.getElectricityPrice());
            bill.setUpdateUser(jwtUser);
            bill.unconfirmed();
            billMapper.settleBatch(Collections.singletonList(bill));
            JSONObject room = roomProviderService.getRoomInfo(dto.getId());
            if (Objects.equals(room.getStr("status"), RoomStatus.ON_RENT.getValue())) {
                DateTime time = DateTime.of(bill.getCompletedDate());
                Bill newBill = Bill.builder()
                        .year(time.year())
                        .month(time.month())
                        .houseId(bill.getHouseId())
                        .houseName(bill.getHouseName())
                        .roomId(bill.getRoomId())
                        .roomNumber(bill.getRoomNumber())
                        .rent(room.getInt("price"))
                        .completedDate(time.offsetNew(DateField.MONTH, 1))
                        .startDate(time.offsetNew(DateField.MONTH, 1))
                        .endDate(time.offsetNew(DateField.MONTH, 2))
                        .utilityStartId(bill.getUtilityEndId())
                        .status(BillStatus.INIT)
                        .type(BillType.MONTHLY)
                        .build();
                newBill.setCreateUser(jwtUser.getId(), jwtUser.getRealName(),
                        new Timestamp(System.currentTimeMillis()));
                billMapper.insert(newBill);
            }
        } else {
            throw new UnmodifiableException("当前房间未到结算日");
        }
    }

    /**
     * 房客获取账单
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<BillVO> getTenantBillList(ListQueryDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Tenant tenant = tenantProviderService.getTenantByUserId(jwtUser.getId());
        Integer count = billMapper.countBillByTenant(tenant.getId());
        if(count > 0){
            Integer pageIndex = dto.getPageIndex();
            Integer pageSize = dto.getPageSize();
            Integer start = (pageIndex - 1) * pageSize;
            List<Bill> billList = billMapper.getBillListByTenant(tenant.getId(), start, pageSize);
            List<BillVO> list = billList.stream().map(BillVO::of).collect(Collectors.toList());
            return new ListQueryVO<>(list, (long) count);
        }else{
            return new ListQueryVO<>(ListUtil.empty(), 0L);
        }
    }
}
