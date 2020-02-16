package top.rizon.asyncretryable.sql;

import top.rizon.asyncretryable.sql.annotation.Column;
import top.rizon.asyncretryable.sql.annotation.Table;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rizon
 * @date 2020/2/16
 */
public class CreateTableUtil {
    public static Map<String, String> javaProperty2SqlColumnMap = new HashMap<>();

    //下边是对应的oracle的生成语句，类型都是oracle，如果是mysql还需要改。
    static {
        javaProperty2SqlColumnMap.put("Integer", "INTEGER(11)");
        javaProperty2SqlColumnMap.put("Short", "INTEGER(4)");
        javaProperty2SqlColumnMap.put("Long", "BIGINT(20)");
        javaProperty2SqlColumnMap.put("BigDecimal", "DECIMAL(22,2)");
        javaProperty2SqlColumnMap.put("Double", "FLOAT(22,2)");
        javaProperty2SqlColumnMap.put("Float", "FLOAT(22,2)");
        javaProperty2SqlColumnMap.put("Boolean", "TINYINT(1)");
        javaProperty2SqlColumnMap.put("Timestamp", "DATE");
        javaProperty2SqlColumnMap.put("String", "VARCHAR(255)");
    }

    public static String createTableSql(Class<?> clazz) throws IOException {
        return createTable(clazz, null);
    }

    public static String selectAsSql(Class<?> clazz) {
        String tableName;
        // 获取类上的注解
        Table annotation = clazz.getAnnotation(Table.class);
        // 输出注解上的类名
        String tableNameAnno = annotation.tableName();
        if (!"".equals(tableNameAnno)) {
            tableName = tableNameAnno;
        } else {
            throw new RuntimeException("没有类名");
        }

        Field[] fields;
        fields = clazz.getDeclaredFields();
        String column;
        StringBuilder sb;
        sb = new StringBuilder(50);
        sb.append("select ");
        for (Field f : fields) {
            column = f.getName();
            if ("serialVersionUID".equals(column)) {
                continue;
            }

            boolean fieldHasAnno = f.isAnnotationPresent(Column.class);
            if (fieldHasAnno) {
                Column fieldAnno = f.getAnnotation(Column.class);
                //输出注解属性
                String name = fieldAnno.name();
                if (!"".equals(name)) {
                    sb.append(name).append(" ").append(column);
                }
            } else {
                sb.append(column);
            }
            sb.append(",");
        }
        sb.append(" from ").append(tableName);
        String sql;
        sql = sb.toString();
        //去掉最后一个逗号
        int lastIndex = sql.lastIndexOf(",");
        sql = sql.substring(0, lastIndex) + sql.substring(lastIndex + 1);
        return sql;
    }

    public static String createTable(Class<?> clz, String tableName) throws IOException {
        // 判断类上是否有次注解
        boolean clzHasAnno = clz.isAnnotationPresent(Table.class);
        String prikey = null;
        if (clzHasAnno) {
            // 获取类上的注解
            Table annotation = clz.getAnnotation(Table.class);
            // 输出注解上的类名
            String tableNameAnno = annotation.tableName();
            if (!"".equals(tableNameAnno)) {
                tableName = tableNameAnno;
            } else {
                throw new RuntimeException("没有类名");
            }
            String keyIdAnno = annotation.keyFields();
            if (!"".equals(keyIdAnno)) {
                prikey = keyIdAnno;
            } else {
                throw new RuntimeException("没有设置主键");
            }
        }
        Field[] fields;
        fields = clz.getDeclaredFields();
        String param = null;
        String column = null;
        StringBuilder sb = null;
        sb = new StringBuilder(50);
        sb.append("create table if not exists ").append(tableName).append(" ( \r\n");
        boolean firstId = true;
        for (Field f : fields) {
            column = f.getName();
            if ("serialVersionUID".equals(column)) {
                continue;
            }
            boolean fieldHasAnno = f.isAnnotationPresent(Column.class);
            if (fieldHasAnno) {
                Column fieldAnno = f.getAnnotation(Column.class);
                //输出注解属性
                String name = fieldAnno.name();
                if (!"".equals(name)) {
                    column = name;
                }
            }

            param = f.getType().getSimpleName();
            //一般第一个是主键
            sb.append(column);
            sb.append(" ").append(javaProperty2SqlColumnMap.get(param)).append(" ");
            if (prikey == null) {
                //类型转换
                if (firstId) {
                    sb.append(" PRIMARY KEY ");
                    firstId = false;
                }
            } else {
                if (prikey.equals(column.toLowerCase())) {
                    sb.append(" PRIMARY KEY ");
                }
            }
            sb.append(",\n ");
        }
        String sql;
        sql = sb.toString();
        //去掉最后一个逗号
        int lastIndex = sql.lastIndexOf(",");
        sql = sql.substring(0, lastIndex) + sql.substring(lastIndex + 1);

        sql = sql.substring(0, sql.length() - 1) + " );\r\n";
        return sql;
    }
}
