package site.minnan.rental.userinterface.dto.house;

import lombok.Data;
import site.minnan.rental.userinterface.dto.ListQueryDTO;

@Data
public class GetHouseListDTO extends ListQueryDTO {

    private String address;
}
