package com.brydonleonard.gatekey.persistence.ext

import com.brydonleonard.gatekey.auth.Permissions
import com.j256.ormlite.field.FieldType
import com.j256.ormlite.field.SqlType
import com.j256.ormlite.field.types.StringType

/**
 * Allows ORMLite to de/serialize sets of permissions into comma-separated lists of strings
 */
object PermissionsSetPersister : StringType(SqlType.STRING, arrayOf(Permissions::class.java)) {
    @JvmStatic
    fun getSingleton() = this

    override fun javaToSqlArg(fieldType: FieldType?, javaObject: Any): Any {
        if (javaObject !is Set<*>) {
            throw IllegalArgumentException("Can't turn a ${javaObject::class} into a persistable string")
        }
        if (javaObject.any { it !is Permissions }) {
            throw IllegalArgumentException("Can only persist sets of Permissions")
        }

        return javaObject.joinToString(",") { (it as Permissions).name }
    }

    override fun sqlArgToJava(fieldType: FieldType?, sqlArg: Any?, columnPos: Int): Any {
        sqlArg ?: throw IllegalArgumentException("Can't parse a null sql arg")
        if (sqlArg !is String) {
            throw IllegalArgumentException("Can't parse a ${sqlArg::class} into a set of permissions")
        }

        val stringRepresentation: String = sqlArg

        return stringRepresentation.split(",").map { Permissions.valueOf(it) }.toSet()
    }
}