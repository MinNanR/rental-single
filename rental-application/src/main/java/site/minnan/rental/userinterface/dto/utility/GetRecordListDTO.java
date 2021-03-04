package site.minnan.rental.userinterface.dto.utility;

import lombok.Data;
import site.minnan.rental.userinterface.dto.ListQueryDTO;

/**
 * 水电记录查询参数
 *
 * @author Minnan on 2021/1/27
 */
@Data
public class GetRecordListDTO extends ListQueryDTO {

    private Integer houseId;
}
