package org.pado.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @Author xuda
 * @Date 2023/8/18 14:00
 */
public class DateUtils {
    public static Date convertToDateFromLocalDateTime(LocalDateTime localDateTime){
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        Instant instant = zonedDateTime.toInstant();
        return Date.from(instant);
    }

    public static LocalDateTime convertToLocalDateTime(Date date){
        ZoneId defaultZoneId = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(date.toInstant(), defaultZoneId);
    }

    public static LocalDateTime convertToLocalDateTime(Long time){
        ZoneId defaultZoneId = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), defaultZoneId);
    }


}
