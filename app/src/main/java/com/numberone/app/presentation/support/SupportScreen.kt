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

private data class SupportSection(val title: String, val content: String)

@Composable
fun SupportScreen(
    strings: Strings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = language == AppLanguage.ARABIC
    val sections = remember(language) {
        if (isArabic) {
            listOf(
                SupportSection(
                    strings.privacyPolicy,
                    "نستخدم بيانات الحساب الأساسية، وسجل الطلبات، وإيصال الدفع الذي تختار رفعه لمعالجة الطلب ومراجعته. لا تضع كلمات مرور حساباتك الخارجية أو مفاتيحك السرية في التطبيق. لطلب مساعدة تتعلق ببياناتك، تواصل مع الدعم."
                ),
                SupportSection(
                    strings.termsOfUse,
                    "الأسعار والكميات المعروضة تخص الخدمات المتاحة وقت عرضها وقد تتغير. لا يُعد الطلب مقبولاً نهائياً إلا بعد مراجعة الدفع وتأكيد الإدارة. استخدام الرقم لدى المنصات الخارجية يخضع لشروط تلك المنصات، ولا نضمن قبولها للرقم أو استمرار الحساب لديها."
                ),
                SupportSection(
                    strings.faq,
                    "• متى يتم تأكيد الطلب؟\nيظهر تحديث حالة الطلب في تبويب «طلباتي» بعد مراجعة الإدارة.\n\n• هل الدفع متاح الآن؟\nالدفع غير مفعّل حالياً. لا تحوّل أي مبلغ حتى تظهر تعليمات الدفع الرسمية داخل التطبيق.\n\n• أين أجد رقم التفعيل؟\nعند إسناد رقم إلى طلبك، ستظهر بياناته وتحديثاته في تبويب «طلباتي»."
                )
            )
        } else {
            listOf(
                SupportSection(
                    strings.privacyPolicy,
                    "We use basic account details, order history, and any payment receipt you choose to upload to process and review requests. Do not enter passwords for external accounts or secret keys in the app. Contact support for help with your account data."
                ),
                SupportSection(
                    strings.termsOfUse,
                    "Displayed prices and stock apply to the services shown at the time and may change. An order is not finally approved until payment is reviewed and the administrator confirms it. Use on third-party platforms is subject to their terms; acceptance of a number or continued account access is not guaranteed."
                ),
                SupportSection(
                    strings.faq,
                    "• When is an order confirmed?\nOrder status updates appear in the Orders tab after administrator review.\n\n• Are payments available now?\nPayments are currently disabled. Do not transfer funds until official payment instructions appear in the app.\n\n• Where is my activation number?\nWhen a number is assigned to your order, its details and updates appear in the Orders tab."
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
                Text(strings.switchLanguage, fontWeight = FontWeight.Bold)
            }
        }

        items(sections) { section ->
            var expanded by remember(section.title) { mutableStateOf(false) }
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
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = strings.contactSupport,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
