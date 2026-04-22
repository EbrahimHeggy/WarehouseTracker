package com.example.warehousetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehousetracker.AmberColor
import com.example.warehousetracker.GreenColor
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.RedColor
import com.example.warehousetracker.data.model.UserProfile
import com.example.warehousetracker.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onImportClick: () -> Unit = {},
    onRegisterUserClick: () -> Unit = {} // المعامل المفقود تم إضافته
) {

    LaunchedEffect(Unit) {
        authViewModel.loadAllUsers()
    }
    val context = LocalContext.current
    val authState by authViewModel.state.collectAsState()
    val profile = authState.profile

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

    var successMsg by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
            .statusBarsPadding()
            .navigationBarsPadding() // AVOID SYSTEM BUTTONS
    ) {
        // Header
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text(
                "My Profile",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Avatar + Name
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(NavyBlue.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = profile?.name?.split(" ")
                            ?.filter { it.isNotBlank() }?.take(2)
                            ?.map { it.first().uppercase() }?.joinToString("") ?: "?"
                        Text(
                            initials,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyBlue
                        )
                    }
                    Text(
                        profile?.name ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (profile?.role == "admin") NavyBlue.copy(0.1f) else GreenColor.copy(
                                    0.1f
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (profile?.role == "admin") "Admin" else "Employee",
                            fontSize = 12.sp,
                            color = if (profile?.role == "admin") NavyBlue else GreenColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Admin Tools
                if (profile?.role == "admin") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onRegisterUserClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                            ) {
                                Icon(
                                    Icons.Default.PersonAddAlt,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Create New User Account")
                            }
                            OutlinedButton(
                                onClick = onImportClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileUpload,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Import Employees from Excel")
                            }
                        }
                    }

                    // Users List
                    Text(
                        "Manage App Users",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyBlue
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            authState.allUsers.filter { it.uid != profile.uid }.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .background(NavyBlue.copy(0.05f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            user.name.take(1).uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyBlue
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            user.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(user.email, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { userToDelete = user }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            null,
                                            tint = RedColor.copy(0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (user != authState.allUsers.lastOrNull()) HorizontalDivider(
                                    color = Color.Gray.copy(
                                        0.05f
                                    )
                                )
                            }
                        }
                    }
                }

                // Account Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Personal Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyBlue
                        )
                        HorizontalDivider(color = Color.Gray.copy(0.1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                null,
                                tint = NavyBlue.copy(0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("Email", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    profile?.email ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    null,
                                    tint = NavyBlue.copy(0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Password", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        "••••••••",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(onClick = { showChangePasswordDialog = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = NavyBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Security Actions
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor)
                ) {
                    Icon(Icons.Default.Mail, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send Reset Link to Email")
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // --- Dialogs ---

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete '${userToDelete?.name}' from database?") },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.deleteUser(userToDelete!!.uid)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false; newPassword = ""; confirmPassword = ""; errorMsg =
                ""
            },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                showNewPassword = !showNewPassword
                            }) {
                                Icon(
                                    if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                showConfirmPassword = !showConfirmPassword
                            }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                    if (errorMsg.isNotEmpty()) Text(errorMsg, color = Color.Red, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) errorMsg = "Min 6 characters"
                        else if (newPassword != confirmPassword) errorMsg = "No match"
                        else {
                            authViewModel.changePassword(newPassword) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT)
                                        .show()
                                    showChangePasswordDialog = false
                                } else errorMsg = error ?: "Failed"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false; authViewModel.clearResetState() },
            title = { Text("Reset Password") },
            text = { Text(if (authState.resetEmailSent) "Email sent to ${profile?.email}" else "Send reset link to ${profile?.email}?") },
            confirmButton = {
                if (!authState.resetEmailSent) {
                    Button(onClick = { profile?.email?.let { authViewModel.resetPassword(it) } }) {
                        Text(
                            "Send"
                        )
                    }
                } else {
                    Button(onClick = {
                        showResetDialog = false; authViewModel.clearResetState()
                    }) { Text("Done") }
                }
            }
        )
    }
}