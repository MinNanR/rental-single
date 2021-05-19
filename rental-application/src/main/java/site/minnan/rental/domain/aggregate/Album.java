package site.minnan.rental.domain.aggregate;

import cn.hutool.core.date.DateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Album {

    private Integer id;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date time;

    private List<String> imageList;
}
