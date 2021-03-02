package site.minnan.rental.application.provider.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.minnan.rental.application.provider.TenantProviderService;
import site.minnan.rental.domain.aggregate.Tenant;
import site.minnan.rental.domain.mapper.TenantMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TenantProviderServiceImpl implements TenantProviderService {

    @Autowired
    private TenantMapper tenantMapper;

    /**
     * 根据房间id获取房客名称
     *
     * @param id
     * @return
     */
    @Override
    public JSONArray getTenantInfoByRoomId(Integer id) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "phone")
                .eq("room_id", id);
        List<Tenant> tenants = tenantMapper.selectList(queryWrapper);
        return tenants.stream().map(JSONObject::new).collect(JSONArray::new, JSONArray::add, JSONArray::addAll);
    }

    /**
     * 批量根据房间id获取房客名称
     *
     * @param ids
     * @return
     */
    @Override
    public Map<Integer, JSONObject> getTenantInfoByTenantIds(Collection<Integer> ids) {
        List<Tenant> tenantList = tenantMapper.selectBatchIds(ids);
        return tenantList.stream().collect(Collectors.groupingBy(Tenant::getRoomId,
                Collectors.collectingAndThen(Collectors.toList(), e -> {
                    String nameStr = e.stream().map(Tenant::getName).collect(Collectors.joining("、"));
                    String phone = e.stream().findFirst().map(Tenant::getPhone).orElse("");
                    JSONObject jsonObject = new JSONObject();
                    return jsonObject.putOpt("name", nameStr).putOpt("phone", phone);
                })));
    }

    @Override
    public Map<Integer, List<Integer>> getTenantIdByRoomId(Collection<Integer> ids) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "room_id")
                .in("room_id", ids);
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        return tenantList.stream().collect(Collectors.groupingBy(Tenant::getRoomId,
                Collectors.mapping(Tenant::getId, Collectors.toList())));
    }

    /**
     * 根据房客id获取信息
     *
     * @param ids
     * @return
     */
    @Override
    public JSONArray getTenantByIds(List<Integer> ids) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name", "phone")
                .in("id", ids);
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        return tenantList.stream().map(JSONObject::new).collect(JSONArray::new, JSONArray::add, JSONArray::addAll);
    }
}
