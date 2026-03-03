import os
import re
import time
from deep_translator import GoogleTranslator

# Configuration
BASE_BUNDLE_PATH = r"assets\bundles\bundle.properties"
BUNDLES_DIR = r"assets\bundles"

def load_properties(file_path):
    """
    Parses a .properties file into a dictionary and a list of lines (to preserve structure if needed).
    Returns: (keys_dict, lines)
    """
    props = {}
    lines = []
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        for line in lines:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            # Simple splitting by first '='
            parts = line.split('=', 1)
            if len(parts) == 2:
                key = parts[0].strip()
                value = parts[1].strip()
                props[key] = value
    except UnicodeDecodeError:
        # Fallback to latin-1 if utf-8 fails, though Mindustry usually uses UTF-8
        with open(file_path, 'r', encoding='latin-1') as f:
            lines = f.readlines()
        for line in lines:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            parts = line.split('=', 1)
            if len(parts) == 2:
                key = parts[0].strip()
                value = parts[1].strip()
                props[key] = value
                
    return props, lines

def get_target_lang(filename):
    """
    Extracts language code from filename (e.g. bundle_es.properties -> es).
    Handles mapped codes for Google Translate if necessary.
    """
    # bundle_es.properties -> es
    # bundle_zh_CN.properties -> zh-CN
    name = os.path.basename(filename)
    code = name.replace("bundle_", "").replace(".properties", "")
    
    # Map Mindustry codes to Google Translate codes
    mapping = {
        "zh_CN": "zh-CN",
        "zh_TW": "zh-TW",
        "pt_BR": "pt", # Google often auto-detects or uses 'pt' for Portuguese
        "pt_PT": "pt",
        "uk_UA": "uk",
        "fil": "tl", # Filipino -> Tagalog
        "id": "id",
        "he": "iw",
    }
    
    return mapping.get(code, code)

def main():
    print(f"Loading base bundle from {BASE_BUNDLE_PATH}...")
    base_props, _ = load_properties(BASE_BUNDLE_PATH)
    print(f"Found {len(base_props)} keys in base bundle.")
    
    files = [f for f in os.listdir(BUNDLES_DIR) if f.startswith("bundle_") and f.endswith(".properties") and f != "bundle.properties"]
    
    print(f"Found {len(files)} bundle files to process.")
    
    for filename in files:
        file_path = os.path.join(BUNDLES_DIR, filename)
        target_lang = get_target_lang(filename)
        
        print(f"\nProcessing {filename} (Target Lang: {target_lang})...")
        
        target_props, target_lines = load_properties(file_path)
        missing_keys = [k for k in base_props if k not in target_props]
        
        if not missing_keys:
            print(f"  - No missing keys.")
            continue
            
        print(f"  - Missing {len(missing_keys)} keys.")
        
        # Prepare for translation
        keys_to_translate = []
        texts_to_translate = []
        
        for key in missing_keys:
            keys_to_translate.append(key)
            texts_to_translate.append(base_props[key])
            
        # Batch translation (chunks of 50)
        translated_values = []
        chunk_size = 50
        
        try:
            translator = GoogleTranslator(source='en', target=target_lang)
            
            for i in range(0, len(texts_to_translate), chunk_size):
                chunk = texts_to_translate[i:i+chunk_size]
                print(f"    - Translating batch {i//chunk_size + 1}/{(len(texts_to_translate)-1)//chunk_size + 1}...")
                
                try:
                    results = translator.translate_batch(chunk)
                    translated_values.extend(results)
                except Exception as e:
                    print(f"    - Translation failed for batch: {e}")
                    # Fallback to English
                    translated_values.extend(chunk)
                
                time.sleep(1) # Be nice to the API
                
        except Exception as e:
            print(f"  - Error initializing translator: {e}")
            translated_values = texts_to_translate # Fallback
            
        # Append to file
        with open(file_path, 'a', encoding='utf-8') as f:
            f.write("\n\n# Auto-generated missing translations\n")
            for key, value in zip(keys_to_translate, translated_values):
                # Escape newlines if needed, though properties usually handle them
                value = value.replace("\n", "\\n")
                f.write(f"{key}={value}\n")
                
        print(f"  - Appended {len(translated_values)} keys.")

if __name__ == "__main__":
    main()
