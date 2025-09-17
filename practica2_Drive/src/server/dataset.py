import json
import random
import unidecode
from pathlib import Path

# === CONFIGURACIÓN ===
NUM_EJEMPLOS_GPT2 = 1_000_000
NUM_EJEMPLOS_BERT = 1_000_000

# === CARGAR DATOS ===
with open("/mnt/c/Files/chatbot/metadatos/dataset/plantas_info.json", "r", encoding="utf-8") as f:
    plantas_info = json.load(f)

with open("/mnt/c/Files/chatbot/metadatos/dataset/plantas_normalizadas.json", "r", encoding="utf-8") as f:
    normalizadas = json.load(f)
    plantas_normalizadas = normalizadas["plantas"]

# === SINÓNIMOS ===
sinonimos = {}
for planta_id, datos in plantas_normalizadas.items():
    nombre = datos["nombre_cientifico"]
    for s in datos.get("sinonimos_normalizados", []):
        sinonimos[s.lower()] = nombre

def normalizar(texto):
    return unidecode.unidecode(texto.lower())

# === INTENCIONES BERT ===
intenciones_bert = {
    "diagnostico_problema": [
        "mi planta tiene hojas amarillas",
        "se están cayendo las hojas de mi bugambilia",
        "mi cactus tiene manchas negras"
    ],
    "cuidados_generales": [
        "como cuido una trinitaria",
        "que cuidados necesita una planta con flores",
        "que temperatura necesita la santa rita"
    ],
    "recomendacion_planta": [
        "recomiendame una planta facil de cuidar",
        "quiero una planta que resista el sol",
        "cual es buena para decorar interiores"
    ],
    "identificacion_planta": [
        "como se ve la bugambilia",
        "como saber si tengo una trinitaria",
        "como identifico una santa rita"
    ],
    "calendario_cuidados": [
        "que hago con mi planta en invierno",
        "cuando se debe podar la bugambilia",
        "que cuidados necesita en verano"
    ]
}

# === PREGUNTAS-RESPUESTAS GPT-2 ===
frases_generativas = []
for planta in plantas_info:
    nombre = planta["planta"]
    if "cuidados" in planta:
        for tema, explicacion in planta["cuidados"].items():
            pregunta = f"¿Qué cuidados de {tema} necesita la {nombre}?"
            frases_generativas.append((pregunta, explicacion))
    if "recomendaciones" in planta:
        for recomendacion in planta["recomendaciones"]:
            pregunta = f"Dame un consejo para cuidar la {nombre}"
            frases_generativas.append((pregunta, recomendacion))

def expandir_variantes(texto):
    variantes = [texto]
    variantes.append(normalizar(texto))
    palabras = texto.split()
    for i, palabra in enumerate(palabras):
        if palabra.lower() in sinonimos:
            nueva = palabras.copy()
            nueva[i] = sinonimos[palabra.lower()]
            variantes.append(" ".join(nueva))
    return list(set(variantes))

# === GENERAR BERT ===
bert_data = []
for label, ejemplos in intenciones_bert.items():
    for _ in range(NUM_EJEMPLOS_BERT // len(intenciones_bert)):
        base = random.choice(ejemplos)
        variante = random.choice(expandir_variantes(base))
        bert_data.append({
            "text": variante,
            "labels": {
                "intent": label
            }
        })

# === GENERAR GPT-2 ===
gpt2_data = []
for _ in range(NUM_EJEMPLOS_GPT2):
    base = random.choice(frases_generativas)
    variantes = expandir_variantes(base[0])
    gpt2_data.append({
        "input_text": random.choice(variantes),
        "target_text": base[1]
    })

# === GUARDAR ===
Path("bert_training_data_robusto.json").write_text(json.dumps(bert_data, indent=2, ensure_ascii=False), encoding="utf-8")
Path("gpt2_training_data_robusto.json").write_text(json.dumps(gpt2_data, indent=2, ensure_ascii=False), encoding="utf-8")

print("✅ Archivos creados: bert_training_data_robusto.json y gpt2_training_data_robusto.json")
