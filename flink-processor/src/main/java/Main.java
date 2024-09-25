import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.common.TopicPartition;
import org.apache.flink.api.java.tuple.Tuple3;
import java.util.Objects;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.common.functions.MapFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;

import java.util.Arrays;
import java.util.HashSet;

public class Main {

    static final String BROKERS = "kafka:9092";

    public static void main(String[] args) throws Exception {
      StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

      System.out.println("Environment created");
      KafkaSource<StockTransaction> source = KafkaSource.<StockTransaction>builder()
                                      .setBootstrapServers(BROKERS)
                                      .setProperty("partition.discovery.interval.ms", "1000")
                                      .setTopics("stockTicker")
                                      .setGroupId("groupdId-919292")
                                      .setStartingOffsets(OffsetsInitializer.earliest())
                                      .setValueOnlyDeserializer(new StockTransactionDeserializationSchema())
                                      .build();

      DataStreamSource<StockTransaction> kafka = env.fromSource(source, WatermarkStrategy.noWatermarks(), "kafka");

      System.out.println("Kafka source created");

      DataStream<Tuple2<StockAverage, Double>> averageTemperatureStream = kafka.keyBy(myEvent -> myEvent.stock)
        .window(TumblingProcessingTimeWindows.of(Time.seconds(60)))
        .aggregate(new AverageAggregator());

      DataStream<Tuple3<String, Integer, Double>> cityAndValueStream = averageTemperatureStream
        .map(new MapFunction<Tuple2<StockAverage, Double>, Tuple3<String, Integer, Double>>() {
            @Override
            public Tuple3<String, Integer, Double> map(Tuple2<StockAverage, Double> input) throws Exception {
                return new Tuple3<>(input.f0.stock, input.f0.count, input.f1);
            }
        });
    
      System.out.println("Aggregation created");
      

      // cityAndValueStream.print();
      cityAndValueStream.addSink(JdbcSink.sink("insert into stockTrans (stock, qty,avgValue) values (?, ?,?)",
            (statement, event) -> {
              statement.setString(1, event.f0);
              statement.setDouble(2, event.f1);
              statement.setDouble(3, event.f2);
            },
            JdbcExecutionOptions.builder()
              .withBatchSize(1000)
              .withBatchIntervalMs(200)
              .withMaxRetries(5)
              .build(),
            new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
              .withUrl("jdbc:singlestore://<<hostname>>:<<port>>/<<database>>")
              .withDriverName("com.singlestore.jdbc.Driver")
              .withUsername("<<username>>")
              .withPassword("<<password>>")
              .build()
      ));

      env.execute("Kafka-flink-postgres");
    }

    /**
     * Aggregation function for average.
     */
    public static class AverageAggregator implements AggregateFunction<StockTransaction, StockAverage, Tuple2<StockAverage, Double>> {

        @Override
        public StockAverage createAccumulator() {
            return new StockAverage();
        }

        @Override
        public StockAverage add(StockTransaction stockTransaction, StockAverage stockAverage) {
            stockAverage.stock = stockTransaction.stock;
            stockAverage.count = stockAverage.count + stockTransaction.Qty;
            stockAverage.sum = stockAverage.sum + stockTransaction.value;
            return stockAverage;
        }

        @Override
        public Tuple2<StockAverage, Double> getResult(StockAverage stockAverage) {
            return new Tuple2<>(stockAverage, stockAverage.sum / stockAverage.count);
        }

        @Override
        public StockAverage merge(StockAverage stockAverage, StockAverage acc1) {
              stockAverage.sum = stockAverage.sum + acc1.sum;
              stockAverage.count = stockAverage.count + acc1.count;
            return stockAverage;
        }
    }

    public static class StockAverage {

        public String stock;
        public Integer count = 0;
        public Double sum = 0d;

        @Override
        public String toString() {
            return "StockAverage{" +
                    "stock='" + stock + '\'' +
                    ", count=" + count +
                    ", sum=" + sum +
                    '}';
        }
    }
}

class StockTransactionDeserializationSchema extends AbstractDeserializationSchema<StockTransaction> {
  private static final long serialVersionUUID = 1L;

  private transient ObjectMapper objectMapper;

  @Override
  public void open(InitializationContext context) {
    objectMapper = JsonMapper.builder().build().registerModule(new JavaTimeModule());
  }

  @Override
  public StockTransaction deserialize(byte[] message) throws IOException {
    return objectMapper.readValue(message, StockTransaction.class);
  }
}

class StockTransaction {

  public String stock;
  public double value;
  public String userID;
  public int Qty;

  public StockTransaction() {}

  public StockTransaction(String stock, double value, int Qty, String userID) {
    this.stock = stock;
    this.value = value;
    this.Qty = Qty;
    this.userID= userID;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("StockTransaction{");
    sb.append("stock=").append(stock).append('\'');
    sb.append(", value=").append(String.valueOf(value)).append('\'');
    sb.append(", Qty=").append(String.valueOf(Qty)).append('\'');
    sb.append(", userID=").append(userID).append('\'');
    return sb.toString();
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), stock, value, Qty, userID);
  }
}