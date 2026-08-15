package com.dearly.app.data.repository

import androidx.room.withTransaction
import com.dearly.app.data.local.ContactDao
import com.dearly.app.data.local.ContactEntity
import com.dearly.app.data.local.DearlyDatabase
import com.dearly.app.data.remote.ContactRequest
import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.domain.model.CallMethod
import com.dearly.app.domain.model.Contact
import com.dearly.app.domain.model.NewContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val api: DearlyApi,
    private val database: DearlyDatabase,
    private val dao: ContactDao
) {
    fun observe(elderId: String): Flow<List<Contact>> =
        dao.observe(elderId).map { items -> items.map(ContactEntity::toDomain) }

    suspend fun refresh(elderId: String?) {
        val remote = api.contacts(elderId)
        val resolvedElderId = elderId ?: remote.firstOrNull()?.elderId ?: return
        database.withTransaction {
            dao.deleteForElder(resolvedElderId)
            dao.upsertAll(remote.map { it.toEntity() })
        }
    }

    suspend fun create(elderId: String?, item: NewContact) {
        val created = api.createContact(
            ContactRequest(
                elderId = elderId, nickname = item.nickname, fullName = item.fullName,
                phoneNumber = item.phoneNumber, relationship = item.relationship,
                callMethod = item.callMethod.name
            )
        )
        dao.upsertAll(listOf(created.toEntity()))
    }
}

private fun ContactEntity.toDomain() = Contact(
    id, elderId, nickname, fullName, phoneNumber, relationship,
    runCatching { CallMethod.valueOf(callMethod) }.getOrDefault(CallMethod.PHONE)
)

private fun com.dearly.app.data.remote.ContactDto.toEntity() = ContactEntity(
    id, elderId, nickname, fullName, phoneNumber, relationship, callMethod
)
