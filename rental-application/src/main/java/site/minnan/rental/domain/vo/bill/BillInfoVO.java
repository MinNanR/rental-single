package site.minnan.rental.domain.vo.bill;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.Builder;
import lombok.Data;
import site.minnan.rental.domain.aggregate.Bill;
import site.minnan.rental.domain.entity.BillDetails;
import site.minnan.rental.infrastructure.enumerate.PaymentMethod;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 账单详情数据（小程序用）
 *
 * @author Minnan on 2021/1/20
 */
@Data
@Builder
public class BillInfoVO {

    private Integer id;

    private Integer roomId;

    private Integer houseId;

    private String houseName;

    private Integer year;

    private Integer month;

    private String roomNumber;

    private BigDecimal waterUsage;

    private BigDecimal waterCharge;

    private BigDecimal electricityUsage;

    private BigDecimal electricityCharge;

    private Integer rent;

    private Integer deposit;

    private Integer accessCardQuantity;

    private Integer accessCardCharge;

    private BigDecimal totalCharge;

    private String completeDate;

    private String updateTime;

    private String payTime;

    private String paymentMethod;

    private String paymentMethodCode;

    private String status;

    private String statusCode;

    private String type;

    private String typeCode;

    private String receiptUrl;

    private BigDecimal waterStart;

    private BigDecimal waterEnd;

    private BigDecimal electricityStart;

    private BigDecimal electricityEnd;

    public static BillInfoVO assemble(Bill bill) {
        return BillInfoVO.builder()
                .id(bill.getId())
                .houseId(bill.getHouseId())
                .houseName(bill.getHouseName())
                .roomId(bill.getRoomId())
                .roomNumber(bill.getRoomNumber())
                .year(bill.getYear())
                .month(bill.getMonth())
                .waterUsage(bill.getWaterUsage())
                .waterCharge(bill.getWaterCharge())
                .electricityUsage(bill.getElectricityUsage())
                .electricityCharge(bill.getElectricityCharge())
                .rent(bill.getRent())
                .deposit(bill.getDeposit())
                .accessCardQuantity(bill.getAccessCardQuantity())
                .accessCardCharge(bill.getAccessCardCharge())
                .totalCharge(bill.totalCharge())
                .updateTime(DateUtil.format(bill.getUpdateTime(), "MM-dd"))
                .completeDate(DateUtil.format(bill.getCompletedDate(), "MM-dd"))
                .payTime(DateUtil.format(bill.getPayTime(), "MM-dd"))
                .paymentMethod(Optional.ofNullable(bill.getPaymentMethod()).map(PaymentMethod::getMethod).orElse(""))
                .paymentMethodCode(Optional.ofNullable(bill.getPaymentMethod()).map(PaymentMethod::getValue).orElse(""))
                .status(bill.getStatus().getStatus())
                .statusCode(bill.getStatus().getValue())
                .type(bill.getType().getType())
                .typeCode(bill.getType().getValue())
                .receiptUrl(bill.getReceiptUrl())
                .build();
    }

    public static BillInfoVO assemble(BillDetails bill) {
        DateTime endDate = DateTime.of(bill.getStartDate());
        return BillInfoVO.builder()
                .id(bill.getId())
                .houseId(bill.getHouseId())
                .houseName(bill.getHouseName())
                .roomId(bill.getRoomId())
                .roomNumber(bill.getRoomNumber())
                .year(endDate.year())
                .month(endDate.month())
                .waterUsage(bill.getWaterUsage())
                .waterCharge(bill.getWaterCharge())
                .electricityUsage(bill.getElectricityUsage())
                .electricityCharge(bill.getElectricityCharge())
                .rent(bill.getRent())
                .deposit(bill.getDeposit())
                .accessCardQuantity(bill.getAccessCardQuantity())
                .accessCardCharge(bill.getAccessCardCharge())
                .totalCharge(bill.totalCharge())
                .updateTime(DateUtil.format(bill.getUpdateTime(), "MM-dd"))
                .completeDate(DateUtil.format(bill.getCompletedDate(), "MM-dd"))
                .payTime(DateUtil.format(bill.getPayTime(), "MM-dd"))
                .paymentMethod(Optional.ofNullable(bill.getPaymentMethod()).map(PaymentMethod::getMethod).orElse(""))
                .paymentMethodCode(Optional.ofNullable(bill.getPaymentMethod()).map(PaymentMethod::getValue).orElse(""))
                .status(bill.getStatus().getStatus())
                .statusCode(bill.getStatus().getValue())
                .type(bill.getType().getType())
                .typeCode(bill.getType().getValue())
                .receiptUrl(bill.getReceiptUrl())
                .waterStart(bill.getWaterStart())
                .waterEnd(bill.getWaterEnd())
                .electricityStart(bill.getElectricityStart())
                .electricityEnd(bill.getElectricityEnd())
                .build();
    }
}
