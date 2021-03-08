package site.minnan.rental.userinterface.fascade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.rental.application.service.HouseService;
import site.minnan.rental.domain.vo.house.HouseDropDown;
import site.minnan.rental.domain.vo.house.HouseInfoVO;
import site.minnan.rental.domain.vo.house.HouseVO;
import site.minnan.rental.domain.vo.ListQueryVO;
import site.minnan.rental.userinterface.dto.house.AddHouseDTO;
import site.minnan.rental.userinterface.dto.DetailsQueryDTO;
import site.minnan.rental.userinterface.dto.house.GetHouseListDTO;
import site.minnan.rental.userinterface.dto.house.UpdateHouseDTO;
import site.minnan.rental.userinterface.response.ResponseEntity;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("rental/house")
public class HouseController {

    @Autowired
    private HouseService houseService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getHouseList")
    public ResponseEntity<ListQueryVO<HouseVO>> getHouseList(@RequestBody @Valid GetHouseListDTO dto) {
        ListQueryVO<HouseVO> houseList = houseService.getHouseList(dto);
        return ResponseEntity.success(houseList);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("addHouse")
    public ResponseEntity<?> addHouse(@RequestBody @Valid AddHouseDTO dto) {
        houseService.addHouse(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getHouseInfo")
    public ResponseEntity<HouseInfoVO> getHouseInfo(@RequestBody @Valid DetailsQueryDTO dto) {
        HouseInfoVO houseInfo = houseService.getHouseInfo(dto);
        return ResponseEntity.success(houseInfo);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD')")
    @PostMapping("updateHouse")
    public ResponseEntity<?> updateHouse(@RequestBody @Valid UpdateHouseDTO dto){
        houseService.updateHouse(dto);
        return ResponseEntity.success();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LANDLORD','GUEST')")
    @PostMapping("getHouseDropDown")
    public ResponseEntity<?> getHouseDropDown(){
        List<HouseDropDown> vo = houseService.getHouseDropDown();
        return ResponseEntity.success(vo);
    }
}
