import requests
import random
from faker import Faker

fake = Faker()
BASE_URL = "http://localhost:8080/api"

TECH_PRODUCTS = [
    "Laptop", "Smartphone","Gaming" ,"Monitor ", "Teclado Mecánico", "Mouse Gamer",
    "Tablet", "Auriculares Bluetooth", "Smartwatch", "Cámara DSLR",
    "Tarjeta Gráfica", "Memoria RAM", "Disco SSD", "Router WiFi 6",
    "Impresora Láser", "Microscopio Digital"
]

def obtener_items(data):
    return data.get('content', data) if isinstance(data, dict) else data

def asegurar_usuarios_minimos(minimo=5):
    res = requests.get(f"{BASE_URL}/users")
    usuarios = obtener_items(res.json()) if res.status_code == 200 else []

    while len(usuarios) < minimo:
        nuevo = {
            "name": fake.name(),
            "email": fake.unique.email(),
            "password": "password123"
        }
        requests.post(f"{BASE_URL}/users", json=nuevo)
        res = requests.get(f"{BASE_URL}/users")
        usuarios = obtener_items(res.json())

    print(f"Usuarios disponibles: {len(usuarios)}")
    return [u['id'] for u in usuarios]

def asegurar_categorias():
    nombres = ["Electrónica", "Hogar", "Ropa", "Deportes", "Salud"]

    res = requests.get(f"{BASE_URL}/categories")
    existentes = obtener_items(res.json()) if res.status_code == 200 else []
    existentes_nombres = {c['name'] for c in existentes}

    for nombre in nombres:
        if nombre not in existentes_nombres:
            requests.post(f"{BASE_URL}/categories", json={
                "name": nombre,
                "description": "Carga masiva"
            })

    res = requests.get(f"{BASE_URL}/categories")
    categorias = obtener_items(res.json())

    print(f"Categorías disponibles: {len(categorias)}")
    return [c['id'] for c in categorias]

def run_seeder():
    print("INICIANDO CARGA TOTAL (1000 PRODUCTOS REALES)")

    # 1. Asegurar mínimo 5 usuarios
    user_ids = asegurar_usuarios_minimos(5)

    # 2. Asegurar categorías
    category_ids = asegurar_categorias()

    if len(category_ids) < 2:
        print("Se requieren al menos 2 categorías.")
        return

    # 3. Crear 1000 productos (cada uno con mínimo 2 categorías)
    print("Insertando 1000 productos...")
    success = 0
    intentos = 0

    while success < 1000 and intentos < 3000:
        intentos += 1

        product_base = random.choice(TECH_PRODUCTS)
        product_name = f"{product_base} {fake.word().capitalize()} {random.randint(100, 9999)}"
        product_name = product_name[:145]

        categorias_producto = random.sample(category_ids, k=random.randint(2, len(category_ids)))

        p_data = {
            "name": product_name,
            "price": round(random.uniform(10.0, 5000.0), 2),
            "description": fake.sentence(nb_words=15),
            "userId": random.choice(user_ids),
            "categoryIds": categorias_producto
        }

        res_p = requests.post(f"{BASE_URL}/products", json=p_data)

        if res_p.status_code in [200, 201]:
            success += 1
            if success % 100 == 0:
                print(f"{success} productos creados")
        else:
            continue

    print(f"CARGA COMPLETADA: {success} productos insertados.")

if __name__ == "__main__":
    run_seeder()
