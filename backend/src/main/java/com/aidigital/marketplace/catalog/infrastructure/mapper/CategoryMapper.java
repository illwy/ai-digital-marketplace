package com.aidigital.marketplace.catalog.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.catalog.infrastructure.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {}
