import os
import requests
import psycopg2
from flask import Flask, jsonify
# os: para leer variables de entorno y trabajar con archivos
# requests: para llamar a la PokeAPI externa
# psycopg2: driver para conectarse a PostgreSQL
# jsonify: convierte diccionarios Python a JSON para la respuesta HTTP

app = Flask(__name__)
# crea la aplicacion Flask

import logging
logging.basicConfig(level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)
# configura el sistema de logs, igual que el Logger de Spring Boot

DB_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://sisdis:sisdis123@postgres:5432/sisdis_db"
)
# os.getenv lee la variable de entorno DATABASE_URL que inyecta Docker
# si no existe usa el valor por defecto (el segundo parametro)
# "postgres" es el nombre del contenedor Docker, no localhost

stats = {
    "file_ok": 0, "file_error": 0,
    "db_ok": 0,   "db_error": 0,
    "pokemon_ok": 0, "pokemon_error": 0, "pokemon_timeout": 0
}
# diccionario en memoria que cuenta llamadas por endpoint
# se resetea al reiniciar el contenedor Flask

# ── HEALTH CHECK ──────────────────────────────────────────────────────────

@app.route("/health")
def health():
    return jsonify({"status": "ok", "servicio": "Flask API - SisDis Practica 2"})
    # endpoint para verificar que Flask esta vivo
    # lo primero que compruebas tras hacer docker compose up

# ── EXCEPCIONES DE ARCHIVOS ───────────────────────────────────────────────

@app.route("/api/file-ok")
def file_ok():
    stats["file_ok"] += 1
    logger.info("Invocado /api/file-ok")
    ruta = "/tmp/sisdis_data.txt"
    if not os.path.exists(ruta):
        with open(ruta, "w") as f:
            f.write("Archivo de ejemplo - Sistemas Distribuidos Practica 2.")
        # crea el archivo si no existe para que la demo funcione siempre
    try:
        with open(ruta, "r") as f:
            contenido = f.read()
        return jsonify({"resultado": contenido, "tipo": "lectura_archivo_ok"})
    except Exception as e:
        return jsonify({"error": "Error inesperado", "detalle": str(e)}), 500

@app.route("/api/file-error")
def file_error():
    stats["file_error"] += 1
    logger.info("Invocado /api/file-error — simulando FileNotFoundError")
    ruta = "/tmp/archivo_inexistente_sisdis_99999.txt"
    # este archivo nunca existe, es para forzar el error
    try:
        with open(ruta, "r") as f:
            contenido = f.read()
        return jsonify({"resultado": contenido})

    except FileNotFoundError as e:
        # el archivo no existe → 404
        return jsonify({
            "error": "Archivo no encontrado",
            "tipo": "FileNotFoundError",
            "detalle": f"No existe: {ruta}. Causa: {str(e)}"
        }), 404

    except PermissionError as e:
        # sin permisos de lectura → 403
        return jsonify({
            "error": "Sin permisos para leer el archivo",
            "tipo": "PermissionError",
            "detalle": str(e)
        }), 403

    except OSError as e:
        # error generico de sistema de archivos → 500
        return jsonify({
            "error": "Error de sistema de archivos",
            "tipo": "OSError",
            "detalle": str(e)
        }), 500

# ── EXCEPCIONES DE BASE DE DATOS ──────────────────────────────────────────

@app.route("/api/db-ok")
def db_ok():
    stats["db_ok"] += 1
    logger.info("Invocado /api/db-ok")
    try:
        conn = psycopg2.connect(DB_URL)
        # abre conexion real a PostgreSQL en Docker
        cur = conn.cursor()
        cur.execute("SELECT version();")
        # consulta la version de PostgreSQL
        version = cur.fetchone()[0]
        cur.close()
        conn.close()
        return jsonify({"resultado": f"Conexion correcta: {version}", "tipo": "db_ok"})
    except psycopg2.OperationalError as e:
        return jsonify({"error": "Error de conexion", "tipo": "OperationalError",
                        "detalle": str(e)}), 500

@app.route("/api/db-error")
def db_error():
    stats["db_error"] += 1
    logger.info("Invocado /api/db-error — simulando OperationalError")
    url_mala = "postgresql://usuario_falso:password_wrong@localhost:9999/bd_inexistente"
    # credenciales y puerto incorrectos a posta para forzar el error
    try:
        conn = psycopg2.connect(url_mala, connect_timeout=3)
        # connect_timeout=3 para que falle en 3 segundos y no se quede colgado
        cur = conn.cursor()
        cur.execute("SELECT 1;")
        conn.close()
        return jsonify({"resultado": "No deberia llegar aqui"})

    except psycopg2.OperationalError as e:
        # fallo de conexion: host malo, credenciales invalidas, puerto cerrado
        return jsonify({
            "error": "Error de conexion a la BD",
            "tipo": "psycopg2.OperationalError",
            "detalle": str(e)
        }), 500

    except psycopg2.DatabaseError as e:
        # error generico de BD
        return jsonify({
            "error": "Error generico de base de datos",
            "tipo": "psycopg2.DatabaseError",
            "detalle": str(e)
        }), 500

# ── EXCEPCIONES DE API EXTERNA (POKEAPI) ──────────────────────────────────

POKEAPI_BASE = "https://pokeapi.co/api/v2/pokemon"

@app.route("/api/pokemon/<nombre>")
def pokemon_ok(nombre):
    # <nombre> es un parametro dinamico de la URL: /api/pokemon/pikachu
    stats["pokemon_ok"] += 1
    logger.info("Invocado /api/pokemon/%s", nombre)
    try:
        r = requests.get(f"{POKEAPI_BASE}/{nombre.lower()}", timeout=8)
        r.raise_for_status()
        # raise_for_status() lanza HTTPError si el codigo es 4xx o 5xx
        data = r.json()
        return jsonify({
            "nombre": data["name"],
            "id":     data["id"],
            "peso":   data["weight"],
            "altura": data["height"],
            "tipo":   "pokemon_ok"
        })
    except requests.exceptions.HTTPError as e:
        codigo = e.response.status_code if e.response is not None else 404
        return jsonify({"error": f"Pokemon '{nombre}' no encontrado",
                        "tipo": "requests.HTTPError", "detalle": str(e)}), codigo
    except requests.exceptions.Timeout:
        return jsonify({"error": "Timeout llamando a PokeAPI",
                        "tipo": "requests.Timeout",
                        "detalle": "Tiempo de espera (8s) agotado."}), 504
    except requests.exceptions.ConnectionError as e:
        return jsonify({"error": "Sin conexion con PokeAPI",
                        "tipo": "requests.ConnectionError", "detalle": str(e)}), 502

@app.route("/api/pokemon-error")
def pokemon_error():
    stats["pokemon_error"] += 1
    logger.info("Invocado /api/pokemon-error — simulando HTTPError 404")
    nombre_falso = "pokemonquenuncaexistira99999"
    try:
        r = requests.get(f"{POKEAPI_BASE}/{nombre_falso}", timeout=8)
        r.raise_for_status()
        return jsonify(r.json())
    except requests.exceptions.HTTPError as e:
        return jsonify({
            "error": f"Pokemon '{nombre_falso}' no existe en PokeAPI",
            "tipo":  "requests.HTTPError",
            "detalle": str(e)
        }), 404

@app.route("/api/pokemon-timeout")
def pokemon_timeout():
    stats["pokemon_timeout"] += 1
    logger.info("Invocado /api/pokemon-timeout — simulando Timeout")
    try:
        r = requests.get("https://httpbin.org/delay/30", timeout=2)
        # httpbin.org/delay/30 tarda 30 segundos en responder
        # con timeout=2 forzamos que salte Timeout antes
        r.raise_for_status()
        return jsonify({"resultado": "ok"})
    except requests.exceptions.Timeout:
        return jsonify({
            "error": "Timeout al llamar al servicio externo",
            "tipo":  "requests.Timeout",
            "detalle": "El servicio no respondio en 2 segundos."
        }), 504
    except requests.exceptions.ConnectionError as e:
        return jsonify({"error": "Error de conexion",
                        "tipo": "requests.ConnectionError", "detalle": str(e)}), 502

# ── ESTADISTICAS ──────────────────────────────────────────────────────────

@app.route("/api/stats")
def get_stats():
    total   = sum(stats.values())
    errores = stats["file_error"] + stats["db_error"] + \
              stats["pokemon_error"] + stats["pokemon_timeout"]
    return jsonify({
        "total_llamadas": total,
        "total_errores":  errores,
        "total_exitos":   total - errores,
        "detalle": stats
        # devuelve cuantas veces se ha llamado a cada endpoint en esta sesion
    })

# ── ARRANQUE ──────────────────────────────────────────────────────────────

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
    # host="0.0.0.0" necesario en Docker para aceptar conexiones externas
    # si fuera "localhost" solo aceptaria conexiones desde dentro del contenedor