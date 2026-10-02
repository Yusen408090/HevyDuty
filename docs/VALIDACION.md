# Verificación de la entrega

Fecha: 2 de octubre de 2026.

## Comprobaciones ejecutadas

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- **APK debug compilado**: `app/build/outputs/apk/debug/app-debug.apk`.
- **21 pruebas automatizadas**: 12 de reglas de dominio, 8 de Room y 1 del flujo completo de interfaz.
- **Lint sin errores**. Quedan advertencias de versiones de dependencias y sugerencias de estilo; no se han ocultado mediante una baseline.
- **Esquema Room generado**: `app/schemas/com.example.myapplication.data.local.GymDatabase/1.json`.

Los informes reproducibles quedan en:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/lint-results-debug.html`

## Qué comprueban las pruebas

### Dominio · 12 pruebas

Volumen solo de series completadas; exclusión de sesiones en curso; orden cronológico; máximos por ejercicio; carga externa cero; sesiones sin series terminadas; rechazo de NaN/infinito/pesos negativos/reps inválidas; rutinas sin nombre, sin ejercicios, con duplicados o con cero series; búsqueda sin acentos; duración no negativa.

### Room · 8 pruebas con Robolectric, API 28

Siembra idempotente; conservación del historial al editar y eliminar una plantilla; dos inicios simultáneos producen una sesión; rechazo de finalización sin series completas; rollback ante una edición inválida; sesiones finalizadas inmutables; recuperación desde un archivo de base de datos después de cerrarlo y abrirlo; borrado en cascada de ejercicios y series de una sesión.

### Interfaz · 1 prueba con Compose y Robolectric, API 35

El test abre la actividad real y recorre:

1. Inicio y catálogo.
2. Búsqueda de `triceps` y detalle de Extensión de tríceps.
3. Creación de una rutina con Press de banca.
4. Inicio de sesión y registro de 60 kg × 10 repeticiones.
5. Finalización y comprobación de 600 kg de volumen.
6. Apertura del historial y comprobación de la serie guardada.

Se generan capturas reales del render de Compose en `app/build/screenshots`. Las copias de esta entrega están en `docs/screenshots`.

La prueba de interfaz usa un entorno Android simulado en la JVM. No sustituye la comprobación de hardware.

## Pendiente de verificar en dispositivo

No había un dispositivo ADB ni un AVD configurado durante esta entrega. Cámara, permisos del sistema y ubicación están implementados, pero sus resultados de hardware no se presentan como verificados.

| Prueba manual | Resultado esperado |
| --- | --- |
| CAMERA concedida | Vista previa, cambio de cámara y captura visible en Perfil |
| CAMERA denegada | La app sigue utilizable; explica el permiso y mantiene Galería disponible |
| Denegación permanente | Opción para abrir los ajustes de la aplicación |
| Seleccionar y cancelar Photo Picker | Importación privada de la foto seleccionada; cancelar no crea registros |
| Foto girada / imagen grande | Orientación corregida y copia de tamaño acotado |
| Borrar foto | Confirmación, eliminación de archivo privado y entrada de Room |
| GPS preciso y aproximado | Guarda las coordenadas disponibles sin exigir precisión |
| GPS apagado o sin señal | Timeout/error visible, sin guardar coordenadas ficticias |
| Dispositivo sin Play Services | Error visible al intentar obtener ubicación; resto de módulos disponible |
| Rotar en mitad de una sesión | Series persistidas y estado de pantalla recuperable |
| Cerrar/abrir tras guardar una serie | Inicio ofrece continuar la sesión con sus valores |
| Fuente grande y pantalla pequeña | Scroll accesible en editor, tracking, perfil y diálogos |

## Límites funcionales explícitos

Esta versión cubre los módulos locales solicitados. No incluye backend, cuentas, feed, amistades, sincronización, mapas ni grabación continua de recorridos. El temporizador funciona dentro de la app y no emite una alarma de fondo. Las fotografías son de progreso; la tarjeta del perfil muestra la más reciente como imagen, sin un selector independiente de avatar.
