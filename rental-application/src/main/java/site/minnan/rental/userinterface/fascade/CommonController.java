package site.minnan.rental.userinterface.fascade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.rental.application.service.ConfigService;
import site.minnan.rental.domain.aggregate.Menu;
import site.minnan.rental.domain.entity.JwtUser;
import site.minnan.rental.domain.vo.RoleDropDown;
import site.minnan.rental.domain.vo.UserInformation;
import site.minnan.rental.infrastructure.enumerate.Role;
import site.minnan.rental.userinterface.dto.AddMenuDTO;
import site.minnan.rental.userinterface.response.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础配置controller（下拉框，菜单等）
 * @author Minnan created by 2020/12/20
 */
@RestController
@RequestMapping("rental/common")
public class CommonController {

    @Autowired
    private ConfigService configService;

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("addMenu")
    public ResponseEntity<?> addMenu(@RequestBody AddMenuDTO dto){
        configService.addMenu(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("getMenu")
    public ResponseEntity<List<Menu>> getMenu(){
        List<Menu> menuList = configService.getMenu();
        return ResponseEntity.success(menuList);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("getRoleDropDown")
    public ResponseEntity<List<RoleDropDown>> getRoleDropDown(){
        List<RoleDropDown> dropDownList = Arrays.stream(Role.values())
                .map(e -> new RoleDropDown(e.getValue(), e.getRoleName()))
                .collect(Collectors.toList());
        return ResponseEntity.success(dropDownList);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLOR', 'TENANT')")
    @PostMapping("getUserInformation")
    public ResponseEntity<UserInformation> getUserInformation(){
        JwtUser user = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserInformation userInformation = UserInformation.builder()
                .role(user.getAuthorities().toString())
                .realName(user.getRealName())
                .build();
        return ResponseEntity.success(userInformation);
    }
}
