package site.minnan.rental.userinterface.dto.album;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadImageDTO {
    /**
     * 上传的文件
     */
    private MultipartFile image;

    private Integer id;
}
