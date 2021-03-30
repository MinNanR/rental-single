package site.minnan.rental.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import site.minnan.rental.domain.aggregate.Bill;
import site.minnan.rental.domain.entity.BillDetails;
import site.minnan.rental.domain.entity.BillTenantEntity;
import site.minnan.rental.domain.vo.bill.BillData;
import site.minnan.rental.infrastructure.enumerate.BillStatus;
import site.minnan.rental.infrastructure.enumerate.BillType;

import java.util.Collection;
import java.util.List;

/**
 * @author Minnan on 2021/01/06
 */
@Mapper
@Repository
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 批量结算
     *
     * @param bills
     */
    void settleBatch(@Param("bills") Collection<? extends Bill> bills);

    /**
     * 批量插入
     *
     * @param bills
     */
    void insertBatch(@Param("bills") Collection<Bill> bills);

    /**
     * 查询账单
     *
     * @param status
     * @param start
     * @param pageSize
     * @return
     */
    List<BillTenantEntity> getBillList(@Param("status") BillStatus status, @Param("start") Integer start, @Param(
            "pageSize") Integer pageSize);

    /**
     * 计算数量
     *
     * @param status
     * @return
     */
    Long countBill(@Param("status") BillStatus status);

    /**
     * 获取账单所有信息
     *
     * @param id
     * @return
     */
    BillDetails getBillDetails(@Param("id") Integer id);

    /**
     * 获取处于初始化的账单
     *
     * @return
     */
    List<BillDetails> getInitBillList(@Param("status") BillStatus status, @Param("type") BillType type);

    /**
     * 根据房间id获取账单信息
     *
     * @param roomId
     * @return
     */
    BillDetails getBillDetailsByRoomId(@Param("id") Integer roomId);

    /**
     * 根据房间id批量获取处于初始化的账单
     *
     * @param roomIds
     * @return
     */
    List<BillDetails> getBillDetailsList(@Param("roomIds") Iterable<Integer> roomIds);

    @Select("select t1.id id, t1.water_charge waterCharge, t1.electricity_charge electricityCharge, " +
            "t1.access_card_charge, t1.rent rent, t1.deposit deposit, t1.type type, t1.status status,t1.start_date " +
            "startDate, t1.update_time updateTime from rental_bill t1 " +
            "left join rental_bill_tenant_relevance t2 on t1.id = t2.bill_id " +
            "where t2.tenant_id = #{tenantId} and (t1.status = 'UNPAID' or t1.status = 'PAID') " +
            "limit #{start}, #{pageSize}")
    List<Bill> getBillListByTenant(@Param("tenantId") Integer tenantId, @Param("start") Integer start,
                             @Param("pageSize") Integer pageSize);


    @Select("select count(1) from rental_bill t1 left join rental_bill_tenant_relevance t2 on t1.id = t2.bill_id " +
            "where t2.tenant_id = #{tenantId} and (t1.status = 'UNPAID' or t1.status = 'PAID')")
    Integer countBillByTenant(@Param("tenantId") Integer tenantId);

    @Select("select t2.id id, t1.price price,t3.water waterStart,t3.electricity electricityStart,t3.create_time " +
            "utilityStartDate,t3.id utilityStartId ,t2.start_date startDate, t2.end_date endDate from rental_room t1 " +
            "left join rental_bill t2 on t1.id = t2.room_id and t2.status = 'INIT' " +
            "left join rental_utility t3 on t2.utility_start_id = t3.id where t1.id = #{roomId}")
    BillData getBillData(@Param("roomId")Integer roomId);

    @Select("select t1.water_usage waterUsage, t1.electricity_usage, t1.start_date startDate " +
            "from rental_bill t1 " +
            "left join rental_bill_tenant_relevance t2 on t1.id = t2.bill_id " +
            "where tenant_id = #{tenantId} and status != 'INIT' and type != 'CHECK_IN' limit 6")
    List<Bill> getChartData(@Param("tenantId") Integer tenantId);
}
