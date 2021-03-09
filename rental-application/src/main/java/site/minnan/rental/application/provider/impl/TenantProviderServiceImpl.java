package site.minnan.rental.application.provider.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
     * 根据房客id获取房客对象
     *
     * @param id
     * @return
     */
    @Override
    @Cacheable("tenant")
    public Tenant getTenantByUserId(Integer id) {
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", id);
        return tenantMapper.selectOne(queryWrapper);
    }

    /**
     * 根据房间id获取房客id
     *
     * @param roomId
     * @return
     */
    @Override
    public List<Integer> getTenantByRoomId(Integer roomId) {
        Assert.notNull(roomId);
        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId);
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        return tenantList.stream().map(Tenant::getId).collect(Collectors.toList());
    }
}
