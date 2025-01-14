package vn.sesgroup.hddt.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.SessionSynchronization;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;

@Configuration
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = { "vn.nhatrovn" }, considerNestedRepositories = true)
public class MongoConfiguration extends AbstractMongoClientConfiguration{
	
	@Value("${spring.data.mongodb.application.name}")
	private String applicationName;
	
	@Value("${spring.data.mongodb.database}")
	private String dbName;

	@Value("${spring.data.mongodb.host}")
	private String dbHost;

	@Value("${spring.data.mongodb.port}")
	private int dbPort;
	
	@Value("${spring.data.mongodb.username}")
	private String userName;
	
	@Value("${spring.data.mongodb.password}")
	private String password;

	@Override
	protected String getDatabaseName() {
		return dbName;
	}

	private MongoClientSettings getMongoClientSettings() {
		MongoCredential credential = MongoCredential.createCredential(userName, dbName, password.toCharArray());
		List<ServerAddress> serverAddressList = new ArrayList<>();
		serverAddressList.add(new ServerAddress(dbHost, dbPort));

		return MongoClientSettings.builder()
				/*
				 * Sets the logical name of the application using this MongoClient. The
				 * application name may be used by the client to identifythe application to the
				 * server, for use in server logs, slow query logs, and profile collection.
				 */
				.applicationName(applicationName).applyToClusterSettings(builder -> builder.hosts(serverAddressList))
				.credential(credential)

				.uuidRepresentation(UuidRepresentation.STANDARD)
				.applyToSslSettings(
						sslBuilder -> SslSettings.builder().enabled(false).invalidHostNameAllowed(false).build())
				.applyToConnectionPoolSettings(connPoolBuilder -> ConnectionPoolSettings.builder()
//						.maxWaitTime(2, TimeUnit.SECONDS).minSize(2).maxSize(25).build())
						.maxWaitTime(2, TimeUnit.SECONDS).minSize(5).maxSize(100)
						.build())
				.applyToSocketSettings(
						socketBuilder -> SocketSettings.builder().connectTimeout(5, TimeUnit.SECONDS).build())
				/* .readPreference(ReadPreference.secondaryPreferred()) */.build();
	}

	@Bean
	public MongoClient mongoClient() {
		MongoClientSettings mongoSettingsProperties = getMongoClientSettings();
		MongoClient mongoClient = MongoClients.create(mongoSettingsProperties);
		return mongoClient;
	}

	@Bean
	public MongoDatabaseFactory mongoDbFactory() {
		return new SimpleMongoClientDatabaseFactory(mongoClient(), dbName);
	}

	@Bean
	public MongoTemplate mongoTemplate() {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
		MappingMongoConverter mappingMongoConverter = new MappingMongoConverter(dbRefResolver,
				new MongoMappingContext());
		// Don't save _class to mongo
		mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
		MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory(), mappingMongoConverter);
		mongoTemplate.setSessionSynchronization(SessionSynchronization.ON_ACTUAL_TRANSACTION);
		
		mongoTemplate.setReadPreference(ReadPreference.secondaryPreferred());
		mongoTemplate.setWriteConcernResolver(writeConcernResolver());
		
		return mongoTemplate;
	}

	public MongoTemplate fetchMongoTemplate(int projectId) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
		MappingMongoConverter mappingMongoConverter = new MappingMongoConverter(dbRefResolver,
				new MongoMappingContext());
		// Don't save _class to mongo
		mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));

		MongoDatabaseFactory customizedDBFactory = new SimpleMongoClientDatabaseFactory(mongoClient(), dbName);
		MongoTemplate mongoTemplate = new MongoTemplate(customizedDBFactory, mappingMongoConverter);
//	      MongoTransactionManager mongoTransactionManager = new MongoTransactionManager(customizedDBFactory);
//		mongoTemplate.setSessionSynchronization(SessionSynchronization.ON_ACTUAL_TRANSACTION);
		
		return mongoTemplate;
	}

	@Bean
	public MongoTransactionManager mongoTransactionManager() {
		return new MongoTransactionManager(mongoDbFactory());
	}
	
	private WriteConcernResolver writeConcernResolver() {
        return action -> {
//            if (action.getCollectionName()
//                    .equals("your_collecton")
//                    && (action.getMongoActionOperation() == MongoActionOperation.SAVE
//                            || action.getMongoActionOperation() == MongoActionOperation.UPDATE)) {
//                return WriteConcern.MAJORITY;
//            }
            return action.getDefaultWriteConcern();
        };
    }
	
}
