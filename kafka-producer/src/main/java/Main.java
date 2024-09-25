import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.github.javafaker.Faker;
import com.google.gson.Gson;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class Main {

  private static final String KAFKA_NODES = "kafka:9092";
  private static final String TOPIC = "stockTicker";
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws InterruptedException {
    while (true) {
      genData();
      Thread.sleep(10000); // Sleep for 10 seconds
    }
  }

  private static void genData() {
    Faker faker = new Faker();
    Random random = new Random();

    // Setting Kafka producer properties
    Properties props = new Properties();
    props.put("bootstrap.servers", KAFKA_NODES);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    // Creating the producer
    Producer<String, String> producer = new KafkaProducer<>(props);

    // Generating data
    StockTransData stockTransData = new StockTransData(faker.company().name(), random.nextDouble() * (100.0),
        random.nextInt(250), faker.name().firstName());
    String dataJson = gson.toJson(stockTransData);
    System.out.println(dataJson);

    // Sending data to the topic
    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, dataJson);

    try {
      RecordMetadata metadata = producer.send(record).get();
      System.out.printf("Sent message to topic %s, partition %d, offset %d%n", metadata.topic(), metadata.partition(),
          metadata.offset());
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }

    producer.close();
  }

  // Data model class for Stock Transaction data
  static class StockTransData {

    private final String stock;
    private final double value;
    private final String userID;
    private final int Qty;

    public StockTransData(String stock, double value, int Qty, String userID) {
      this.stock = stock;
      this.value = value;
      this.Qty = Qty;
      this.userID = userID;
    }

    public String getUserID() {
      return userID;
    }

    public int getQty() {
      return Qty;
    }

    public double getValue() {
      return value;
    }

    public String getStock() {
      return stock;
    }

  }
}
