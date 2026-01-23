import requests
import random
from faker import Faker

fake = Faker()
BASE_URL = "http://localhost:8080/api"

TECH_PRODUCTS = [
    "Laptop", "Smartphone", "Monitor 4K", "Teclado Mecánico", "Mouse Gamer", 
    "Tablet", "Auriculares Bluetooth", "Smartwatch", "Cámara DSLR", 
    "Tarjeta Gráfica", "Memoria RAM", "Disco SSD", "Router WiFi 6", 
    "Impresora Láser", "Microscopio Digital", "Gaming"
]

def run_seeder():
    print("INICIANDO CARGA TOTAL (1000 PRODUCTOS REALES)")
    
    # 1. OBTENER O CREAR USUARIOS
    user_ids = []
    u_data = {
        "name": "User Test",
        "email": f"test{random.randint(1,9999)}@ups.edu.ec",
        "password": "password123"
    }
    requests.post(f"{BASE_URL}/users", json=u_data)
    
    res_users = requests.get(f"{BASE_URL}/users")
    if res_users.status_code == 200:
        users_data = res_users.json()
        items = users_data.get('content', users_data) if isinstance(users_data, dict) else users_data
        user_ids = [u['id'] for u in items]
    
    if not user_ids:
        print("No hay usuarios registrados.")
        return

    # 2. ASEGURAR CATEGORÍAS
    cat_names = ["Electrónica", "Hogar", "Ropa", "Deportes", "Salud"]
    for name in cat_names:
        requests.post(f"{BASE_URL}/categories", json={"name": name, "description": "Carga masiva"})
    
    res_cats = requests.get(f"{BASE_URL}/categories")
    category_ids = []
    if res_cats.status_code == 200:
        cats_data = res_cats.json()
        items = cats_data.get('content', cats_data) if isinstance(cats_data, dict) else cats_data
        category_ids = [c['id'] for c in items]

    if not category_ids:
        print("No hay categorías disponibles.")
        return

    # 3. CREAR 1000 PRODUCTOS
    print("Insertando 1000 productos...")
    success = 0
    intentos = 0

    while success < 1000 and intentos < 3000:
        intentos += 1
        
        product_base = random.choice(TECH_PRODUCTS)
        product_name = f"{product_base} {fake.catch_phrase().split()[-1].capitalize()} {random.randint(100, 9999)}"
        product_name = product_name[:145]

        p_data = {
            "name": product_name, 
            "price": round(random.uniform(10.0, 5000.0), 2),
            "description": fake.sentence(nb_words=15),
            "userId": random.choice(user_ids),
            "categoryIds": random.sample(category_ids, k=min(2, len(category_ids)))
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
