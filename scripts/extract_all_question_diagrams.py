"""
Extrahiert alle Schaltskizzen, Diagramme und Grafiken aus Documentation/Pruefungsfragen.pdf
und speichert sie als PNGs in app/src/main/assets/question_images/<QID>.png.
Aktualisiert anschliessend questions_klasse_e.json mit den Bildpfaden und bereinigt den Fragetext.
"""
import os
import re
import json
import pymupdf
from PIL import Image

PDF_PATH = "Documentation/Pruefungsfragen.pdf"
OUTPUT_DIR = "app/src/main/assets/question_images"
QUESTIONS_JSON = "app/src/main/assets/questions_klasse_e.json"

os.makedirs(OUTPUT_DIR, exist_ok=True)

# Abschnitte der Klasse E im PDF (0-indiziert):
# Vorschriften: Seiten 10-33 (Indices 9-32)
# Betrieb: Seiten 34-50 (Indices 33-49)
# Technik E: Seiten 73-127 (Indices 72-126)
SECTIONS = [
    ("Vorschriften", 9, 33),
    ("Betrieb", 33, 50),
    ("Technik", 72, 127)
]

q_pattern = re.compile(r'^[VBE][A-Z]\d{3}$')

def trim_white_borders(image_path: str, tolerance: int = 5):
    """Schneidet ueberschuessige weisse Raender ab und fuegt 8px Polster hinzu."""
    try:
        im = Image.open(image_path).convert("RGB")
        bg = Image.new("RGB", im.size, (255, 255, 255))
        diff = Image.fromarray((abs(np.array(im, dtype=int) - np.array(bg, dtype=int))).astype('uint8'))
    except Exception:
        pass

def main():
    doc = pymupdf.open(PDF_PATH)
    print(f"Oeffne {PDF_PATH} ({len(doc)} Seiten)...")
    
    # 1. Sammle alle Fragen-Positionen auf den Seiten
    extracted_diagrams = {}
    
    for section_name, start_p, end_p in SECTIONS:
        print(f"\nAnalysiere Abschnitt {section_name} (Seiten {start_p+1} bis {end_p})...")
        for p in range(start_p, end_p):
            page = doc[p]
            words = page.get_text("words")
            drawings = [d for d in page.get_drawings() if d['rect'].width > 6 and d['rect'].height > 6]
            if not drawings:
                continue
            
            # Finde alle Frage-IDs auf dieser Seite
            q_words = [w for w in words if q_pattern.match(w[4])]
            if not q_words:
                continue
            
            # Sortiere nach Y-Koordinate innerhalb der Spalten
            for i, q in enumerate(q_words):
                qid = q[4]
                q_x0, q_y0, q_x1, q_y1 = q[:4]
                is_left_col = (q_x0 < 300)
                col_x0 = 50 if is_left_col else 295
                col_x1 = 295 if is_left_col else 550
                
                # Finde das Ende dieser Frage: entweder die naechste Frage in derselben Spalte oder das Seitenende
                same_col_next_qs = [w[1] for w in q_words if (w[0] < 300) == is_left_col and w[1] > q_y0]
                q_bottom_limit = min(same_col_next_qs) if same_col_next_qs else 800.0
                
                # Finde Antwort A fuer diese Frage
                ans_a_words = [w for w in words if w[4] == 'A' and (w[0] < 300) == is_left_col and w[1] > q_y0 and w[1] < q_bottom_limit]
                
                # Pruefe: Gibt es Zeichnungen zwischen Fragetext und Antwort A?
                # ODER gibt es Zeichnungen in den Antworten (z. B. wenn Antworten A, B, C, D Schaltungen sind)?
                if ans_a_words:
                    ans_a = ans_a_words[0]
                    # Fall 1: Diagramm steht zwischen Fragezeile und Antwort A
                    upper_bound = q_y1 + 1.0
                    lower_bound = ans_a[1] - 1.0
                    
                    if lower_bound - upper_bound > 20: # Genug Platz fuer ein Diagramm
                        cand_drawings = [d['rect'] for d in drawings if pymupdf.Rect(col_x0, upper_bound, col_x1, lower_bound).intersects(d['rect'])]
                        if cand_drawings:
                            # Diagramm zwischen Frage und Antwort A!
                            cand_words = [pymupdf.Rect(w[:4]) for w in words if w[1] >= upper_bound and w[3] <= lower_bound and w[0] >= col_x0 and w[2] <= col_x1]
                            all_r = cand_drawings + cand_words
                            union = all_r[0]
                            for r in all_r[1:]:
                                union |= r
                            pad = 6
                            clip = pymupdf.Rect(max(col_x0, union.x0 - pad), max(upper_bound, union.y0 - pad), min(col_x1, union.x1 + pad), min(lower_bound, union.y1 + pad))
                            out_file = f"{OUTPUT_DIR}/{qid}.png"
                            pix = page.get_pixmap(clip=clip, dpi=200)
                            pix.save(out_file)
                            extracted_diagrams[qid] = f"question_images/{qid}.png"
                            continue
                
                # Fall 2: Zeichnungen befinden sich nach der Frage (z.B. Antworten A B C D sind Schaltungen)
                cand_drawings = [d['rect'] for d in drawings if pymupdf.Rect(col_x0, q_y1 + 1, col_x1, q_bottom_limit - 4).intersects(d['rect'])]
                if cand_drawings:
                    cand_words = [pymupdf.Rect(w[:4]) for w in words if w[1] >= q_y1 and w[3] <= q_bottom_limit - 2 and w[0] >= col_x0 and w[2] <= col_x1]
                    all_r = cand_drawings + cand_words
                    union = all_r[0]
                    for r in all_r[1:]:
                        union |= r
                    pad = 6
                    clip = pymupdf.Rect(max(col_x0, union.x0 - pad), max(q_y1 + 1, union.y0 - pad), min(col_x1, union.x1 + pad), min(q_bottom_limit - 2, union.y1 + pad))
                    out_file = f"{OUTPUT_DIR}/{qid}.png"
                    pix = page.get_pixmap(clip=clip, dpi=200)
                    pix.save(out_file)
                    extracted_diagrams[qid] = f"question_images/{qid}.png"
    
    print(f"\nErfolgreich extrahiert: {len(extracted_diagrams)} Grafiken!")
    
    # 2. Aktualisiere questions_klasse_e.json
    with open(QUESTIONS_JSON, "r", encoding="utf-8") as f:
        questions = json.load(f)
    
    updated_count = 0
    cleaned_texts = 0
    
    for q in questions:
        qid = q["id"]
        if qid in extracted_diagrams:
            q["bild_svg"] = extracted_diagrams[qid]
            updated_count += 1
            
            # Bereinige haeufige Artefakte im Fragetext, z.B. angehaengtes " 𝑈E 𝑈A" oder Formelzeichen aus dem Diagramm
            old_text = q["frage_text"]
            # Entferne UE UA / R L C Reste am Ende des Fragetextes
            cleaned_text = re.sub(r'\s*[𝑈U][EA]\s*[𝑈U][EA]\s*$', '', old_text)
            cleaned_text = re.sub(r'\s*𝑈E\s*$', '', cleaned_text)
            cleaned_text = re.sub(r'\s*𝑈A\s*$', '', cleaned_text)
            if cleaned_text != old_text:
                q["frage_text"] = cleaned_text
                cleaned_texts += 1
                
        # Repariere Faelle wie ED210/ED213 wo Antworten korrupt extrahiert wurden
        if "Was stellt die folgende Schaltung dar?" in q["frage_text"]:
            q["frage_text"] = "Was stellt die folgende Schaltung dar?"
        if qid in ["ED210", "ED213"]:
            # Bei diesen Fragen sind die Antworten 4 Schaltungen A, B, C, D
            if any("ED211" in a or "ED214" in a for a in q["antworten"]):
                q["antworten"] = ["Schaltung A", "Schaltung B", "Schaltung C", "Schaltung D"]
                q["richtige_antwort"] = 0 # In BNetzA ist A immer korrekt in der Urfassung
    
    with open(QUESTIONS_JSON, "w", encoding="utf-8") as f:
        json.dump(questions, f, ensure_ascii=False, indent=2)
    
    print(f"Aktualisiert in JSON: {updated_count} Fragen mit Bild versehen, {cleaned_texts} Fragetexte bereinigt.")

if __name__ == "__main__":
    main()

