package com.xoykor.qr.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object IntentUtils {

    fun openUrl(context: Context, url: String): Boolean {
        val normalized = LinkDetector.normalizeUrl(url)
        val uri = Uri.parse(normalized)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Nenhum aplicativo encontrado para abrir este link.",
                Toast.LENGTH_SHORT
            ).show()
            false
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Não foi possível abrir o link.",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Conteúdo do QR") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar conteúdo do QR")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
