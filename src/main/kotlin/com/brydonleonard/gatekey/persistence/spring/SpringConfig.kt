package com.brydonleonard.gatekey.persistence.spring

import com.brydonleonard.gatekey.Config
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
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
    fun conversationDao(connectionSource: ConnectionSource): Dao<ConversationStepModel, String> =
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
}