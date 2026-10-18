package com.changan.park.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.exceptions.ServiceException;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.config.oss.OssProperties;
import com.changan.common.enums.FileStatus;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.SysFile;
import com.changan.model.vo.FileVO;
import com.changan.park.mapper.SysFileMapper;
import com.changan.park.service.IFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements IFileService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final OSSClient ossClient;

    private final OssProperties properties;

    @Override
    public FileVO upload(MultipartFile file) {
        // 1.校验
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择要上传的文件");
        }
        if (properties.getMaxSize() != null && file.getSize() > properties.getMaxSize()) {
            throw new BadRequestException("文件大小不能超过" + (properties.getMaxSize() / 1024 / 1024) + "MB");
        }

        String originName = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String ext = FileNameUtil.extName(originName).toLowerCase();
        if (StrUtil.isBlank(ext)) {
            throw new BadRequestException("上传的文件缺少扩展类型");
        }
        if (CollUtil.isEmpty(properties.getAllowExt()) || !properties.getAllowExt().contains(ext)) {
            throw new BadRequestException("不支持的文件格式：" + ext);
        }

        // 2.对象键
        String key = StrUtil.format("{}/{}.{}",
                LocalDate.now().format(DATE_DIR),
                IdUtil.fastSimpleUUID(),
                ext);
        String url = buildUrl(key);
        String contentType = resolveContentType(file, key);

        // 3.上传文件
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizIllegalException("文件上传失败");
        }

        try {
            ossClient.putObject(PutObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .body(BinaryData.fromBytes(bytes))
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            ServiceException se = ServiceException.asCause(e);
            if (se != null) {
                // OSS 返回的业务错误，requestId 是唯一能拿去提工单的东西
                log.error("OSS 上传失败 errorCode={}, requestId={}, status={}",
                        se.errorCode(), se.requestId(), se.statusCode(), e);
            } else {
                log.error("OSS 上传失败（非 OSS 业务异常）key={}", key, e);
            }
            throw new BizIllegalException("文件上传失败");
        }

        // 4.入库
        SysFile record = new SysFile();
        record.setObjectKey(key);
        record.setUrl(url);
        record.setOriginName(originName);
        record.setExt(ext);
        record.setContentType(contentType);
        record.setSize(file.getSize());
        record.setStatus(FileStatus.TEMP);
        try {
            save(record);
        } catch (Exception e) {
            deleteObjectQuietly(key);
            throw e;
        }
        return new FileVO(record.getId(), url, originName, record.getSize(), contentType);
    }


    @Override
    public List<FileVO> uploadBatch(List<MultipartFile> files) {
        if (CollUtil.isEmpty(files)) {
            throw new BadRequestException("请选择要上传的文件");
        }
        List<FileVO> result = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            result.add(upload(file));
        }
        return result;
    }

    @Override
    public void markUsed(Collection<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            return;
        }
        List<String> targets = urls.stream().filter(StrUtil::isNotBlank).distinct().toList();
        if (targets.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .in(SysFile::getUrl, targets)
                .eq(SysFile::getStatus, FileStatus.TEMP)
                .set(SysFile::getStatus, FileStatus.USED)
                .update();
    }

    @Override
    public void release(Collection<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            return;
        }
        List<String> targets = urls.stream().filter(StrUtil::isNotBlank).distinct().toList();
        if (targets.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .in(SysFile::getUrl, targets)
                .eq(SysFile::getStatus, FileStatus.USED)
                .set(SysFile::getStatus, FileStatus.TEMP)
                .set(SysFile::getUpdateTime, LocalDateTime.now())
                .update();
    }

    /**
     * 每天 03:30 清理超过 24 小时仍未被业务引用的临时文件
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanTempFiles() {
        LocalDateTime before = LocalDateTime.now().minusHours(24);
        List<SysFile> list = lambdaQuery()
                .eq(SysFile::getStatus, FileStatus.TEMP)
                .lt(SysFile::getUpdateTime, before)
                .last("limit 200")
                .list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        int success = 0;
        for (SysFile record : list) {
            if (deleteObjectQuietly(record.getObjectKey())) {
                removeById(record.getId());
                success++;
            }
        }
        log.info("清理临时文件完成，共 {} 条，成功 {} 条", list.size(), success);
    }

    private String buildUrl(String key) {
        if (StrUtil.isNotBlank(properties.getPublicDomain())) {
            return StrUtil.removeSuffix(properties.getPublicDomain(), "/") + "/" + key;
        }
        String host = StrUtil.isNotBlank(properties.getEndpoint()) ?
                properties.getEndpoint() :
                "oss-" + properties.getRegion() + ".aliyuncs.com";
        host = StrUtil.removePrefix(StrUtil.removePrefix(host, "https://"), "http://");
        return "https://" + properties.getBucket() + "." + host + "/" + key;
    }

    /**
     * Content-Type 决定浏览器是直接显示还是触发下载，缺省时按扩展名兜底
     */
    private String resolveContentType(MultipartFile file, String key) {
        String byExt = MediaTypeFactory.getMediaType(key)
                .map(MediaType::toString)
                .orElse("");
        if (StrUtil.isNotBlank(byExt)) {
            return byExt;
        }
        String contentType = file.getContentType();
        return StrUtil.isBlank(contentType)
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }

    /**
     * 删除 OSS 对象，失败只记日志不抛（清理任务不能因为一个对象失败就中断）
     */
    private boolean deleteObjectQuietly(String key) {
        try {
            ossClient.deleteObject(DeleteObjectRequest.newBuilder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("删除 OSS 对象失败 key={}", key, e);
            return false;
        }
    }
}
