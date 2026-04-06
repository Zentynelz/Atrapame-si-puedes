import os
import json
import subprocess
import urllib.request
from datetime import datetime

# Rutas relativas
GOOGLE_SERVICES_PATH = os.path.join("app", "google-services.json")
BASE_FOLDER = "resultados_organizados"

def parse_firestore_value(val):
    if "stringValue" in val:
        return val["stringValue"]
    if "integerValue" in val:
        return int(val["integerValue"])
    if "doubleValue" in val:
        return float(val["doubleValue"])
    if "booleanValue" in val:
        return val["booleanValue"]
    if "arrayValue" in val:
        array_vals = val["arrayValue"].get("values", [])
        return [parse_firestore_value(v) for v in array_vals]
    if "mapValue" in val:
        fields = val["mapValue"].get("fields", {})
        return {k: parse_firestore_value(v) for k, v in fields.items()}
    if "nullValue" in val:
        return None
    return str(val) # Fallback

def get_project_id():
    if not os.path.exists(GOOGLE_SERVICES_PATH):
        print(f"❌ Error: No se encontró {GOOGLE_SERVICES_PATH}")
        return None
    with open(GOOGLE_SERVICES_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
        return data.get("project_info", {}).get("project_id")

def organizar_datos_resto():
    project_id = get_project_id()
    if not project_id:
        return

    print(f"Conectando a Firebase del proyecto: {project_id}...")
    url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/scores?pageSize=300"
    
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req) as response:
            raw_data = json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"Error conectando a Firebase API: {e}")
        print("Asegúrate de tener conexión a Internet y que la base de datos esté en Modo de Prueba (pública).")
        return

    documents = raw_data.get("documents", [])
    if not documents:
        print("⚠️ No se encontraron scores en la base de datos de Firebase.")
        return

    if not os.path.exists(BASE_FOLDER):
        os.makedirs(BASE_FOLDER)
        
    count = 0
    for doc in documents:
        # Extraer ID del documento de la ruta /projects/../documents/scores/ID
        doc_id = doc.get("name", "").split("/")[-1]
        fields = doc.get("fields", {})
        
        # Convertir formato Firestore REST a diccionario normal de Python
        data = {k: parse_firestore_value(v) for k, v in fields.items()}
        
        # Ignorar si no tiene timeline
        if 'emotionTimeline' not in data or not data['emotionTimeline']:
            continue
            
        diff = data.get('difficulty', 'UNKNOWN')
        device_id = data.get('deviceId', 'UNKNOWN_DEVICE')
        player_name = data.get('playerName', 'Anonimo')
        timestamp_ms = data.get('timestamp', 0)
        
        # Estructura: resultados_organizados / Dificultad / Celular_ID
        diff_folder = os.path.join(BASE_FOLDER, str(diff))
        device_folder = os.path.join(diff_folder, f"Celular_{device_id}")
        
        if not os.path.exists(device_folder):
            os.makedirs(device_folder)
            
        try:
            date_str = datetime.fromtimestamp(timestamp_ms / 1000.0).strftime('%Y-%m-%d_%H-%M-%S')
        except:
            date_str = str(timestamp_ms)
            
        file_base_name = f"{player_name}_{date_str}_{doc_id}"
        json_path = os.path.join(device_folder, f"{file_base_name}.json")
        img_path = os.path.join(device_folder, f"{file_base_name}_grafica.png")
        
        # Guardar JSON limpio
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
            
        print(f"Descargado: {json_path}")
        
        # Graficar automáticamente
        try:
            subprocess.run(["python", "generar_grafica_estres.py", "-i", json_path, "-o", img_path], check=True)
            count += 1
        except Exception as e:
            print(f"No se pudo generar grafica para {json_path}")

    print("\n" + "="*50)
    print(f"EXITO! Se descargaron y graficaron {count} partidas.")
    print(f"Revisa la carpeta '{os.path.abspath(BASE_FOLDER)}'")
    print("="*50)

if __name__ == "__main__":
    organizar_datos_resto()
