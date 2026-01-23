#!/usr/bin/env python3
"""
Script para probar los endpoints PAGINADOS de ProductController

Casos evaluados:
1. Paginación básica
2. Paginación con ordenamiento
3. Ordenamiento múltiple
4. Slice para performance
5. Búsqueda con filtros y paginación
6. Productos por usuario con paginación
7. Casos de error (validación y campos inválidos)

Autor: Nayeli
"""

import requests

BASE_URL = "http://localhost:8080/api/products"

# -----------------------------
# Utilidades
# -----------------------------
def test_endpoint(url, description, expected_status=[200]):
    print(f"\n[TEST] {description}")
    try:
        response = requests.get(url)
        if response.status_code in expected_status:
            print(f"[OK] Status {response.status_code}")
            try:
                data = response.json()
                if isinstance(data, dict):
                    print(f"  Claves: {list(data.keys())}")
                elif isinstance(data, list):
                    print(f"  Registros: {len(data)}")
            except:
                print("  Respuesta sin JSON")
        else:
            print(f"[ERROR] Status {response.status_code}")
            print(response.text[:200])
    except Exception as e:
        print(f"[ERROR] {str(e)}")


# -----------------------------
# CASOS DE PRUEBA
# -----------------------------

def test_basic_pagination():
    url = f"{BASE_URL}?page=0&size=5"
    test_endpoint(url, "1. Paginación básica")


def test_sorted_pagination():
    url = f"{BASE_URL}?page=1&size=10&sort=price,desc"
    test_endpoint(url, "2. Paginación con ordenamiento (price desc)")


def test_multi_sort():
    url = f"{BASE_URL}?page=0&size=5&sort=categories.name,asc&sort=price,desc"
    test_endpoint(url, "3. Ordenamiento múltiple (category asc, price desc)")


def test_slice():
    url = f"{BASE_URL}/slice?page=0&size=10&sort=createdAt,desc"
    test_endpoint(url, "4. Slice para performance")


def test_search_filters():
    url = f"{BASE_URL}/search?name=gaming&minPrice=500&page=0&size=3"
    test_endpoint(url, "5. Búsqueda con filtros y paginación")


def test_products_by_user():
    user_id = 1
    url = f"{BASE_URL}/user/{user_id}?page=0&size=5&sort=name,asc"
    test_endpoint(url, "6. Productos por usuario con paginación")


def test_validation_error():
    url = f"{BASE_URL}?page=-1&size=0"
    test_endpoint(url, "7.1 Error de validación (page=-1,size=0)", expected_status=[400])


def test_invalid_sort_field():
    url = f"{BASE_URL}?sort=invalidField,asc"
    test_endpoint(url, "7.2 Campo de ordenamiento inválido", expected_status=[400])


# -----------------------------
# EJECUCIÓN PRINCIPAL
# -----------------------------
if __name__ == "__main__":
    print("=" * 60)
    print("TEST DE ENDPOINTS PAGINADOS - PRODUCTCONTROLLER")
    print("=" * 60)

    test_basic_pagination()
    test_sorted_pagination()
    test_multi_sort()
    test_slice()
    test_search_filters()
    test_products_by_user()
    test_validation_error()
    test_invalid_sort_field()

    print("\nPruebas finalizadas")
