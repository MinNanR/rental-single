package site.minnan.rental.userinterface.fascade;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import site.minnan.rental.userinterface.dto.album.UploadImageDTO;
import site.minnan.rental.userinterface.response.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Controller
@RequestMapping("album")
@Slf4j
public class SpecialController {

    @Autowired
    private OSS oss;

    @Value("${aliyun.bucketName}")
    private String bucketName;

    @Value("${aliyun.baseUrl}")
    private String baseUrl;

    @RequestMapping("addImage")
    @ResponseBody
    public ResponseEntity<String> uploadImage(UploadImageDTO dto) {
        try {
            MultipartFile imageFile = dto.getImage();
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            String ossKey = StrUtil.format("album/{}/{}.{}", dto.getId(), uuid, extension);
            InputStream is = imageFile.getInputStream();
            oss.putObject(bucketName, ossKey, is);
            String url = StrUtil.format("{}/album/{}/{}.{}", baseUrl, dto.getId(), uuid, extension);
            return ResponseEntity.success(url);
        } catch (IOException e) {
            log.error("上传图片失败", e);
            return ResponseEntity.fail("上传失败");
        }
    }

}
