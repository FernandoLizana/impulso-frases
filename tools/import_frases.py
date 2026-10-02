"""Carga la hoja Frases del Excel y escribe app/src/main/assets/catalog.json.

Solo usa esa hoja. El texto en español y el autor salen de la planilla.
Las filas con el mismo texto se omiten y queda la primera.
Unknown se muestra como Autor desconocido.
"""

import json
import sys
from pathlib import Path

import openpyxl

OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "catalog.json"

# fila de Excel -> categoría, tono, etiqueta
META = {
    2: ("proposito", "espiritual", "asumir"),
    3: ("gratitud", "cercano", "felicidad"),
    4: ("proposito", "directo", "oficio"),
    5: ("cambios", "cercano", "reaccion"),
    6: ("cambios", "cercano", "tiempo"),
    7: ("proposito", "directo", "oficio"),
    8: ("creatividad", "cercano", "imaginacion"),
    9: ("confianza", "directo", "duda"),
    10: ("confianza", "cercano", "creer"),
    11: ("proposito", "directo", "logro"),
    12: ("creatividad", "cercano", "sueno"),
    13: ("enfoque", "directo", "aprendizaje"),
    14: ("descanso", "cercano", "pausa"),
    15: ("resiliencia", "directo", "continuar"),
    17: ("relaciones", "directo", "tiempo"),
    18: ("constancia", "directo", "dia"),
    19: ("constancia", "directo", "esfuerzo"),
    20: ("resiliencia", "directo", "adversidad"),
    21: ("constancia", "directo", "paso"),
    22: ("cambios", "cercano", "oportunidad"),
    23: ("constancia", "cercano", "camino"),
    24: ("autoestima", "cercano", "valor"),
    25: ("confianza", "directo", "animo"),
    26: ("proposito", "directo", "oficio"),
    27: ("enfoque", "directo", "solucion"),
    29: ("confianza", "cercano", "actitud"),
    30: ("resiliencia", "directo", "obstaculo"),
    32: ("resiliencia", "directo", "fracaso"),
    33: ("constancia", "cercano", "ritmo"),
    34: ("enfoque", "directo", "aprendizaje"),
    35: ("calma", "cercano", "presente"),
    36: ("confianza", "directo", "valor"),
    37: ("constancia", "directo", "paso"),
    38: ("enfoque", "directo", "inicio"),
    39: ("cambios", "cercano", "meta"),
    40: ("confianza", "directo", "limite"),
    41: ("gratitud", "cercano", "felicidad"),
    42: ("autoestima", "cercano", "valor"),
    46: ("proposito", "directo", "futuro"),
    47: ("resiliencia", "cercano", "levantarse"),
    48: ("confianza", "directo", "duda"),
    49: ("constancia", "cercano", "camino"),
    52: ("resiliencia", "cercano", "intento"),
    53: ("creatividad", "cercano", "sueno"),
    54: ("constancia", "directo", "repetir"),
    57: ("proposito", "cercano", "oficio"),
    58: ("cambios", "cercano", "tiempo"),
    60: ("constancia", "cercano", "dia"),
    61: ("creatividad", "espiritual", "imaginacion"),
    62: ("confianza", "espiritual", "fe"),
    63: ("creatividad", "espiritual", "imaginacion"),
    64: ("cambios", "espiritual", "mente"),
    65: ("calma", "espiritual", "interior"),
    66: ("confianza", "espiritual", "fe"),
    67: ("proposito", "espiritual", "asumir"),
    68: ("creatividad", "espiritual", "imaginacion"),
    69: ("autoestima", "espiritual", "realidad"),
    70: ("confianza", "espiritual", "deseo"),
    71: ("creatividad", "espiritual", "arte"),
    72: ("relaciones", "espiritual", "amor"),
    73: ("relaciones", "espiritual", "perdon"),
    74: ("proposito", "espiritual", "guion"),
    75: ("autoestima", "espiritual", "comparacion"),
    76: ("cambios", "espiritual", "conciencia"),
    77: ("creatividad", "espiritual", "mirada"),
    78: ("proposito", "espiritual", "exito"),
    79: ("autoestima", "espiritual", "belleza"),
    80: ("gratitud", "espiritual", "felicidad"),
}

PROMPTS = {
    "autoestima": (
        "¿Qué parte de ti no depende de la opinión de otros?",
        "Escribe una cualidad tuya que quieras tratar con más respeto.",
    ),
    "calma": (
        "¿Dónde puedes aflojar el ritmo sin abandonar lo importante?",
        "Haz una pausa de un minuto antes de la siguiente tarea.",
    ),
    "constancia": (
        "¿Qué paso pequeño cabe hoy, aunque sea corto?",
        "Haz hoy solo ese paso y márcalo cuando termines.",
    ),
    "enfoque": (
        "¿Qué merece tu atención en la próxima media hora?",
        "Elige una sola tarea y aparta lo que la interrumpe.",
    ),
    "cambios": (
        "¿Qué puedes aceptar de este momento sin pelearte con él?",
        "Anota un cambio pequeño que sí esté en tus manos.",
    ),
    "gratitud": (
        "¿Qué ya está presente y sueles pasar por alto?",
        "Nombra en voz baja una cosa concreta que te haga bien.",
    ),
    "resiliencia": (
        "¿Qué tropiezo puedes mirar sin convertirlo en una sentencia?",
        "Escribe el siguiente paso después de ese tropiezo.",
    ),
    "descanso": (
        "¿Qué señal de cansancio ya apareció hoy?",
        "Deja un hueco breve sin llenarlo con otra tarea.",
    ),
    "relaciones": (
        "¿A quién puedes tratar con más cuidado, incluyéndote?",
        "Envía o di una frase breve y honesta a esa persona.",
    ),
    "creatividad": (
        "¿Qué idea llevas rato sin darle un lugar?",
        "Anota esa idea en una línea, sin juzgarla.",
    ),
    "proposito": (
        "¿Qué parte de tu día se parece a lo que quieres sostener?",
        "Dedica diez minutos a eso, sin añadir otra meta.",
    ),
    "confianza": (
        "¿Qué duda te está frenando más que el hecho mismo?",
        "Da un paso visible y pequeño a pesar de esa duda.",
    ),
}


def clean_text(value):
    text = "" if value is None else str(value).strip()
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "\"'“”":
        text = text[1:-1].strip()
    return text


def author_name(value):
    name = "" if value is None else str(value).strip()
    if name.lower() == "unknown":
        return "Autor desconocido"
    return name


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Uso: python tools/import_frases.py ruta/al/libro.xlsx")
    source = Path(sys.argv[1])
    if not source.is_file():
        raise SystemExit(f"No se encontró el Excel: {source}")
    workbook = openpyxl.load_workbook(source, data_only=True)
    sheet = workbook["Frases"]
    seen = set()
    quotes = []
    skipped = []
    for row_number, row in enumerate(sheet.iter_rows(min_row=2, values_only=True), 2):
        text = clean_text(row[0])
        author = author_name(row[2] if len(row) > 2 else "")
        if not text:
            continue
        key = " ".join(text.lower().split())
        if key in seen:
            skipped.append(row_number)
            continue
        seen.add(key)
        if row_number not in META:
            raise SystemExit(f"Falta clasificación de la fila {row_number}")
        category, tone, tag = META[row_number]
        reflection, micro = PROMPTS[category]
        quotes.append(
            {
                "id": f"frase_{category}_{row_number:03d}",
                "text": text,
                "author": author,
                "authorship": "attributed",
                "sourceUrl": None,
                "category": category,
                "tone": tone,
                "tags": [tag],
                "reflection": reflection,
                "microAction": micro,
                "locale": "es",
                "catalogVersion": 1,
                "english": clean_text(row[1] if len(row) > 1 else ""),
            }
        )
    missing = sorted(set(META) - {int(item["id"].rsplit("_", 1)[1]) for item in quotes})
    if missing:
        raise SystemExit(f"Filas clasificadas que no entraron: {missing}")
    payload = {"catalogVersion": 1, "quotes": quotes}
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"quotes {len(quotes)} skipped_rows {skipped}")


if __name__ == "__main__":
    main()
