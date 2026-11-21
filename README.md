# IthaGram

Cliente **no oficial** de Telegram para Android, basado en el código oficial de [Telegram 13.2.2](https://github.com/DrKLO/Telegram), con un diseño Material 3 y un motor de plugins propio.

> IthaGram no es la app oficial de Telegram ni está afiliada a Telegram.

## Qué trae

**Diseño Material 3**
- Lista de chats en tarjetas, con tinte tonal que sigue el color de acento del tema.
- Carpetas como chips (activa rellena, inactivas con contorno).
- Barra inferior flotante (Perfil, Ajustes, Contactos, Chats) y botón flotante tonal.
- Ícono de enviar con color adaptable y fondo redondo.

**Ajustes de Itha**
- Pantalla propia con estilos para la cabecera del chat, mensajes fijados, barra de escritura y lista de chats.
- Menú del avatar y cabecera del chat personalizados.
- Elección del ícono de la app.

**Plugins**
- Motor de plugins `.plugin` con pantalla de gestión: instalar, activar, desactivar, actualizar y borrar.
- Pantalla de ajustes por plugin.
- Instalación directa desde un archivo recibido en el chat.

## Descargar

Los APK compilados están en [Releases](../../releases). Para instalar: descargá el APK, permití "instalar apps de orígenes desconocidos" y abrilo.

## Ramas

| Rama | Uso |
|---|---|
| `main` | Versión estable |
| `dev` | Desarrollo |
| `motor-nagram` | Rama de trabajo con el motor de plugins |

Las versiones estables se marcan con tags (por ejemplo `v13.2.2-itha`).

## Compilar

Requisitos (los de Telegram 13.2.2): Android Studio 2025.1.4, Android NDK 27.2.12479018 y Android SDK 36.

1. Cloná el repo con sus submódulos:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/gabrielacjs/IthaGram.git
   ```
   Si olvidaste `--recursive`:
   ```bash
   git submodule update --init --recursive --depth=1
   ```
2. Poné tu propio `release.keystore` en `TMessagesProj/config` y completá `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS` y `RELEASE_STORE_PASSWORD` en `gradle.properties`.
3. Compilá desde la terminal:
   ```powershell
   .\gradlew :TMessagesProj_App:assembleAfatRelease
   ```
   El APK queda en `TMessagesProj_App/build/outputs/apk/afat/release/`.

También se puede abrir el proyecto en Android Studio (abrirlo, no importarlo).

**Nota:** para respetar las [compilaciones reproducibles](https://core.telegram.org/reproducible-builds), el repo base trae un `release.keystore` y un `google-services.json` de ejemplo. Antes de publicar tus propios APK, reemplazalos por los tuyos.

## Créditos y licencia

- [Telegram para Android](https://github.com/DrKLO/Telegram), de Telegram FZ-LLC, es la base de este proyecto.
- El motor de plugins se porta de [exteraGram](https://github.com/exteraSquad/exteraGram) y [Nagram](https://github.com/NextAlone/Nagram).

Este proyecto se distribuye bajo la misma licencia que Telegram para Android (GNU GPL v2, ver [LICENSE](LICENSE)), y su código fuente se publica para cumplirla.

## Requisitos de Telegram para apps derivadas

Telegram pide a todos los desarrolladores que usan su API y su código:

1. [Obtener tu propio `api_id`](https://core.telegram.org/api/obtaining_api_id).
2. No usar el nombre Telegram en la app, o dejar claro que no es oficial.
3. No usar el logo estándar de Telegram (avión de papel blanco en círculo azul).
4. Leer las [pautas de seguridad](https://core.telegram.org/mtproto/security_guidelines) y cuidar los datos de los usuarios.
5. Publicar el código propio para cumplir con las licencias.

Documentación: [API de Telegram](https://core.telegram.org/api) y [protocolo MTProto](https://core.telegram.org/mtproto).

<!--
Capturas (agregar cuando estén en docs/capturas/):
![Lista de chats](docs/capturas/lista.png)
![Ajustes de Itha](docs/capturas/ajustes.png)
![Plugins](docs/capturas/plugins.png)
-->
