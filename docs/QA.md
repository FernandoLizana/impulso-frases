# QA

Fecha: 29 de septiembre de 2026.

El catálogo que se recorrió en esas capturas era el provisional. Después se reemplazó por las 68 frases distintas de la hoja Frases, con el autor de la planilla, y el icono pasó a ser el logo propio de Impulso.

Dispositivo: emulador `Medium_Phone`, imagen `google_apis` x86_64, API 36 (`sdk_gphone64_x86_64`). El informe de instrumentación lo nombra `Medium_Phone(AVD) - 16`.

APK: `dist/impulso-debug.apk` (depuración, sin firma de publicación). Instalación comprobada con `adb install -r`.

## Compilación y pruebas automáticas

| Comprobación | Resultado |
| --- | --- |
| `:app:assembleDebug` | Correcta |
| `:app:lintDebug` | Correcta, 0 errores. Quedan 54 advertencias (no bloquean). |
| Pruebas de unidad | 31 correctas, 0 fallos: selector 11, horarios 12, respaldo y migración 7, catálogo 1 |
| Entrega programada | `ReminderDeliveryTest` correcta en 50 s. Apareció la notificación de recordatorio (id 5101) |

La prueba de entrega concedió antes `POST_NOTIFICATIONS` y `SCHEDULE_EXACT_ALARM` (`appops allow`), apagó el horario silencioso y pidió alarma exacta. Programó una hora propia a unos 75 segundos y esperó la notificación real de `AlarmManager`. El botón de aviso de prueba de la pantalla no se usó como prueba de una entrega futura.

Eso no mide la ventana aproximada de 10 minutos, que es el modo por defecto cuando la persona no pide exactitud o el sistema no la permite.

## Recorrido en el emulador

Con la app recién instalada: Continuar en la bienvenida, nombre Ana, objetivo Calma, tono Cercano y amable, Empezar.

En Hoy, con fecha local del 29 de septiembre por la tarde:

- El saludo fue «Buenas tardes, Ana».
- «Ver otra frase» mostró otra tarjeta y dejó la reflexión y la acción en la frase del día.
- «Lo hice» y después «Deshacer» dejaron el día en «Sin registrar».
- Los siete días se listan como hecha, omitida o sin registrar. No hay racha.

Explorar, Recordatorios y Ajustes se abrieron en tema claro y en tema oscuro. En Recordatorios el interruptor seguía apagado, con el texto de que Android puede retrasar el aviso y el silencio de 22:00 a 08:00. En Ajustes se veían Ana, Calma y el tono cercano.

Con la fuente al 200 % la pantalla Hoy se desplaza: la frase, «Lo hice», «Hoy lo dejo pasar» y los siete días quedan alcanzables, y la barra inferior no los tapa. La etiqueta «Recordatorios» se corta en la barra («Recordatori…») con cinco destinos.

Capturas en `docs/capturas/`: `bienvenida-logo.png`, `explorar-claro.png`, `recordatorios-claro.png`, `hoy-oscuro.png`, `explorar-oscuro.png`, `recordatorios-oscuro.png`, `ajustes-oscuro.png`, `hoy-fuente-200.png`, `hoy-fuente-200-scroll.png`.

Las capturas en claro de Hoy y de Ajustes se quitaron del repositorio: mostraban el nombre escrito en el perfil de un teléfono, y ese dato no forma parte del proyecto. El nombre «Ana» de este informe es el que se escribió durante la prueba.

## Qué no quedó demostrado en el dispositivo

- Pulsar el corazón, crear una frase propia, compartir el PNG y oír la lectura en voz.
- Importar y exportar el JSON desde la pantalla (la fusión sí está cubierta por pruebas de unidad).
- Una entrega solo aproximada, sin permiso de alarma exacta.
- Un aviso que caiga dentro del horario silencioso o con más de 60 minutos de retraso (cubierto en las pruebas de unidad del cálculo, no con una alarma real).
- TalkBack y la escala de animación en cero. El código no avanza solo la bienvenida en esos casos; no se recorrió con los servicios activos.

Después de `Force Stop`, Android cancela las alarmas hasta que la app vuelve a abrirse o recibe arranque, cambio de hora o zona, o una actualización. La prueba de entrega no cubre ese corte.

La notificación de prueba de la pantalla solo comprueba el canal y el permiso. No demuestra el siguiente aviso programado.
