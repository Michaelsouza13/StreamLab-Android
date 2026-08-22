# Streamlab TV 📺

Aplicativo nativo para Android TV construído para streaming de listas IPTV/M3U, focado 100% no uso com controle remoto (D-Pad).

## Stack Tecnológica 🛠
- **Kotlin** (100%)
- **Jetpack Compose for TV** (`androidx.tv.material3`) para interfaces otimizadas para Leanback.
- **Media3 (ExoPlayer)** integrado com Compose, configurado para HLS/M3U8.
- **Room Database** e **DataStore** para persistência local (playlists e histórico).
- **Retrofit & OkHttp** (pronto para integração TMDB e chamadas de rede).
- **Hilt** para Injeção de Dependência.
- **MVVM** e **Clean Architecture**.

## Funcionalidades Implementadas
- **Navegação D-Pad**: Suporte nativo para controle remoto.
- **Parse M3U**: Lógica robusta para ingestão de listas.
- **Grid de Canais TV**: Exibição em grid carrossel com auto-foco e tratamentos visuais do Material 3 TV.
- **Player de Vídeo Nativo**: Overlay adaptativo e resposta direta aos eventos do controle remoto.