package site.minnan.rental.domain.vo.bill;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import site.minnan.rental.domain.aggregate.Bill;

import java.math.BigDecimal;

@Data
@Builder
public class BillVO {

    private Integer id;

    private Integer roomId;

    private String roomNumber;

    private BigDecimal waterCharge;

    private BigDecimal electricityCharge;

    private Integer rent;

    private BigDecimal totalCharge;

    private String time;

    private String month;

    @Setter
    private String livingPeople;

    @Setter
    private String phone;

    private String updateTime;

    private String status;

    private String statusCode;

    private String type;

    private String typeCode;

    public static BillVO assemble(Bill bill){
        return BillVO.builder()
                .id(bill.getId())
                .roomId(bill.getRoomId())
                .roomNumber(StrUtil.format("{}-{}", bill.getHouseName(), bill.getRoomNumber()))
                .waterCharge(bill.getWaterCharge())
                .electricityCharge(bill.getElectricityCharge())
                .rent(bill.getRent())
                .totalCharge(bill.totalCharge())
                .status(bill.getStatus().getStatus())
                .statusCode(bill.getStatus().getValue())
                .type(bill.getType().getType())
                .typeCode(bill.getType().getValue())
                .month(DateTime.of(bill.getStartDate()).month() + 1 + "月")
//                .time(StrUtil.format("{}年{}月", bill.getYear(), bill.getMonth()))
                .time(DateUtil.format(bill.getStartDate(), "yyyy年M月"))
                .updateTime(DateUtil.format(bill.getUpdateTime(), "yyyy-MM-dd"))
                .build();
    }

    public static BillVO of(Bill bill){
        return BillVO.builder()
                .id(bill.getId())
                .totalCharge(bill.totalCharge())
                .status(bill.getStatus().getStatus())
                .statusCode(bill.getStatus().getValue())
                .type(bill.getType().getType())
                .typeCode(bill.getType().getValue())
                .time(DateUtil.format(bill.getStartDate(), "yyyy年M月"))
                .month(DateTime.of(bill.getStartDate()).month() + 1  + "月")
                .updateTime(DateUtil.format(bill.getUpdateTime(), "yyyy-MM-dd"))
                .build();
    }

    public void setTenantInfo(String livingPeople,String phone){
        this.livingPeople = livingPeople;
        this.phone = phone;
    }

}
