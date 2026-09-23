package top.kafuumiaki.animegirlsdownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.kafuumiaki.animegirlsdownloader.R

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Boolean, (Boolean) -> Unit) -> Unit,
) {
    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun submit() {
        if (userName.isBlank() || password.isBlank() || loading) return
        loading = true
        failed = false
        onSubmit(userName, password, register) { success -> loading = false; failed = !success }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(if (register) R.string.register_title else R.string.login_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text(stringResource(R.string.login_username)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    isError = failed,
                    supportingText = {
                        when {
                            failed -> Text(stringResource(if (register) R.string.register_failed else R.string.login_failed))
                            else -> Text(stringResource(R.string.login_enter_to_submit))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = register, onCheckedChange = { register = it }, enabled = !loading)
                    Text(stringResource(R.string.action_register))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text(stringResource(R.string.action_cancel)) } },
        confirmButton = {
            Button(onClick = { submit() }, enabled = !loading && userName.isNotBlank() && password.isNotBlank()) {
                if (loading) CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text(stringResource(if (register) R.string.action_register else R.string.action_login))
            }
        },
    )
}
