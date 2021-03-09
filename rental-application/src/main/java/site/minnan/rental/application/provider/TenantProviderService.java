package site.minnan.rental.application.provider;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import site.minnan.rental.domain.aggregate.Tenant;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 房客服务
 *
 * @author Minnan on 2021/1/18
 */
public interface TenantProviderService {

    Map<Integer, List<Integer>> getTenantIdByRoomId(Collection<Integer> ids);

    /**
     * 根据房间id获取房客id
     *
     * @param roomId
     * @return
     */
    List<Integer> getTenantByRoomId(Integer roomId);

    /**
     * 根据房客id获取房客对象
     *
     * @param id
     * @return
     */
    Tenant getTenantByUserId(Integer id);
}
