package org.pado.utils;

import cn.hutool.core.lang.generator.SnowflakeGenerator;

/**
 * @author xuda
 */
public class IdUtil {
    private static final SnowflakeGenerator snowflakeGenerator= new  SnowflakeGenerator();

    public static Long nextId(){
        return snowflakeGenerator.next();
    }



}
