package com.numberone.app.presentation.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.TealAccent

private data class SupportSection(
    val title: String,
    val content: String
)

@Composable
fun SupportScreen(
    strings: Strings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = remember(language) {
        if (language == AppLanguage.ARABIC) {
            listOf(
                SupportSection(
                    title = strings.privacyPolicy,
                    content = """
                        نلتزم في تطبيق Number One بحماية خصوصيتك وأمان بياناتك الشخصية:
                        1. البيانات التي نجمعها: نجمع فقط البريد الإلكتروني أو معرف الحساب، وسجل الطلبات، وإيصالات الدفع المرفوعة لغرض التحقق اليدوي وتفعيل الخدمة.
                        2. استخدام البيانات: تُستخدم بياناتك حصرياً لمعالجة طلبات الأرقام الافتراضية، وتحديث حالة الطلب، وإرسال إشعارات أكواد التفعيل.
                        3. حماية البيانات: يتم تخزين جميع البيانات بشكل مشفر عبر خوادم Supabase الآمنة، ولا نشارك أو نبيع بياناتك لأي طرف ثالث إطلاقاً.
                        4. حذف الحساب: يمكنك طلب حذف حسابك وبياناتك في أي وقت عبر التواصل مع فريق الدعم.
                    """.trimIndent()
                ),
                SupportSection(
                    title = strings.termsOfUse,
                    content = """
                        باستخدامك لتطبيق Number One فإنك توافق على الشروط التالية:
                        1. الاستخدام المشروع: تُستخدم الأرقام الافتراضية لأغراض التفعيل المشروع للحسابات الشخصية والتجارية فقط، ويُحظر استخدامها في أي نشاط غير قانوني أو احتيالي.
                        2. المراجعة اليدوية: تخضع طلبات الشراء والشحن للمراجعة اليدوية من قبل الإدارة لضمان دقة التحويلات، ويتم تنفيذ الطلبات خلال أوقات العمل بأسرع وقت ممكن.
                        3. سياسة الاسترداد: في حال رفض الطلب أو عدم وصول رسالة التفعيل (SMS) خلال المدة المحددة، يتم حفظ أو استرداد قيمة الطلب إلى رصيد محفظتك داخل التطبيق.
                        4. مسؤولية الحساب: التطبيق غير مسؤول عن حظر الحسابات الناتج عن مخالفة شروط المنصات الخارجية (مثل واتساب أو تيليجرام) بعد استلام الكود بنجاح.
                    """.trimIndent()
                ),
                SupportSection(
                    title = strings.faq,
                    content = """
                        • كم يستغرق تفعيل الطلب؟
                        تتم مراجعة الطلبات يدوياً حسب توفر الخدمة وحالة الدفع.

                        • ماذا يحدث إذا لم يصلني كود التفعيل؟
                        لا تقلق، رصيدك محمي بالكامل؛ سيتم تعويضك برقم بديل أو استرداد المبلغ لمحفظتك.

                        • كيف أتأكد من قبول إيصال الدفع؟
                        يرجى رفع صورة واضحة للإيصال مع كتابة رقم المرجع إن وُجد، وستصلك إشعارات فورية فور المراجعة.
                    """.trimIndent()
                )
            )
        } else {
            listOf(
                SupportSection(
                    title = strings.privacyPolicy,
                    content = "Number One respects your privacy. We only collect account identifiers, order history, and payment receipts for manual verification. All data is securely stored on Supabase and never sold to third parties."
                ),
                SupportSection(
                    title = strings.termsOfUse,
                    content = "Virtual numbers must be used for legitimate verification purposes only. Manual orders are reviewed promptly. If an SMS code is not delivered, your balance is protected and refunded to your in-app wallet."
                ),
                SupportSection(
                    title = strings.faq,
                    content = "• Approval Time: Orders are reviewed based on service availability and payment status.\n• No SMS Received: Your wallet balance is automatically protected.\n• Payment Proof: Upload a clear receipt screenshot for faster approval."
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedButton(
                onClick = onToggleLanguage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(text = strings.switchLanguage, fontWeight = FontWeight.Bold)
            }
        }

        items(sections) { section ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = section.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onContactClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Color(0xFF091321)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = strings.contactSupport,
                    color = Color(0xFF091321),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
