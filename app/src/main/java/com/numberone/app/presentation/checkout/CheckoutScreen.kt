package com.numberone.app.presentation.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.domain.model.VirtualService

@Composable
fun CheckoutScreen(service:VirtualService,strings:Strings,language:AppLanguage,loading:Boolean,error:String?,onConfirmWalletPurchase:()->Unit,onCancel:()->Unit,modifier:Modifier=Modifier){
    val country=if(language==AppLanguage.ARABIC)service.countryNameAr else service.countryNameEn
    Column(modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text(if(language==AppLanguage.ARABIC)"تأكيد الطلب" else "Confirm your order",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(language==AppLanguage.ARABIC)"راجع تفاصيل الرقم قبل إتمام الشراء." else "Review the number details before completing your purchase.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(language==AppLanguage.ARABIC)"الخدمة" else "Service",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(service.appName,fontWeight=FontWeight.Bold)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(language==AppLanguage.ARABIC)"الدولة" else "Country",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(service.countryFlag+" "+country,fontWeight=FontWeight.Bold)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(language==AppLanguage.ARABIC)"المتاح" else "Available",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(service.availableCount.toString())}
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(language==AppLanguage.ARABIC)"الإجمالي" else "Total",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(String.format("%.2f USDT",service.priceUsd),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.ExtraBold)}
            }
        }
        Text(if(language==AppLanguage.ARABIC)"سيتم خصم المبلغ من محفظتك. بعدها يظهر طلبك في صفحة طلباتي بانتظار تخصيص الرقم." else "The amount will be deducted from your wallet. Your order will then appear in My Orders while the number is assigned.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(error!=null)Text(error,color=MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        Button(onClick=onConfirmWalletPurchase,enabled=!loading&&service.availableCount>0,modifier=Modifier.fillMaxWidth().height(53.dp),shape=RoundedCornerShape(14.dp)){
            if(loading)CircularProgressIndicator() else Text(if(language==AppLanguage.ARABIC)"تأكيد الشراء من المحفظة" else "Pay with wallet",fontWeight=FontWeight.Bold)
        }
        OutlinedButton(onClick=onCancel,enabled=!loading,modifier=Modifier.fillMaxWidth()){Text(if(language==AppLanguage.ARABIC)"رجوع" else "Back")}
    }
}