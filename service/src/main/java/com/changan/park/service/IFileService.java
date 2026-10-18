package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.model.po.SysFile;
import com.changan.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface IFileService extends IService<SysFile> {

    /**
     * 单个上传
     * */
    FileVO upload(MultipartFile file);

    /**
     * 批量上传（逐个上传，任一失败即中断）
     */
    List<FileVO> uploadBatch(List<MultipartFile> files);

    /**
     * 标记为已使用：业务保存时把 cover / images 里的 url 传进来
     */
    void markUsed(Collection<String> urls);

    /**
     * 释放引用：USED → TEMP，交给定时任务延迟删除（留 24 小时反悔窗口）
     */
    void release(Collection<String> urls);
}
