package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.SensitiveWord;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    @Select("SELECT word FROM sensitive_word")
    List<String> selectAllWords();
}
