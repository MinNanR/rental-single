package site.minnan.rental.application.service;

import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.vo.ListQueryVO;
import site.minnan.rental.domain.vo.utility.UtilityFileVO;
import site.minnan.rental.domain.vo.utility.UtilityRecordVO;
import site.minnan.rental.domain.vo.utility.UtilityVO;
import site.minnan.rental.userinterface.dto.utility.AddUtilityDTO;
import site.minnan.rental.userinterface.dto.utility.GetRecordListDTO;
import site.minnan.rental.userinterface.dto.utility.GetUtilityDTO;
import site.minnan.rental.userinterface.dto.utility.UpdateUtilityDTO;

import java.util.List;

public interface UtilityService {

    /**
     * 登记水电（批量）
     *
     * @param dtoList
     */
    void addUtilityBatch(List<AddUtilityDTO> dtoList);

    /**
     * 登记水电（单个房间）
     * @param dto
     */
    Utility addUtility(AddUtilityDTO dto);

    /**
     * 修改水电记录
     *
     * @param dto
     */
    void updateUtility(UpdateUtilityDTO dto);

    /**
     * 获取水电记录
     *
     * @param dto
     * @return
     */
    ListQueryVO<UtilityVO> getUtilityList(GetUtilityDTO dto);

    /**
     * 查询登记水电的记录
     *
     * @param dto
     * @return
     */
    ListQueryVO<UtilityRecordVO> getRecordList(GetRecordListDTO dto);

    /**
     * 备份水电记录（每月定时任务）
     */
    void backUpUtility();

    /**
     * 获取水电记录的备份记录
     * @return
     */
    List<UtilityFileVO> getUtilityFileList();
}
