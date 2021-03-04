package site.minnan.rental.application.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.rental.application.provider.BillProviderService;
import site.minnan.rental.application.service.UtilityService;
import site.minnan.rental.domain.aggregate.Utility;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.entity.UtilityRecord;
import site.minnan.rental.domain.mapper.UtilityMapper;
import site.minnan.rental.domain.mapper.UtilityRecordMapper;
import site.minnan.rental.domain.vo.ListQueryVO;
import site.minnan.rental.domain.vo.utility.UtilityFileVO;
import site.minnan.rental.domain.vo.utility.UtilityRecordVO;
import site.minnan.rental.domain.vo.utility.UtilityVO;
import site.minnan.rental.infrastructure.enumerate.UtilityStatus;
import site.minnan.rental.infrastructure.exception.UnmodifiableException;
import site.minnan.rental.infrastructure.utils.RedisUtil;
import site.minnan.rental.userinterface.dto.utility.AddUtilityDTO;
import site.minnan.rental.userinterface.dto.utility.GetRecordListDTO;
import site.minnan.rental.userinterface.dto.utility.GetUtilityDTO;
import site.minnan.rental.userinterface.dto.utility.UpdateUtilityDTO;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UtilityServiceImpl implements UtilityService {

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private UtilityRecordMapper utilityRecordMapper;

    @Autowired
    private BillProviderService billProviderService;

    @Autowired
    private OSS oss;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${aliyun.bucketName}")
    private String bucketName;

    @Value("${aliyun.utilityFolder}")
    private String folder;

    @Value("${aliyun.baseUrl}")
    private String baseUrl;

    @Override
    @Transactional
    public void addUtilityBatch(List<AddUtilityDTO> dtoList) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        beforeRecord(dtoList.get(0));
        Timestamp current = new Timestamp(System.currentTimeMillis());
        List<Utility> newUtilityList = dtoList.stream()
                .map(Utility::assemble)
                .peek(e -> {
                    e.setCreateUser(jwtUser, current);
                })
                .collect(Collectors.toList());
        Set<Integer> roomIdSet = newUtilityList.stream().map(Utility::getRoomId).collect(Collectors.toSet());
        UpdateWrapper<Utility> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("status", UtilityStatus.RECORDING)
                .in("room_id", roomIdSet)
                .set("status", UtilityStatus.RECORDED);
        utilityMapper.update(null, updateWrapper);
        utilityMapper.addUtilityBatch(newUtilityList);
        billProviderService.completeBill(roomIdSet);
    }

    /**
     * 登记水电（单个房间）
     *
     * @param dto
     */
    @Override
    @Transactional
    public void addUtility(AddUtilityDTO dto) {
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        beforeRecord(dto);
        Utility utility = Utility.assemble(dto);
        utility.setCreateUser(jwtUser);
        UpdateWrapper<Utility> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("room_id", dto.getRoomId())
                .eq("status", UtilityStatus.RECORDING)
                .set("status", UtilityStatus.RECORDED);
        utilityMapper.update(null, updateWrapper);
        utilityMapper.insert(utility);
        billProviderService.completeBill(Collections.singletonList(dto.getRoomId()));
    }

    /**
     * 修改水电记录
     *
     * @param dto
     */
    @Override
    public void updateUtility(UpdateUtilityDTO dto) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", dto.getId())
                .select("id", "status");
        Utility utility = utilityMapper.selectOne(queryWrapper);
        if (!UtilityStatus.RECORDING.equals(utility.getStatus())) {
            throw new UnmodifiableException("当前记录不可修改");
        }
        JwtUser jwtUser = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UpdateWrapper<Utility> updateWrapper = new UpdateWrapper<>();
        Optional.ofNullable(dto.getWater()).ifPresent(s -> updateWrapper.set("water", s));
        Optional.ofNullable(dto.getElectricity()).ifPresent(s -> updateWrapper.set("electricity", s));
        updateWrapper.eq("id", dto.getId())
                .set("update_user_id", jwtUser.getId())
                .set("update_user_name", jwtUser.getRealName())
                .set("update_time", new Timestamp(System.currentTimeMillis()));
        utilityMapper.update(null, updateWrapper);
    }

    /**
     * 获取水电记录
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<UtilityVO> getUtilityList(GetUtilityDTO dto) {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        Optional.ofNullable(dto.getHouseId()).ifPresent(s -> queryWrapper.eq("house_id", s));
        Optional.ofNullable(dto.getRoomNumber())
                .map(String::trim)
                .ifPresent(s -> queryWrapper.likeRight("room_number", s));
        Optional.ofNullable(dto.getYear()).ifPresent(s -> queryWrapper.eq("year(create_time)", s));
        Optional.ofNullable(dto.getMonth()).ifPresent(s -> queryWrapper.eq("month(create_time)", s));
        Optional.ofNullable(dto.getDate()).ifPresent(s -> queryWrapper.eq("datediff(create_time, '" + s + "')", 0));
        Optional.ofNullable(dto.getRoomId()).ifPresent(s -> queryWrapper.eq("room_id", s));
        Optional.ofNullable(dto.getStatus()).ifPresent(s -> queryWrapper.eq("status", s));
        queryWrapper.orderByDesc("create_time", "room_number");
        IPage<Utility> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        IPage<Utility> page = utilityMapper.selectPage(queryPage, queryWrapper);
        List<UtilityVO> vo = page.getRecords().stream().map(UtilityVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(vo, page.getTotal());
    }

    /**
     * 查询登记水电的记录
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<UtilityRecordVO> getRecordList(GetRecordListDTO dto) {
        QueryWrapper<UtilityRecord> queryWrapper = new QueryWrapper<>();
        Optional.ofNullable(dto.getHouseId()).ifPresent(s -> queryWrapper.eq("house_id", s));
        queryWrapper.orderByDesc("record_date");
        Page<UtilityRecord> queryPage = new Page<>(dto.getPageIndex(), dto.getPageSize());
        IPage<UtilityRecord> page = utilityRecordMapper.selectPage(queryPage, queryWrapper);
        List<UtilityRecordVO> list =
                page.getRecords().stream().map(UtilityRecordVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(list, page.getTotal());
    }

    private void beforeRecord(AddUtilityDTO first) {
        //检查今天是否有记录
        Object haveRecord = redisUtil.getValue("haveRecord_" + first.getHouseId());
        if (haveRecord == null) {
            DateTime now = DateTime.now();
            DateTime endOfDay = DateUtil.endOfDay(now);
            long timeout = DateUtil.between(now, endOfDay, DateUnit.SECOND);
            String recordName = StrUtil.format("{}{}记录", first.getHouseName(), now.toString("yyyy年M月d日"));
            UtilityRecord record = new UtilityRecord(first.getHouseId(), first.getHouseName(), recordName, now);
            utilityRecordMapper.insert(record);
            redisUtil.valueSet("haveRecord_" + first.getHouseId(), true, Duration.ofSeconds(timeout));
        }
    }

    /**
     * 备份水电记录（每月定时任务）
     */
    @Override
    public void backUpUtility() {
        QueryWrapper<Utility> queryWrapper = new QueryWrapper<>();
        DateTime lastMonth = DateTime.now().offset(DateField.MONTH, -1);
        queryWrapper.eq("year(create_time)", lastMonth.year())
                .eq("month(create_time)", lastMonth.month() + 1)
                .orderByAsc("update_time");
        List<Utility> utilityList = utilityMapper.selectList(queryWrapper);
//        Map<String, List<Utility>> groupByHouseName = utilityList.stream()
//                .collect(Collectors.groupingBy(Utility::getHouseName, Collectors.collectingAndThen(Collectors
//                .toList(),
//                        e -> e.stream().sorted(Comparator.comparing(Utility::getRoomNumber)).collect(Collectors
//                        .toList()))));
//        String time = DateUtil.format(lastMonth, "yyyy年M月");
//        groupByHouseName.forEach((key, value) -> {
//            try {
//                saveToCsv(key, time, value);
//            } catch (IOException e) {
//                log.error("备份水电记录失败，房屋：{}，时间：{}", key, time);
//            }
//        });
        int year = DateUtil.year(lastMonth);
        utilityList.stream()
                .collect(Collectors.groupingBy(e -> StrUtil.format("{}-{}", e.getHouseName(), e.getRoomNumber())))
                .entrySet().forEach(entry -> {
            try {
                appendToCsv(entry.getValue(), entry.getKey(), year);
            } catch (IOException e) {
                log.error("备份水电记录失败，房间：{}，时间：{}", entry.getValue(), DateUtil.format(lastMonth, "yyyy-MM"));
            }
        });


    }

    /**
     * 将水电记录备份成csv文件，并保存到oss
     *
     * @param houseName 房屋名称
     * @param time      年份+月份
     * @param data      数据
     */
    public void saveToCsv(String houseName, String time, List<Utility> data) throws IOException {
        File temp = File.createTempFile("temp", "csv");
        CsvWriter writer = CsvUtil.getWriter(temp, CharsetUtil.CHARSET_GBK);
        writer.write(new String[]{"房号", "时间", "水表行度", "电表行度"});
        for (Utility utility : data) {
            writer.write(new String[]{utility.getRoomNumber(),
                    DateUtil.format(utility.getCreateTime(), "M月d日"),
                    String.valueOf(utility.getWater().intValue()),
                    String.valueOf(utility.getElectricity().intValue())
            });
        }
        writer.flush();
        writer.close();
        BufferedInputStream is = FileUtil.getInputStream(temp);
        String ossKey = StrUtil.format("{}/{}{}水电记录.csv", folder, houseName, time);
        oss.putObject(bucketName, ossKey, is);
        log.info("备份{}完成", StrUtil.format("{}{}水电记录", houseName, time));
    }

    /**
     * 获取水电记录的备份记录
     *
     * @return
     */
    @Override
    public List<UtilityFileVO> getUtilityFileList() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName, folder + "/", null, null, 1000);
        ObjectListing objectListing = oss.listObjects(listObjectsRequest);
        List<OSSObjectSummary> objects = objectListing.getObjectSummaries();
        return objects.stream()
                .map(e -> new UtilityFileVO(StrUtil.subBetween(e.getKey(), folder + "/", ".csv"),
                        StrUtil.format("{}/{}", baseUrl, e.getKey())))
                .collect(Collectors.toList());
    }

    /**
     * 追加到csv中
     *
     * @param utilityList 水电数据
     */
    public void appendToCsv(List<Utility> utilityList, String houseNameAndRoomNumber, int year) throws IOException {
        File temp = File.createTempFile("temp", ".csv");
        CsvWriter writer = CsvUtil.getWriter(temp, CharsetUtil.CHARSET_GBK, true);
        String ossKey = StrUtil.format("{}/{}号房{}年水电记录.csv", folder, houseNameAndRoomNumber, year);
        if (!oss.doesObjectExist(bucketName, ossKey)) {
            writer.write(new String[]{"时间", "水表行度", "电表行度"});
        } else {
            oss.getObject(new GetObjectRequest(bucketName, ossKey), temp);
        }
        for (Utility utility : utilityList) {
            writer.write(new String[]{DateUtil.format(utility.getCreateTime(), "M月d日"),
                    String.valueOf(utility.getWater().intValue()),
                    String.valueOf(utility.getElectricity().intValue())
            });
        }
        writer.flush();
        writer.close();
        BufferedInputStream is = FileUtil.getInputStream(temp);
        oss.putObject(bucketName, ossKey, is);
        log.info("备份{}完成", StrUtil.format("{}水电记录", houseNameAndRoomNumber));
    }
}
