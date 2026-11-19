package com.xoykor.qr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xoykor.qr.model.DetectedLink
import com.xoykor.qr.model.QrContentType
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.util.IntentUtils

@Composable
fun ClickableQrText(
    text: String,
    links: List<DetectedLink>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, links, linkColor) {
        buildAnnotatedString {
            append(text)
            for (link in links) {
                if (link.startIndex >= 0 && link.endIndex <= text.length && link.startIndex < link.endIndex) {
                    addLink(
                        url = LinkAnnotation.Url(
                            url = link.normalizedUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = {
                                onOpenUrl(link.normalizedUrl)
                            }
                        ),
                        start = link.startIndex,
                        end = link.endIndex
                    )
                }
            }
        }
    }

    SelectionContainer(modifier = modifier) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrResultCard(
    result: QrScanResult,
    modifier: Modifier = Modifier,
    onRescanClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val primaryUrl = result.primaryUrl
    val hasLink = !primaryUrl.isNullOrBlank() || result.links.isNotEmpty()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = iconForType(result.contentType),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = result.contentType.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = result.source.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ) {
                ClickableQrText(
                    text = result.displayValue,
                    links = result.links,
                    onOpenUrl = { url -> IntentUtils.openUrl(context, url) },
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (hasLink) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toque no link destacado acima ou no botão abaixo para abrir.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.links.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    result.links.forEach { link ->
                        AssistChip(
                            onClick = { IntentUtils.openUrl(context, link.normalizedUrl) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = link.displayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targetUrl = primaryUrl ?: result.links.firstOrNull()?.normalizedUrl
                if (targetUrl != null) {
                    Button(
                        onClick = { IntentUtils.openUrl(context, targetUrl) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir link")
                    }
                }

                FilledTonalButton(
                    onClick = { IntentUtils.copyToClipboard(context, result.rawValue) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar")
                }

                FilledTonalButton(
                    onClick = { IntentUtils.shareText(context, result.rawValue) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartilhar")
                }

                if (onRescanClick != null) {
                    TextButton(onClick = onRescanClick) {
                        Text("Escanear outro")
                    }
                }
            }
        }
    }
}

private fun iconForType(type: QrContentType) = when (type) {
    QrContentType.URL -> Icons.Rounded.Link
    QrContentType.EMAIL -> Icons.Rounded.Email
    QrContentType.PHONE -> Icons.Rounded.Phone
    QrContentType.SMS -> Icons.Rounded.Sms
    QrContentType.WIFI -> Icons.Rounded.Wifi
    QrContentType.GEO -> Icons.Rounded.LocationOn
    QrContentType.TEXT -> Icons.Rounded.QrCode2
}
