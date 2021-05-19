package site.minnan.rental.userinterface.fascade;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import site.minnan.rental.domain.aggregate.Album;
import site.minnan.rental.infrastructure.utils.RedisUtil;
import site.minnan.rental.userinterface.dto.album.AddEventDTO;
import site.minnan.rental.userinterface.dto.album.UploadImageDTO;
import site.minnan.rental.userinterface.response.ResponseEntity;

import javax.xml.ws.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping("addImage")
    @ResponseBody
    public ResponseEntity<String> uploadImage(UploadImageDTO dto) {
        try {
            MultipartFile imageFile = dto.getImage();
            Integer id = dto.getId();
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            String ossKey = StrUtil.format("album/{}/{}.{}", id, uuid, extension);
            InputStream is = imageFile.getInputStream();
            oss.putObject(bucketName, ossKey, is);
            String url = StrUtil.format("{}/album/{}/{}.{}", baseUrl, id, uuid, extension);
            ObjectMapper objectMapper = new ObjectMapper();
            Album album = redisUtil.getBeanFromJsonString("album:" + id, objectMapper, Album.class);
            album.getImageList().add(url);
            redisUtil.putBeanAsJsonString("album:" + id, album, objectMapper);
            return ResponseEntity.success(url);
        } catch (IOException e) {
            log.error("上传图片失败", e);
            return ResponseEntity.fail("上传失败");
        }
    }

    @RequestMapping("listEvent")
    @ResponseBody
    public ResponseEntity<List<Album>> getAlbumList() {
        List<String> scan = redisUtil.scan("album:*");
        ObjectMapper objectMapper = new ObjectMapper();
        List<Album> eventList = scan.stream().map(e -> {
            try {
                return Optional.ofNullable(redisUtil.getBeanFromJsonString(e, objectMapper, Album.class));
            } catch (JsonProcessingException jsonProcessingException) {
                return Optional.empty();
            }
        })
                .filter(Optional::isPresent)
                .map(e -> (Album) e.get())
                .sorted(Comparator.comparing(Album::getTime).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.success(eventList);
    }

    @RequestMapping("addEvent")
    @ResponseBody
    public ResponseEntity<Album> addEvent(@RequestBody AddEventDTO dto) {
        List<String> scan = redisUtil.scan("album:*");
        Integer maxId = scan.stream()
                .map(e -> StrUtil.subAfter(e, "album:", false))
                .map(Integer::parseInt)
                .max(Integer::compareTo)
                .orElse(0);
        int id = maxId + 1;
        Album album = new Album();
        album.setId(id);
        album.setDescription(dto.getDescription());
        album.setTime(DateUtil.parseDate(dto.getTime()));
        album.setImageList(new ArrayList<>());
        try {
            redisUtil.putBeanAsJsonString("album:" + id, album, new ObjectMapper());
            return ResponseEntity.success(album);
        } catch (JsonProcessingException e) {
            log.error("异常", e);
            return ResponseEntity.fail("操作失败");
        }
    }
}
