#!/usr/bin/env python3
import csv, struct, sys, re, os, gzip, shutil, hashlib
csv.field_size_limit(2147483647)

OUTPUT_FILE = "products.bin"
NAMES_FILE = "product_names.bin"
CSV_URL = "https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz"
CSV_GZ = "en.openfoodfacts.org.products.csv.gz"
CSV_FILE = "en.openfoodfacts.org.products.csv"
RU_PREFIXES = tuple(str(p) for p in range(460, 470))

SUGAR_KEYS = ["сахар", "sugar", "глюкоз", "glucose", "фруктоз", "fructose", "сахароз", "sucrose"]
GLUTEN_KEYS = ["глютен", "gluten", "пшениц", "wheat", "ячмен", "barley", "ржи", "rye"]
LACTOSE_KEYS = ["лактоз", "lactose", "молок", "milk", "сыворотк", "whey"]
PALM_OIL_KEYS = ["пальмов", "palm oil", "palmate", "пальмитат"]
E_ADDITIVES_KEYS = ["e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"]
GMO_KEYS = ["гмо", "gmo", "генетически", "genetically", "biotech", "биотех"]
MILK_FAT_REPLACER_KEYS = ["ззж", "заменитель молочного жира", "milkfat replacer", "butterfat replacer", "бзмж"]
ARTIFICIAL_COLORS_KEYS = ["красител", "tartrazine", "quinoline", "sunset", "azorubine", "кармуазин", "тартразин"]

def download_csv(url, dest_gz, dest_csv):
    import urllib.request
    print(f"Скачиваю {url}...")
    urllib.request.urlretrieve(url, dest_gz)
    print("Распаковываю...")
    with gzip.open(dest_gz, 'rb') as f_in:
        with open(dest_csv, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)
    os.remove(dest_gz)
    print("Готово.")

def parse_nutriscore(score_str):
    try: return int(float(score_str))
    except: return 0

def detect_flags(ingredients_text, product_name):
    text = (ingredients_text + " " + product_name).lower()
    return {k: any(w in text for w in lst) for k, lst in [
        ("sugar", SUGAR_KEYS), ("gluten", GLUTEN_KEYS), ("lactose", LACTOSE_KEYS),
        ("palm_oil", PALM_OIL_KEYS), ("hazardous_e", E_ADDITIVES_KEYS),
        ("gmo", GMO_KEYS), ("milk_fat_replacer", MILK_FAT_REPLACER_KEYS),
        ("artificial_colors", ARTIFICIAL_COLORS_KEYS)
    ]}

def scale_level(val, max_val=10.0):
    return max(0, min(15, round((val / max_val) * 15.0)))

def parse_100g(field):
    try: return float(re.sub(r"[^\d.]", "", field))
    except: return 0.0

def process_csv(csv_path, output_path, names_path):
    records = []   # (barcode, rating, flags, name)
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            barcode_str = row.get("code", "").strip()
            if not barcode_str.isdigit() or len(barcode_str) != 13: continue
            if not barcode_str.startswith(RU_PREFIXES): continue
            barcode = int(barcode_str)
            name = row.get("product_name", "").strip() or "Без названия"
            # Обрезаем до 100 символов, чтобы влезло в фиксированное поле
            name = name[:100]
            rating = parse_nutriscore(row.get("nutriscore_score", ""))
            flags = detect_flags(row.get("ingredients_text", ""), name)
            sugars = parse_100g(row.get("sugars_100g", "0"))
            salt = parse_100g(row.get("salt_100g", "0"))
            fat = parse_100g(row.get("fat_100g", "0"))
            bool_flags = 0
            for i, k in enumerate(["sugar","gluten","lactose","palm_oil","hazardous_e","gmo","milk_fat_replacer","artificial_colors"]):
                if flags[k]: bool_flags |= (1 << i)
            packed = bool_flags | ((scale_level(sugars, 50.0) & 0xF) << 8) | ((scale_level(salt, 2.5) & 0xF) << 12) | ((scale_level(fat, 85.0) & 0xF) << 16)
            records.append((barcode, rating, packed, name))
    records.sort(key=lambda x: x[0])
    with open(output_path, "wb") as out, open(names_path, "wb") as nout:
        for barcode, rating, flags, name in records:
            out.write(struct.pack(">Q", barcode))
            rating = max(0, min(255, rating))
            out.write(struct.pack("<B", rating))
            out.write(struct.pack("<I", flags))
            # Записываем название: 8 байт barcode + 100 байт UTF-8 строки
            name_bytes = name.encode("utf-8")[:100]
            name_bytes += b'\x00' * (100 - len(name_bytes))
            nout.write(struct.pack(">Q", barcode))
            nout.write(name_bytes)
    print(f"✅ Создан {output_path} с {len(records)} продуктами.")
    print(f"✅ Создан {names_path} с названиями.")

if __name__ == "__main__":
    csv_input = None
    if "--csv" in sys.argv:
        idx = sys.argv.index("--csv")
        if idx+1 < len(sys.argv): csv_input = sys.argv[idx+1]
    if "-o" in sys.argv:
        idx = sys.argv.index("-o")
        if idx+1 < len(sys.argv): OUTPUT_FILE = sys.argv[idx+1]
    if csv_input is None:
        download_csv(CSV_URL, CSV_GZ, CSV_FILE)
        csv_input = CSV_FILE
    process_csv(csv_input, OUTPUT_FILE, NAMES_FILE)
