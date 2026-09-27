package com.thingfaat.knowledge.mapper;

import com.thingfaat.knowledge.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接口由 MyBatis 生成代理；方法名与 XML statement id 一一对应。
 */
@Mapper
public interface DocumentMapper {
    // 返回插入行数；新 ID 由 XML 的 keyProperty 回填到 entity.id。
    int insert(DocumentEntity entity);

    DocumentEntity findById(@Param("id") Long id);

    List<DocumentEntity> search(@Param("keyword") String keyword);

    int update(DocumentEntity entity);

    int deleteById(@Param("id") Long id);
}
