package site.minnan.rental.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.vo.utility.UtilityVO;

import java.util.List;

@Mapper
@Repository
public interface UtilityMapper extends BaseMapper<Utility> {

    /**
     * 批量登记水电
     *
     * @param utilityList
     */
    void addUtilityBatch(@Param("list") List<Utility> utilityList);

    /**
     * 根据房客id查询水电记录
     *
     * @param tenantId
     * @param start
     * @param pageSize
     * @return
     */
    @Select("select distinct t1.id id, t1.water water, t1.electricity electricity, t1.create_time updateTime," +
            "t1.status status from rental_utility t1  left join rental_bill t2 on t2.utility_start_id = t1.id " +
            "left join rental_bill_tenant_relevance t3 on t3.bill_id = t2.id " +
            "where t3.tenant_id = #{tenantId}  and t2.status != 'INIT' " +
            "order by t1.create_time desc limit #{start}, #{pageSize}")
    List<Utility> getUtilityByTenant(@Param("tenantId") Integer tenantId, @Param("start") Integer start, @Param(
            "pageSize") Integer pageSize);

    /**
     * 统计与房客关联的水电记录数量
     *
     * @param tenantId
     * @return
     */
    @Select("select count(distinct t1.id) from rental_utility t1 " +
            "left join rental_bill t2 on t2.utility_start_id = t1.id " +
            "left join rental_bill_tenant_relevance t3 on t3.bill_id = t2.id " +
            "where t3.tenant_id = #{tenantId} and t2.status != 'INIT'")
    Integer countUtilityByTenant(@Param("tenantId") Integer tenantId);
}
