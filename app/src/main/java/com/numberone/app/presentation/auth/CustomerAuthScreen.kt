package com.numberone.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CustomerAuthScreen(
    email:String,password:String,isSignUp:Boolean,loading:Boolean,error:String?,message:String?,
    onEmailChange:(String)->Unit,onPasswordChange:(String)->Unit,onToggleMode:()->Unit,onSubmit:()->Unit,
    modifier:Modifier=Modifier
){
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(22.dp),verticalArrangement=Arrangement.Center){
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primary)){
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Text("N1  /  NUMBER ONE",color=Color(0xFF8FE6DC),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                Text("YOUR NUMBERS. YOUR CONTROL.",color=Color.White,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold)
                Text("Virtual connectivity, managed in one place.",color=Color.White.copy(alpha=.75f),style=MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(25.dp))
        Text(if(isSignUp)"إنشاء حساب جديد" else "أهلاً بعودتك",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text(if(isSignUp)"أنشئ حسابك للوصول إلى المتجر والمحفظة." else "سجّل الدخول لمتابعة أرقامك وطلباتك.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(value=email,onValueChange=onEmailChange,modifier=Modifier.fillMaxWidth(),label={Text("البريد الإلكتروني")},singleLine=true,shape=RoundedCornerShape(13.dp))
        Spacer(Modifier.height(11.dp))
        OutlinedTextField(value=password,onValueChange=onPasswordChange,modifier=Modifier.fillMaxWidth(),label={Text("كلمة المرور")},visualTransformation=PasswordVisualTransformation(),singleLine=true,shape=RoundedCornerShape(13.dp))
        if(error!=null){Spacer(Modifier.height(10.dp));Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
        if(message!=null){Spacer(Modifier.height(10.dp));Text(message,color=Color(0xFF13865F),style=MaterialTheme.typography.bodySmall)}
        Spacer(Modifier.height(18.dp))
        Button(onClick=onSubmit,enabled=!loading&&email.isNotBlank()&&password.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){
            if(loading)CircularProgressIndicator() else Text(if(isSignUp)"إنشاء الحساب" else "تسجيل الدخول",fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick=onToggleMode,enabled=!loading,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){
            Text(if(isSignUp)"لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حساباً")
        }
    }
}