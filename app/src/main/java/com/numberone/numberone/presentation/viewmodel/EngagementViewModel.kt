package com.numberone.numberone.presentation.viewmodel
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class EngagementViewModel : ViewModel() {
    private val repo = AppContainer.engagementRepository
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var unreadCount by mutableIntStateOf(0); private set
    var notifications by mutableStateOf<List<AppNotification>>(emptyList()); private set
    var myReferralCode by mutableStateOf(""); private set
    var reviewedOrderIds by mutableStateOf<Set<String>>(emptySet()); private set

    fun clearFeedback() { error = null; message = null }
    fun refreshUnreadCount() { viewModelScope.launch { repo.countUnreadNotifications().onSuccess { unreadCount = it } } }
    fun loadNotifications() { viewModelScope.launch { loading = true; error = null; try { notifications = repo.getMyNotifications().getOrThrow(); unreadCount = notifications.count { it.isUnread } } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } finally { loading = false } } }
    fun markNotificationRead(id: String) { viewModelScope.launch { try { if (repo.markNotificationRead(id).getOrThrow()) { notifications = notifications.map { if (it.id == id && it.isUnread) it.copy(readAt = "read") else it }; unreadCount = notifications.count { it.isUnread } } } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } } }
    fun markAllNotificationsRead() { viewModelScope.launch { loading = true; error = null; message = null; try { repo.markAllNotificationsRead().getOrThrow(); notifications = notifications.map { if (it.isUnread) it.copy(readAt = "read") else it }; unreadCount = 0; message = tr("تم تحديد جميع الإشعارات كمقروءة") } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } finally { loading = false } } }
    fun loadMyReferralCode() { viewModelScope.launch { loading = true; error = null; try { myReferralCode = repo.getOrCreateMyReferralCode().getOrThrow() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } finally { loading = false } } }
    fun applyReferralCode(code: String) { viewModelScope.launch { loading = true; error = null; message = null; try { if (repo.applyReferralCode(code).getOrThrow()) message = tr("تم تطبيق كود الإحالة بنجاح!") else error = tr("تعذّر تطبيق كود الإحالة") } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } finally { loading = false } } }
    suspend fun previewPromoCode(code: String, amount: Double): Result<PromoValidationResult> = repo.validatePromoCode(code, amount)
    fun submitOrderReview(orderId: String, rating: Int, reviewText: String?, onDone: () -> Unit = {}) { viewModelScope.launch { loading = true; error = null; message = null; try { repo.submitOrderReview(orderId, rating, reviewText).getOrThrow(); reviewedOrderIds = reviewedOrderIds + orderId; message = tr("شكراً لك! تم إرسال تقييمك بنجاح"); onDone() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = ErrorMapper.toUserMessage(e) } finally { loading = false } } }
}
