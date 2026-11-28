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

fun buildReferralShareMessage(code: String): String =
    "🎁 هدية خاصة لك في تطبيق Number One للأرقام الافتراضية والخاصة VIP!\n\n" +
    "✨ احصل على رقمك الفوري أو الخاص لتفعيل واتساب وتيليجرام وفيسبوك بأعلى جودة وأسرع وصول للأكواد.\n\n" +
    "🔑 استخدم كود الدعوة الخاص بي عند التسجيل:\n👉 $code\n\n" +
    "📲 رابط تحميل التطبيق الرسمي:\nhttps://numberone-vip.github.io/numberone-app/?ref=$code\n\n" +
    "افتح الرابط وحمّل التطبيق، ثم أدخل الكود في قسم الإحالات والمكافآت! 🚀"

@Composable
fun ReferralsScreen(vm: EngagementViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val ltrStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    var inputCode by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.clearFeedback(); vm.loadMyReferralCode() }
    NOScaffold(title = tr("الإحالات والمكافآت"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Star, size = 54.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("👑 برنامج مكافآت Number One VIP"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(tr("ادعُ أصدقاءك للحصول على رصيد مكافآت إضافي وخصومات حصرية على الأرقام الفورية والخاصة."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        NOCard {
            SectionTitle(tr("قيمة المكافأة والخصم"), tr("مزايا الدعوة لك ولصديقك"))
            Text(tr("• مكافأة الداعي: رصيد مكافأة إضافي يُضاف لمحفظتك بعد تسجيل صديقك بكودك وإتمام أول عملية شحن أو شراء."), style = MaterialTheme.typography.bodySmall)
            Text(tr("• ميزة الصديق المدعو: تفعيل الكود يؤهّلك للعروض الترويجية وأكواد الخصم على باقات VIP والأرقام الفورية."), style = MaterialTheme.typography.bodySmall)
        }
        NOCard {
            SectionTitle(tr("شروط وأحكام الإحالة"), tr("قواعد احتساب المكافأة"))
            Text(tr("1. يُستخدم كود إحالة واحد فقط لكل حساب جديد."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("2. لا يُسمح باستخدام كود الدعوة الخاص بحسابك الشخصي."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("3. تُحتسب المكافأة بعد إتمام الصديق أول عملية شحن أو شراء ناجحة."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("4. الحسابات الوهمية أو المتكررة تُلغي المكافأة تلقائياً."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.CardGiftcard, size = 46.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(tr("🎁 شارك كودك واربح مكافآت!"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(tr("كل صديق يسجّل بكودك يمنحك فرصة الحصول على مكافآت حصرية ورصيد إضافي."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (vm.myReferralCode.isNotBlank()) {
                CopyField(label = tr("كود الدعوة الخاص بك • اضغط للنسخ"), value = vm.myReferralCode)
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, buildReferralShareMessage(vm.myReferralCode)) }
                        runCatching { context.startActivity(Intent.createChooser(intent, tr("مشاركة كود الدعوة"))) }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("مشاركة كود الدعوة الآن"), fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = {
                        val text = buildReferralShareMessage(vm.myReferralCode)
                        val wa = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; setPackage("com.whatsapp"); putExtra(Intent.EXTRA_TEXT, text) }
                        runCatching { context.startActivity(wa) }.onFailure {
                            val wab = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; setPackage("com.whatsapp.w4b"); putExtra(Intent.EXTRA_TEXT, text) }
                            runCatching { context.startActivity(wab) }.onFailure {
                                val fb = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                runCatching { context.startActivity(Intent.createChooser(fb, tr("مشاركة كود الدعوة"))) }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("مشاركة مباشرة عبر واتساب"), fontWeight = FontWeight.Bold)
                }
            }
        }
        NOCard {
            SectionTitle(tr("هل لديك كود إحالة من صديق؟"), tr("أدخل كود الدعوة هنا لتفعيله على حسابك فوراً."))
            OutlinedTextField(value = inputCode, onValueChange = { inputCode = it.uppercase() }, label = { Text(tr("أدخل كود الإحالة")) }, singleLine = true, textStyle = ltrStyle, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.applyReferralCode(inputCode) }, enabled = inputCode.trim().length >= 3 && !vm.loading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("تفعيل كود الإحالة"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
