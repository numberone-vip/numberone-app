package com.numberone.numberone.presentation.screens.referrals
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.presentation.viewmodel.EngagementViewModel

@Composable
fun ReferralsScreen(vm: EngagementViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val ltrStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    var inputCode by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.clearFeedback(); vm.loadMyReferralCode() }
    NOScaffold(title = tr("نظام الإحالات"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.CardGiftcard, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(tr("كود الإحالة الخاص بك"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(tr("شارك كودك مع أصدقائك للحصول على مكافآت الإحالة."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (vm.myReferralCode.isNotBlank()) {
                CopyField(label = tr("كود الدعوة"), value = vm.myReferralCode)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "سجّل في تطبيق Number One واستخدم كود الإحالة الخاص بي: ${vm.myReferralCode}") }
                        runCatching { context.startActivity(Intent.createChooser(intent, tr("مشاركة كود الإحالة"))) }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tr("مشاركة الكود"), fontWeight = FontWeight.Bold)
                }
            }
        }
        NOCard {
            SectionTitle(tr("هل لديك كود إحالة من صديق؟"), tr("أدخل كود الإحالة لتفعيله على حسابك."))
            OutlinedTextField(value = inputCode, onValueChange = { inputCode = it.uppercase() }, label = { Text(tr("كود الإحالة")) }, singleLine = true, textStyle = ltrStyle, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.applyReferralCode(inputCode) }, enabled = inputCode.trim().length >= 3 && !vm.loading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("تطبيق كود الإحالة"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
