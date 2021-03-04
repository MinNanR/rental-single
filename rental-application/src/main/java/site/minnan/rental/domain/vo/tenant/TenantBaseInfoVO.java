package site.minnan.rental.domain.vo.tenant;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import lombok.Data;
import site.minnan.rental.domain.aggregate.Tenant;

/**
 * 房客基本信息
 * @author Minnan on 2021/3/4
 */
@Data
public class TenantBaseInfoVO {

    private String name;

    private String firstPinyinLetter;

    private String roomNumber;

    private String checkInDate;

    private Long checkInDays;

    public TenantBaseInfoVO(Tenant tenant, String firstPinyinLetter){
        this.name = tenant.getName();
        this.firstPinyinLetter = firstPinyinLetter;
        this.roomNumber = tenant.getRoomNumber();
        this.checkInDate = DateUtil.format(tenant.getCreateTime(),"yyyy年M月d日");
        this.checkInDays = DateUtil.betweenDay(tenant.getCreateTime(), DateTime.now(),true);
    }
}
