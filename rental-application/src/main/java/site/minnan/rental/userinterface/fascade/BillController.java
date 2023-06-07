package site.minnan.rental.userinterface.fascade;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import site.minnan.rental.application.service.BillService;
import site.minnan.rental.domain.vo.*;
import site.minnan.rental.domain.vo.bill.*;
import site.minnan.rental.domain.vo.utility.UtilityPrice;
import site.minnan.rental.infrastructure.enumerate.BillStatus;
import site.minnan.rental.infrastructure.enumerate.UtilityStatus;
import site.minnan.rental.infrastructure.utils.RedisUtil;
import site.minnan.rental.userinterface.dto.*;
import site.minnan.rental.userinterface.dto.bill.*;
import site.minnan.rental.userinterface.dto.utility.SetUtilityPriceDTO;
import site.minnan.rental.userinterface.response.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 水电登记相关操作
 */
//@RestController
@Controller
@RequestMapping("rental/bill")
@Slf4j
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private RedisUtil redisUtil;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("settleBill")
    @ResponseBody
    public ResponseEntity<?> settleBill(@RequestBody @Valid SettleBillDTO dto) {
        billService.settleBill(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LANDLORD')")
    @PostMapping("setUtilityPrice")
    @ResponseBody
    public ResponseEntity<?> setUtilityPrice(@RequestBody SetUtilityPriceDTO dto) {
        billService.setUtilityPrice(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getUtilityPrice")
    @ResponseBody
    public ResponseEntity<UtilityPrice> getUtilityPrice() {
        UtilityPrice utilityPrice = billService.getUtilityPrice();
        return ResponseEntity.success(utilityPrice);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getUtilityStatusDropDown")
    @ResponseBody
    public ResponseEntity<List<BillStatusDropDown>> getUtilityStatusDropDown() {
        List<BillStatusDropDown> dropDownList = Arrays.stream(UtilityStatus.values())
                .map(e -> new BillStatusDropDown(e.getStatus(), e.getValue()))
                .collect(Collectors.toList());
        return ResponseEntity.success(dropDownList);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getBillStatusDropDown")
    @ResponseBody
    public ResponseEntity<List<BillStatusDropDown>> getBillStatusDropDown() {
        List<BillStatusDropDown> dropDownList = Arrays.stream(ArrayUtil.sub(BillStatus.values(), 1,
                BillStatus.values().length))
                .map(e -> new BillStatusDropDown(e.getStatus(), e.getValue()))
                .collect(Collectors.toList());
        return ResponseEntity.success(dropDownList);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getBillList/{status}")
    @ResponseBody
    public ResponseEntity<ListQueryVO<BillVO>> getBillList(@RequestBody @Valid GetBillListDTO dto,
                                                           @PathVariable("status") String status) {
        dto.setStatus(BillStatus.valueOf(status.toUpperCase()));
        ListQueryVO<BillVO> vo = billService.getBillList(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getBillList")
    @ResponseBody
    public ResponseEntity<ListQueryVO<BillVO>> getBillList(@RequestBody @Valid ListQueryDTO dto) {
        ListQueryVO<BillVO> vo = billService.getBillList(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LANDLORD','GUEST')")
    @PostMapping("getBills")
    @ResponseBody
    public ResponseEntity<ListQueryVO<BillInfoVO>> getBills(@RequestBody @Valid GetBillsDTO dto) {
        ListQueryVO<BillInfoVO> vo = billService.getBills(dto);
        return ResponseEntity.success(vo);
    }


    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getMonthTotal")
    @ResponseBody
    public ResponseEntity<String> getMonthTotal() {
        BigDecimal total = billService.getMonthTotal();
        String totalStr = NumberUtil.decimalFormat(",###", total);
        return ResponseEntity.success(totalStr);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getMonthTotal/v2")
    @ResponseBody
    public ResponseEntity<List<MonthTotalVO>> getMonthTotalList() {
        List<MonthTotalVO> monthTotalList = billService.getMonthTotalVO();
        return ResponseEntity.success(monthTotalList);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST','TENANT')")
    @PostMapping("getBillInfo")
    @ResponseBody
    public ResponseEntity<BillInfoVO> getBillInfo(@RequestBody @Valid DetailsQueryDTO dto) {
        BillInfoVO vo = billService.getBillInfo(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("createReceipt")
    @ResponseBody
    public ResponseEntity<String> createReceipt(@RequestBody @Valid DetailsQueryDTO dto) {
        try {
            String url = billService.createReceipt(dto);
            return ResponseEntity.success(url);
        } catch (IOException e) {
            log.error("生成收据失败, id=" + dto.getId(), e);
            return ResponseEntity.fail("生成收据失败");
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("correctBill")
    @ResponseBody
    public ResponseEntity<?> correctUtility(@RequestBody @Valid DetailsQueryDTO dto) {
        billService.correctUtility(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("confirmBill")
    @ResponseBody
    public ResponseEntity<?> confirm(@RequestBody @Valid DetailsQueryDTO dto) {
        billService.confirmBill(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LANDLORD')")
    @PostMapping("paid")
    @ResponseBody
    public ResponseEntity<?> paid(@RequestBody @Valid BillPaidDTO dto) {
        billService.billPaid(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getRoomBillList")
    @ResponseBody
    public ResponseEntity<?> getRoomBillList(@RequestBody @Valid GetBillListDTO dto) {
        ListQueryVO<BillVO> vo = billService.getRoomBillList(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("triggerSetBillUnconfirmed")
    @ResponseBody
    public ResponseEntity<?> triggerSetBillUnconfirmed(@RequestBody @Valid DetailsQueryDTO dto) {
        billService.triggerSetBillUnconfirmed(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('TENANT')")
    @PostMapping("getTenantBill")
    @ResponseBody
    public ResponseEntity<ListQueryVO<BillVO>> getTenantBill(@RequestBody @Valid ListQueryDTO dto) {
        ListQueryVO<BillVO> vo = billService.getTenantBillList(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LANDLORD','GUEST')")
    @PostMapping("getBillData")
    @ResponseBody
    public ResponseEntity<BillData> getBillData(@RequestBody @Valid GetBillDataDTO dto) {
        BillData vo = billService.getBillData(dto);
        return ResponseEntity.success(vo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("fillBill")
    @ResponseBody
    public ResponseEntity<?> fillBill(@RequestBody @Valid FillBillDTO dto) {
        if (!redisUtil.setnx("fillBill:" + dto.getBillId(), 1)) {
            return ResponseEntity.message("请勿频繁操作");
        }
        redisUtil.setExpire("fillBill:" + dto.getBillId(), 1L, TimeUnit.MINUTES);
        billService.fillMonthlyBill(dto);
        redisUtil.delete("fillBill:" + dto.getBillId());
        return ResponseEntity.success(dto);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LANDLORD')")
    @PostMapping("modifyBill")
    @ResponseBody
    public ResponseEntity<?> modifyBill(@RequestBody @Valid ModifyBillDTO dto) {
        billService.modifyBill(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('TENANT')")
    @PostMapping("getChartData")
    @ResponseBody
    public ResponseEntity<List<ChartVO>> getChartVO() {
        List<ChartVO> chartData = billService.getChartData();
        return ResponseEntity.success(chartData);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("exportBills")
    public void exportBillList(@RequestBody GetBillsDTO dto, HttpServletResponse response) throws UnsupportedEncodingException {
        billService.exportMonthBill(dto, response);
    }
}
