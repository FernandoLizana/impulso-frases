# Migración

## Punto de partida

El repositorio no contenía una app ni datos previos. No hay un `applicationId` anterior que conservar. El identificador provisional es `com.example.impulso`. Cambiarlo más adelante crea una app distinta para Android: la instalación nueva no hereda el almacenamiento privado de la anterior. Hay que documentar el identificador definitivo antes de publicarlo y no volver a cambiarlo sin un plan de copia.

Las dos preguntas del cuestionario son las de este proyecto, porque no había preguntas originales en el repo:

1. Objetivo: autoestima, calma, constancia, enfoque, afrontar cambios, gratitud.
2. Tono: cercano, directo, espiritual.

El nombre es opcional. Los recordatorios no forman parte del cuestionario.

## Preferencias

`PreferenceMigration` solo corre si todavía no existen las claves del esquema nuevo (cuestionario completado, nombre o paso). Si el esquema nuevo ya está, no se reescribe.

Claves heredadas que sí se leen, si aparecen:

| Dato | Claves antiguas | Valor nuevo |
| --- | --- | --- |
| Nombre | `nombre`, `name`, `user_name` | nombre, como máximo 40 caracteres |
| Objetivo | `pregunta1`, `objetivo`, `goal` | id de objetivo, o vacío si no se reconoce |
| Tono | `pregunta2`, `estilo`, `tone` | `cercano`, `directo` o `espiritual` |

Completar esa migración marca el cuestionario como hecho. Añadir campos nuevos no borra las respuestas ya guardadas.

Archivo de preferencias de la app: `impulso_prefs`. El archivo de preferencias por defecto del paquete solo se consulta para las claves heredadas.

## Base de datos

Room versión 1, archivo `impulso.db`, `exportSchema` desactivado mientras no haya una migración real. No hay `fallbackToDestructiveMigration`: un cambio de esquema futuro tiene que traer una migración explícita.

El catálogo vive en `assets/catalog.json` (versión de catálogo 1), no en Room. Sale de la hoja Frases del Excel: texto en español y autor. Las filas con el mismo texto se guardan una sola vez. Actualizar frases no borra favoritos, frases propias ni el historial. Si la frase del día ya no está en el catálogo, Hoy elige otra y deja la acción de ese día sin registrar.

La frase del día y su acción (`hecha`, `omitida` o vacía) comparten la fila de `daily_state`. Una importación puede completar una acción sin borrar la frase ya elegida para esa fecha.

## Respaldo `impulso-backup` versión 1

Tope de lectura: 2 000 000 bytes. Un formato o una versión distintos no modifican nada.

El JSON puede incluir nombre, objetivo, tono, tema, salto de bienvenida, velocidad y voz de lectura, horario silencioso, días, inclusión de frases propias, preferencia de alarma exacta, favoritos, frases propias, acciones por fecha, recordatorios e historial (tope 200 al fusionar).

No incluye `remindersEnabled`. Importar no enciende los recordatorios ni programa alarmas por sí solo; después de fusionar, la app reconcilia el estado que ya tenía el interruptor.

Si la misma clave existe en el teléfono y en el archivo, gana el dato local. Volver a importar el mismo archivo no duplica favoritos, frases ni historial.

## Copia de Android y alarmas

`allowBackup` está activo e incluye `impulso_prefs.xml` e `impulso.db`. La copia de seguridad de Android puede sacar esos datos del teléfono. El texto de privacidad de Ajustes lo dice.

`Force Stop` cancela las alarmas hasta la próxima apertura, el próximo arranque, un cambio de zona u hora, o una actualización del paquete. La app no promete avisos después de forzar el cierre.

Los huecos de horario de verano se omiten. Si una hora local ocurre dos veces, se entrega una sola vez, con el desfase anterior. Una entrega con más de 60 minutos de retraso, o que cae en horario silencioso en el momento de mostrarse, no se avisa y no recupera avisos atrasados.

La transición de Chile no está escrita a mano: la prueba de unidad la descubre con `ZoneRules` de la zona `America/Santiago`.
