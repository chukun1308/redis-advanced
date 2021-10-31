package com.chukun.redis.canal;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 阿里canal数据库同步组件
 * @author chukun
 */
@Slf4j
@Component
public class RedisCanalClientOperator {

    private static final CanalConnector  CANAL_CONNECTOR;

    /**
     * canal连接主机
     */
    private static final String CANAL_HOST = "linux01";

    /**
     * canal连接主机端口
     */
    private static final int CANAL_PORT = 11111;

    /**
     * 60s
     */
    private static final Integer _60SECONDS = 60;

    /**
     * 批处理size
     */
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private RedisTemplate redisTemplate;

    static {
        // 创建canal连接
        CANAL_CONNECTOR = CanalConnectors.newSingleConnector(new InetSocketAddress(CANAL_HOST, CANAL_PORT), "example", "", "");
        CANAL_CONNECTOR.connect();
        CANAL_CONNECTOR.subscribe("shopmall.tbl_user");
        CANAL_CONNECTOR.rollback();
    }

    /**
     * 执行主逻辑
     */
    public void executeCanalLogic() {
        int emptyCount = 0;
        try {
            int totalEmptyCount = 10 * _60SECONDS;
            while (emptyCount < totalEmptyCount) {
                Message message = CANAL_CONNECTOR.getWithoutAck(BATCH_SIZE); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
                } else {
                    emptyCount = 0;
                    printEntry(message.getEntries());
                }
                CANAL_CONNECTOR.ack(batchId); // 提交确认
                // connector.rollback(batchId); // 处理失败, 回滚数据
            }
            System.out.println("empty too many times, exit");
        } finally {
            CANAL_CONNECTOR.disconnect();
        }
    }

    /**
     * 执行redis的逻辑
     * @param entrys
     */
    private  void printEntry(List<CanalEntry.Entry> entrys) {
        for (CanalEntry.Entry entry : entrys) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            CanalEntry.RowChange rowChange = null;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error,data:" + entry.toString(),e);
            }

            CanalEntry.EventType eventType = rowChange.getEventType();
            System.out.println(String.format("================&gt; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(), eventType));

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                if (eventType == CanalEntry.EventType.INSERT) {
                    redisInsert(rowData.getAfterColumnsList());
                } else if (eventType == CanalEntry.EventType.DELETE) {
                    redisDelete(rowData.getBeforeColumnsList());
                } else if (eventType == CanalEntry.EventType.UPDATE){
                    redisUpdate(rowData.getAfterColumnsList());
                }
            }
        }
    }

    /**
     * redis插入操作
     * @param columns
     */
    private  void redisInsert(List<CanalEntry.Column> columns) {
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.Column column : columns)
        {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
            jsonObject.put(column.getName(),column.getValue());
        }
        if(columns.size() > 0)
        {
            try {
                redisTemplate.opsForValue().set(columns.get(0).getValue(),jsonObject.toJSONString());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * redis删除操作
     * @param columns
     */
    private  void redisDelete(List<CanalEntry.Column> columns) {
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.Column column : columns)
        {
            jsonObject.put(column.getName(),column.getValue());
        }
        if(columns.size() > 0)
        {
            try {
                redisTemplate.delete(columns.get(0).getValue());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * redis更新操作
     * @param columns
     */
    private  void redisUpdate(List<CanalEntry.Column> columns) {
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.Column column : columns)
        {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
            jsonObject.put(column.getName(),column.getValue());
        }
        if(columns.size() > 0)
        {
            try {
                redisTemplate.opsForValue().set(columns.get(0).getValue(),jsonObject.toJSONString());
                System.out.println("---------update after: "+redisTemplate.opsForValue().get(columns.get(0).getValue()));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
