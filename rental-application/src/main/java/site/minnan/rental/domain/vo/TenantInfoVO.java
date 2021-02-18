package site.minnan.rental.domain.vo;

import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.minnan.rental.domain.aggregate.Tenant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TenantInfoVO {

    private Integer id;

    private String name;

    private String gender;

    private String phone;

    private String birthday;

    private String hometownProvince;

    private String hometownCity;

    private String identificationNumber;

    private Integer houseId;

    private String houseName;

    private Integer roomId;

    private String roomNumber;

    private String status;

    private String statusCode;

    public TenantInfoVO(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public static TenantInfoVO assemble(Tenant tenant) {
        TenantInfoVOBuilder builder = TenantInfoVO.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .phone(tenant.getPhone())

                .houseId(tenant.getHouseId())
                .houseName(tenant.getHouseName())
                .roomId(tenant.getRoomId())
                .roomNumber(tenant.getRoomNumber())
                .status(tenant.getStatus().getStatus())
                .statusCode(tenant.getStatus().getValue());
        if (tenant.getIdentificationNumber() != null) {
            builder.gender(tenant.getGender().getGender())
                    .identificationNumber(tenant.getIdentificationNumber())
                    .hometownProvince(tenant.getHometownProvince())
                    .hometownCity(tenant.getHometownCity())
                    .birthday(DateUtil.format(tenant.getBirthday(), "yyyy-MM-dd"));
        }
        return builder.build();
    }
}
