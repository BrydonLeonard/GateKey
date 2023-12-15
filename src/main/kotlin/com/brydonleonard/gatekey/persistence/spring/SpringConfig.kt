package com.brydonleonard.gatekey.persistence.spring

import com.brydonleonard.gatekey.Config
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.DbMigrationModel
import com.brydonleonard.gatekey.persistence.model.FeedbackModel
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.model.MetricModel
import com.brydonleonard.gatekey.persistence.model.MqttTopicModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.support.ConnectionSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringConfig {
    @Bean(destroyMethod = "close")
    fun connectionSource(config: Config): JdbcPooledConnectionSource =
            JdbcPooledConnectionSource("jdbc:sqlite:${config.dbPath}")

    @Bean
    fun migrationDao(connectionSource: ConnectionSource): Dao<DbMigrationModel, Int> =
            DaoManager.createDao(connectionSource, DbMigrationModel::class.java)

    @Bean
    fun conversationDao(connectionSource: ConnectionSource): Dao<ConversationStepModel, Long> =
            DaoManager.createDao(connectionSource, ConversationStepModel::class.java)

    @Bean
    fun keyDao(connectionSource: ConnectionSource): Dao<KeyModel, String> =
            DaoManager.createDao(connectionSource, KeyModel::class.java)

    @Bean
    fun userDao(connectionSource: ConnectionSource): Dao<UserModel, String> =
            DaoManager.createDao(connectionSource, UserModel::class.java)

    @Bean
    fun userRegistrationTokenDao(connectionSource: ConnectionSource): Dao<UserRegistrationTokenModel, String> =
            DaoManager.createDao(connectionSource, UserRegistrationTokenModel::class.java)

    @Bean
    fun metricDao(connectionSource: ConnectionSource): Dao<MetricModel, Long> =
            DaoManager.createDao(connectionSource, MetricModel::class.java)

    @Bean
    fun feedbackDao(connectionSource: ConnectionSource): Dao<FeedbackModel, Int> =
            DaoManager.createDao(connectionSource, FeedbackModel::class.java)

    @Bean
    fun householdDao(connectionSource: ConnectionSource): Dao<HouseholdModel, String> =
            DaoManager.createDao(connectionSource, HouseholdModel::class.java)

    @Bean
    fun mqttTopicDao(connectionSource: ConnectionSource): Dao<MqttTopicModel, String> =
            DaoManager.createDao(connectionSource, MqttTopicModel::class.java)
}