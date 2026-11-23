# CART3R VPN Blocker

Aplicación Android en Kotlin que crea una **VPN local de bloqueo** (firewall tipo interruptor). No utiliza servidores VPN ni enruta el tráfico a terceros: descarta los paquetes de red mientras está activada.

## Funciones

- Activar/desactivar el bloqueo desde la app.
- Botón flotante movible para activar/desactivar.
- Tamaño del botón ajustable (40–120 dp).
- Interfaz ligera, sin librerías externas ni procesos de análisis en segundo plano.
- GitHub Actions para generar un APK de prueba.

## Importante

- **No es una VPN de privacidad**: no cambia tu IP, no cifra el tráfico hacia un servidor VPN ni oculta tu ubicación.
- Android muestra un permiso del sistema para crear la VPN. Solo puede haber una VPN activa por usuario/perfil; puede entrar en conflicto con otra VPN o con algunas funciones de filtrado.
- El botón flotante requiere el permiso especial “Mostrar sobre otras apps”.
- Al activarlo, se bloquea el tráfico de Internet del dispositivo, no solo el navegador. Algunas funciones locales o del sistema pueden verse afectadas.
- Para recuperar Internet, desactiva el bloqueo desde la app o desde el botón flotante. Si Android detiene la app inesperadamente, abre la app y vuelve a comprobar el estado.
- El bloqueo no garantiza un corte absolutamente instantáneo: Android aplica los cambios de red y las apps pueden conservar datos ya descargados o conexiones en caché.

## Generar APK con GitHub

1. Crea un repositorio nuevo en GitHub.
2. Descomprime este ZIP y sube **el contenido** del proyecto a la raíz del repositorio.
3. Abre la pestaña **Actions**.
4. Ejecuta **Build Android APK** (o espera a que termine el flujo al hacer push).
5. Abre la ejecución completada y descarga el artefacto `CART3R-VPN-Blocker-debug`.
6. Dentro encontrarás `app-debug.apk`.

Para instalar el APK de depuración, Android puede pedir autorización para instalar apps desde esa fuente. Para publicar en Play Store necesitarás una versión release firmada y revisar las políticas de VPN/foreground services.

## Requisitos de compilación

- JDK 17
- Android SDK 35
- Gradle 8.9 o compatible con Android Gradle Plugin 8.7.3

## Estructura

```text
.
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/carter/vpnblocker/
│           ├── MainActivity.kt
│           ├── BlockVpnService.kt
│           └── FloatingBubbleService.kt
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```
