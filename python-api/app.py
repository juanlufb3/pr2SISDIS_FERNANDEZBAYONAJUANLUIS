"""
API Flask - Práctica 2 Sistemas Distribuidos
Excepciones implementadas:
  1. Archivos  → FileNotFoundError, PermissionError, OSError
  2. Base de datos PostgreSQL → psycopg2.OperationalError, DatabaseError
  3. API externa (PokeAPI) → HTTPError, Timeout, ConnectionError
"""

import os
import requests
import psycopg2
from flask import Flask, jsonify

app = Flask(__name__)

import logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)

# Contador en memoria — se resetea al reiniciar Flask
stats = {
    "file_ok": 0, "file_error": 0,
    "db_ok": 0,   "db_error": 0,
    "pokemon_ok": 0, "pokemon_error": 0, "pokemon_timeout": 0
}

# URL de conexión a PostgreSQL — viene de la variable de entorno de Docker
DB_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://sisdis:sisdis123@postgres:5432/sisdis_db"
)

# ─────────────────────────────────────────────────────────────────────────────
# HEALTH CHECK — para verificar que Flask está vivo
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/health")
def health():
    return jsonify({
        "status": "ok",
        "servicio": "Flask API - SisDis Práctica 2"
    })


# ─────────────────────────────────────────────────────────────────────────────
# 1. EXCEPCIONES DE ARCHIVOS
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/api/file-ok")
def file_ok():
    stats["file_ok"] += 1
        logger.info("Invocado /api/file-ok")
    """Lee un archivo que SÍ existe. Lo crea si no estaba."""
    ruta = "/tmp/sisdis_data.txt"
    if not os.path.exists(ruta):
        with open(ruta, "w") as f:
            f.write("Archivo de ejemplo - Sistemas Distribuidos Práctica 2.\n"
                    "Generado automáticamente por la API Flask.")
    try:
        with open(ruta, "r") as f:
            contenido = f.read()
        return jsonify({
            "resultado": contenido,
            "tipo": "lectura_archivo_ok",
            "ruta": ruta
        })
    except Exception as e:
        return jsonify({
            "error": "Error inesperado leyendo archivo",
            "detalle": str(e)
        }), 500


@app.route("/api/file-error")
def file_error():
    stats["file_error"] += 1
        logger.info("Invocado /api/file-error — simulando FileNotFoundError")

    """Intenta leer un archivo que NO existe → FileNotFoundError."""
    ruta = "/tmp/archivo_inexistente_sisdis_99999.txt"
    try:
        with open(ruta, "r") as f:
            contenido = f.read()
        return jsonify({"resultado": contenido})

    except FileNotFoundError as e:
        return jsonify({
            "error": "Archivo no encontrado",
            "tipo": "FileNotFoundError",
            "detalle": f"No existe el archivo: {ruta}. Causa original: {str(e)}"
        }), 404

    except PermissionError as e:
        return jsonify({
            "error": "Sin permisos para leer el archivo",
            "tipo": "PermissionError",
            "detalle": str(e)
        }), 403

    except OSError as e:
        return jsonify({
            "error": "Error de sistema de archivos",
            "tipo": "OSError",
            "detalle": str(e)
        }), 500


# ─────────────────────────────────────────────────────────────────────────────
# 2. EXCEPCIONES DE BASE DE DATOS
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/api/db-ok")
def db_ok():
    stats["db_ok"] += 1
        logger.info("Invocado /api/db-ok")

    """Consulta la versión de PostgreSQL — conexión real y correcta."""
    try:
        conn = psycopg2.connect(DB_URL)
        cur  = conn.cursor()
        cur.execute("SELECT version();")
        version = cur.fetchone()[0]
        cur.close()
        conn.close()
        return jsonify({
            "resultado": f"Conexión correcta a PostgreSQL: {version}",
            "tipo": "db_ok"
        })

    except psycopg2.OperationalError as e:
        return jsonify({
            "error": "Error de conexión a la base de datos",
            "tipo": "psycopg2.OperationalError",
            "detalle": str(e)
        }), 500


@app.route("/api/db-error")
def db_error():
    stats["db_error"] += 1
        logger.info("Invocado /api/db-error — simulando OperationalError")

    """Usa credenciales incorrectas → OperationalError."""
    url_mala = "postgresql://usuario_falso:password_wrong@localhost:9999/bd_inexistente"
    try:
        conn = psycopg2.connect(url_mala, connect_timeout=3)
        cur  = conn.cursor()
        cur.execute("SELECT 1;")
        conn.close()
        return jsonify({"resultado": "No debería llegar aquí"})

    except psycopg2.OperationalError as e:
        return jsonify({
            "error": "Error de conexión a la BD (credenciales o host inválidos)",
            "tipo": "psycopg2.OperationalError",
            "detalle": str(e)
        }), 500

    except psycopg2.DatabaseError as e:
        return jsonify({
            "error": "Error genérico de base de datos",
            "tipo": "psycopg2.DatabaseError",
            "detalle": str(e)
        }), 500


# ─────────────────────────────────────────────────────────────────────────────
# 3. EXCEPCIONES DE API EXTERNA — PokeAPI
# ─────────────────────────────────────────────────────────────────────────────

POKEAPI_BASE = "https://pokeapi.co/api/v2/pokemon"


@app.route("/api/pokemon/<nombre>")
def pokemon_ok(nombre):
    stats["pokemon_ok"] += 1
        logger.info("Invocado /api/pokemon/%s", nombre)
    """Consulta un Pokémon real a la PokeAPI externa."""
    try:
        r = requests.get(f"{POKEAPI_BASE}/{nombre.lower()}", timeout=8)
        r.raise_for_status()   # lanza HTTPError si status >= 400
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
        return jsonify({
            "error": f"Pokémon '{nombre}' no encontrado en la PokeAPI",
            "tipo":  "requests.HTTPError",
            "detalle": str(e)
        }), codigo

    except requests.exceptions.Timeout:
        return jsonify({
            "error": "Timeout: la PokeAPI tardó demasiado en responder",
            "tipo":  "requests.Timeout",
            "detalle": "El tiempo de espera (8s) se agotó."
        }), 504

    except requests.exceptions.ConnectionError as e:
        return jsonify({
            "error": "Sin conexión con la PokeAPI",
            "tipo":  "requests.ConnectionError",
            "detalle": str(e)
        }), 502


@app.route("/api/pokemon-error")
def pokemon_error():
    stats["pokemon_error"] += 1
        logger.info("Invocado /api/pokemon-error — simulando HTTPError 404")

    """Busca un Pokémon que NO existe → HTTPError 404 de API externa."""
    nombre_falso = "pokemonquenuncaexistira99999"
    try:
        r = requests.get(f"{POKEAPI_BASE}/{nombre_falso}", timeout=8)
        r.raise_for_status()
        return jsonify(r.json())

    except requests.exceptions.HTTPError as e:
        return jsonify({
            "error": f"Pokémon '{nombre_falso}' no existe en la PokeAPI",
            "tipo":  "requests.HTTPError",
            "detalle": str(e)
        }), 404


@app.route("/api/pokemon-timeout")
def pokemon_timeout():
    stats["pokemon_timeout"] += 1
        logger.info("Invocado /api/pokemon-timeout — simulando Timeout")

    """Simula un timeout llamando a una URL que tarda mucho en responder."""
    try:
        # httpbin.org/delay/30 tarda 30 segundos → timeout de 2s lo corta
        r = requests.get("https://httpbin.org/delay/30", timeout=2)
        r.raise_for_status()
        return jsonify({"resultado": "ok"})

    except requests.exceptions.Timeout:
        return jsonify({
            "error": "Timeout al llamar al servicio externo",
            "tipo":  "requests.Timeout",
            "detalle": "El servicio externo no respondió en el tiempo límite (2s)."
        }), 504

    except requests.exceptions.ConnectionError as e:
        return jsonify({
            "error": "Error de conexión al simular timeout",
            "tipo":  "requests.ConnectionError",
            "detalle": str(e)
        }), 502


# ─────────────────────────────────────────────────────────────────────────────
# ARRANQUE
# ─────────────────────────────────────────────────────────────────────────────
@app.route("/api/stats")
def get_stats():
    """Devuelve cuántas veces se ha llamado a cada endpoint en esta sesión."""
    total   = sum(stats.values())
    errores = stats["file_error"] + stats["db_error"] + \
              stats["pokemon_error"] + stats["pokemon_timeout"]
    return jsonify({
        "total_llamadas": total,
        "total_errores":  errores,
        "total_exitos":   total - errores,
        "detalle": stats
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)