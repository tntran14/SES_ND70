package vn.sesgroup.hddt.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;


@Component
@EnableMongoRepositories(basePackages = { "vn.nhatrovn" }, considerNestedRepositories = true)
public class ConfigConnectMongo {

	@Value("${spring.data.mongodb.application.name}")
	private String applicationName;
	
	@Value("${spring.data.mongodb.database}")
	public String dbName;

	@Value("${spring.data.mongodb.host}")
	private String dbHost;

	@Value("${spring.data.mongodb.port}")
	private int dbPort;
	
	@Value("${spring.data.mongodb.username}")
	private String userName;
	
	@Value("${spring.data.mongodb.password}")
	private String password;


	protected String getDatabaseName() {
		return dbName;
	}
	
	
	public MongoClient mongoClient() {
		MongoClientSettings mongoSettingsProperties = getMongoClientSettings();
		MongoClient mongoClient = MongoClients.create(mongoSettingsProperties);
		return mongoClient;
		
}

	private MongoClientSettings getMongoClientSettings() {
		MongoCredential credential = MongoCredential.createCredential(userName, dbName, password.toCharArray());
		List<ServerAddress> serverAddressList = new ArrayList<>();
		serverAddressList.add(new ServerAddress(dbHost, dbPort));
		return MongoClientSettings.builder()
				.applicationName(applicationName).applyToClusterSettings(builder -> builder.hosts(serverAddressList))
				.credential(credential)
				.uuidRepresentation(UuidRepresentation.STANDARD)
				.applyToSslSettings(
						sslBuilder -> SslSettings.builder().enabled(false).invalidHostNameAllowed(false).build())
				.applyToConnectionPoolSettings(connPoolBuilder -> ConnectionPoolSettings.builder()
						.maxConnectionIdleTime(10, TimeUnit.SECONDS)
						.maxWaitTime(2, TimeUnit.SECONDS).minSize(5).maxSize(100).build())						
				.applyToSocketSettings(
						socketBuilder -> SocketSettings.builder().connectTimeout(1, TimeUnit.SECONDS).build())
				.build();
	}
	
	
	
}
