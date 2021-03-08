package site.minnan.rental.application.service;

import site.minnan.rental.domain.vo.bill.BillData;
import site.minnan.rental.domain.vo.bill.BillInfoVO;
import site.minnan.rental.domain.vo.bill.BillVO;
import site.minnan.rental.domain.vo.ListQueryVO;
import site.minnan.rental.domain.vo.utility.UtilityPrice;
import site.minnan.rental.userinterface.dto.*;
import site.minnan.rental.userinterface.dto.bill.*;
import site.minnan.rental.userinterface.dto.utility.SetUtilityPriceDTO;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 账单服务
 *
 * @author Minnan on 2021/01/08
 */
public interface BillService {

    /**
     * 结算账单
     *
     * @param dto
     */
    void settleBill(SettleBillDTO dto);

    /**
     * 设置水电单价
     *
     * @param dto
     */
    void setUtilityPrice(SetUtilityPriceDTO dto);

    /**
     * 获取水电单价，门卡
     *
     * @return
     */
    UtilityPrice getUtilityPrice();

    /**
     * 获取账单列表
     *
     * @param dto
     * @return
     */
    ListQueryVO<BillVO> getBillList(GetBillListDTO dto);

    /**
     * 获取账单列表
     *
     * @param dto
     * @return
     */
    ListQueryVO<BillVO> getBillList(ListQueryDTO dto);

    /**
     * 获取房间账单列表
     */
    ListQueryVO<BillVO> getRoomBillList(GetBillListDTO dto);

    /**
     * 查询账单列表（管理平台）
     *
     * @param dto
     * @return
     */
    ListQueryVO<BillInfoVO> getBills(GetBillsDTO dto);

    /**
     * 获取本月总额
     *
     * @return
     */
    BigDecimal getMonthTotal();

    /**
     * 获取账单详情
     *
     * @param dto
     * @return
     */
    BillInfoVO getBillInfo(DetailsQueryDTO dto);

    /**
     * 生成收据
     *
     * @param dto
     */
    String createReceipt(DetailsQueryDTO dto) throws IOException;

    /**
     * 矫正水电读数
     *
     * @param dto
     */
    void correctUtility(DetailsQueryDTO dto);

    /**
     * 确认账单无误
     *
     * @param dto
     */
    void confirmBill(DetailsQueryDTO dto);

    /**
     * 房客已支付
     *
     * @param dto
     */
    void billPaid(BillPaidDTO dto);

    /**
     * 触发结算账单
     *
     * @param dto 房间id
     */
    void triggerSetBillUnconfirmed(DetailsQueryDTO dto);

    /**
     * 房客获取账单
     *
     * @param dto
     * @return
     */
    ListQueryVO<BillVO> getTenantBillList(ListQueryDTO dto);

    /**
     * 到期账单结算
     */
    void setBillUnconfirmed();

    /**
     * 获取填写月度账单时所需要的数据
     *
     * @param dto
     */
    BillData getBillData(GetBillDataDTO dto);

    /**
     * 添加月度账单
     *
     * @param dto
     */
    void fillMonthlyBill(FillBillDTO dto);

    /**
     * 修改账单数据
     *
     * @param dto
     */
    void modifyBill(ModifyBillDTO dto);
}
