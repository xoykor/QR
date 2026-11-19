# Leitor QR (Android 9+)

Aplicativo Android nativo (API 28+ / Android 9+) para identificar e ler QR codes tanto **ao vivo pela câmera** quanto **a partir de imagens**, tornando automaticamente clicáveis quaisquer links encontrados no conteúdo.

## Funcionalidades

- **Leitura ao vivo pela câmera** (`CameraX` + `ML Kit`):
  - Detecção instantânea em tempo real com destaque visual do QR code na tela.
  - Controle de **lanterna**, **alternar câmera** (traseira/frontal) e **pausar/retomar**.
- **Leitura em imagens**:
  - Seleção pela galeria (`Photo Picker`) ou gerenciador de arquivos.
  - Suporte a receber imagens compartilhadas diretamente de outros aplicativos (WhatsApp, Galeria, navegador, etc.).
  - Decodificação via `ML Kit` com fallback em `ZXing` (incluindo QR codes invertidos branco-no-preto).
  - Suporte a múltiplos QR codes na mesma imagem com destaque visual por caixa delimitadora.
- **Links clicáveis e ações rápidas**:
  - Links (`https://`, `http://`, domínios como `www.` / `github.com/...`, `mailto:`, `tel:`, `geo:`) destacados e clicáveis diretamente no texto e via botão **Abrir link**.
  - Botões para **Copiar** e **Compartilhar** o conteúdo lido.
- **Histórico local**:
  - Armazena as leituras recentes no dispositivo, preservando links clicáveis e ações rápidas.
- **Privacidade**:
  - Processamento local no dispositivo (modelo ML Kit embarcado + fallback ZXing), solicitando permissão de `CAMERA` apenas quando o usuário utiliza a leitura ao vivo.

## Requisitos

- Android 9.0 (Pie, API 28) ou superior.

## Como compilar

```bash
./gradlew testDebugUnitTest assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.
