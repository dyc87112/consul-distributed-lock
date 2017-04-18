package com.didispace.lock.consul;

import com.ecwid.consul.v1.kv.model.GetValue;
import com.google.gson.Gson;
import lombok.Data;
import lombok.SneakyThrows;
import sun.misc.BASE64Decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 信号量存储结构：并发上限值、当前持有者列表
 *
 * @author 翟永超
 * @create 2017/4/12.
 * @blog http://blog.didispace.com
 */
@Data
public class ContenderValue implements Serializable {

    private Integer limit;  // 上限值
    private List<String> holders = new ArrayList<>();   // 持有者列表

    /**
     * 根据consul中获取的/.lock值来转换
     *
     * @param lockKeyContent
     * @return
     */
    @SneakyThrows
    public static ContenderValue parse(GetValue lockKeyContent) {
        // 获取Value信息，decode BASE64
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] v = decoder.decodeBuffer(lockKeyContent.getValue());
        String lockKeyValueDecode = new String(v);
        // 根据json转换为ContenderValue对象
        Gson gson = new Gson();
        return gson.fromJson(lockKeyValueDecode, ContenderValue.class);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}