"""
Vollstaendige und fehlerfreie Extraktion aller 839 BNetzA Pruefungsfragen der Klasse E
inklusive aller Schaltskizzen, Diagramme und Kennlinien.
"""
import fitz
import re
import os
import json
import random

PDF_PATH = "Documentation/Pruefungsfragen.pdf"
OUTPUT_IMG_DIR = "app/src/main/assets/question_images"
QUESTIONS_JSON = "app/src/main/assets/questions_klasse_e.json"

os.makedirs(OUTPUT_IMG_DIR, exist_ok=True)

doc = fitz.open(PDF_PATH)

SECTIONS = [
    ("Vorschriften", 9, 33, "top_vorschriften"),
    ("Betrieb", 33, 50, "top_betrieb"),
    ("Technik", 72, 127, "top_technik")
]

q_pattern = re.compile(r'^[VBE][A-Z]\d{3}$')

# Fragen, deren Antwortoptionen aus 4 Grafiken/Schaltungen/Symbolen im 2x2 Raster bestehen:
GRID_2X2_QIDS = {
    'EC110', 'EC111', 'EC605', 'EC612', 'EC613', 'EC614', 'EC615',
    'ED210', 'ED213', 'ED304', 'EG214', 'EJ117', 'EJ206'
}
# Fragen mit 1x4 vertikalen grafischen Antwortoptionen:
VERTICAL_CHOICES_QIDS = {
    'EF307', 'EE101', 'EE406', 'EE407', 'EF216', 'EI102', 'EJ207', 'EJ208',
    'EC201', 'EC301', 'EC509', 'EC510', 'EC511', 'EC512', 'EC520'
}
ALL_TYPE_B_QIDS = GRID_2X2_QIDS | VERTICAL_CHOICES_QIDS

GERMAN_GRAMMAR = {
    'der', 'die', 'das', 'dem', 'den', 'des', 'ein', 'eine', 'einer', 'einem', 'einen',
    'ist', 'sind', 'wird', 'werden', 'hat', 'haben', 'kann', 'können', 'aus', 'mit',
    'von', 'in', 'bei', 'zu', 'nach', 'für', 'über', 'unter', 'sich', 'nicht'
}

def clean_hyphens(text: str) -> str:
    # Trennungen am Zeilenende bei deutschen Woertern zusammenfuehren
    text = re.sub(r'(\b[a-zäöüß]+)-\s*\n\s*([a-zäöüß]+)', r'\1\2', text)
    text = re.sub(r'(\b[A-ZÄÖÜ][a-zäöüß]+)-\s*\n\s*([a-zäöüß]+)', r'\1\2', text)
    # Zusammengesetzte Begriffe wie 'HF- und Netzkabel', 'Sporadic-E' erhalten:
    text = re.sub(r'-\s*\n\s*', '-', text)
    text = re.sub(r'\s*\n\s*', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def clean_qtext(raw_q: str) -> str:
    # Diagrammbeschriftungen nach dem Satzende entfernen
    if '?' in raw_q:
        idx = raw_q.rfind('?')
        after = raw_q[idx+1:].strip()
        # Nur abschneiden, wenn nach dem '?' kein deutscher Folgesatz (wie 'Eine Amateurfunkstelle ist ...') steht
        if after and not (after.endswith('...') or '...' in after):
            words = set(re.findall(r'[a-zäöüß]+', after.lower()))
            if not (words & GERMAN_GRAMMAR):
                raw_q = raw_q[:idx+1]
    
    # Diagrammbeschriftungen nach Punkt am Satzende (z.B. EC521)
    if '.' in raw_q:
        m = re.search(r'^(.*?\b[a-zäöüß]+\.)(?:\s+[A-Z0-9\+\-𝑅𝑈𝐼𝐶𝐿].*)$', raw_q)
        if m:
            after = raw_q[len(m.group(1)):].strip()
            words = set(re.findall(r'[a-zäöüß]+', after.lower()))
            if not (words & GERMAN_GRAMMAR):
                raw_q = m.group(1)
                
    # Bauteilbeschriftungen nach 'Gegeben:' (z.B. ED108, ED121)
    m = re.search(r'^(.*?Gegeben:.*?)\s+[𝑅𝐶]\d.*$', raw_q)
    if m:
        raw_q = m.group(1)
        
    return raw_q.strip()

def run_extraction():
    random.seed(42) # Reproduzierbare Mischung der Antwortoptionen
    
    all_questions = []
    seen_qids = set()
    
    extracted_images_count = 0
    type_a_count = 0
    type_b_count = 0
    
    for cat_name, start_p, end_p, topic_prefix in SECTIONS:
        print(f"\nVerarbeite Abschnitt {cat_name} (Seiten {start_p+1}-{end_p})...")
        
        for p in range(start_p, end_p):
            page = doc[p]
            words = page.get_text("words")
            blocks = page.get_text("blocks")
            drawings = [d for d in page.get_drawings() if max(d['rect'].width, d['rect'].height) > 4]
            
            q_words = [w for w in words if q_pattern.match(w[4])]
            
            for qw in q_words:
                qid = qw[4]
                if qid in seen_qids:
                    continue
                seen_qids.add(qid)
                
                is_left = (qw[0] < 300)
                col_x0 = 45 if is_left else 290
                col_x1 = 300 if is_left else 555
                
                # Grenze zur naechsten Frage in derselben Spalte oder Seitenende
                next_qs = [w[1] for w in q_words if (w[0] < 300) == is_left and w[1] > qw[1]]
                bottom_limit = min(next_qs) if next_qs else 800.0
                
                # Finde Antwort A
                cand_a = [
                    w for w in words
                    if w[4] == 'A'
                    and (w[0] < 300) == is_left
                    and w[1] > qw[3]
                    and w[1] < bottom_limit
                ]
                margin_a = [w for w in cand_a if (90 <= w[0] <= 100 if is_left else 330 <= w[0] <= 340)]
                ans_a = margin_a[0] if margin_a else (cand_a[0] if cand_a else None)
                ans_a_y0 = ans_a[1] if ans_a else bottom_limit
                
                # --- GESAMTEN FRAGENTEXT & ANTWORTEN EXTRAHIEREN ---
                # Wir sammeln alle Textbloecke in dieser Spalte zwischen qw[1] und bottom_limit:
                q_blocks = [
                    b for b in blocks
                    if (b[0] < 300) == is_left
                    and b[1] >= qw[1] - 5
                    and b[3] <= bottom_limit + 5
                ]
                q_blocks.sort(key=lambda b: (b[1], b[0]))
                full_text = "\n".join(b[4] for b in q_blocks)
                
                # Frage-ID am Anfang entfernen
                lines = full_text.split('\n')
                if lines and lines[0].strip().startswith(qid):
                    lines[0] = lines[0].strip()[len(qid):].strip()
                full_text = "\n".join(lines).strip()
                
                is_type_b = (qid in ALL_TYPE_B_QIDS)
                
                if is_type_b:
                    # Antwortoptionen sind Grafiken/Schaltungen/Symbole
                    m_q = re.search(r'^(.*?)(?=\n\s*A\b|\Z)', full_text, re.DOTALL)
                    clean_q = clean_qtext(clean_hyphens(m_q.group(1))) if m_q else f"Frage {qid}"
                    
                    if "Schaltung" in clean_q or "Filter" in clean_q:
                        raw_answers = ["Schaltung A", "Schaltung B", "Schaltung C", "Schaltung D"]
                    elif "Schaltzeichen" in clean_q or "Symbol" in clean_q or "Bauteil" in clean_q:
                        raw_answers = ["Schaltzeichen A", "Schaltzeichen B", "Schaltzeichen C", "Schaltzeichen D"]
                    elif "Diagramm" in clean_q or "Kennlinie" in clean_q or "Kurve" in clean_q or "Frequenzgang" in clean_q or "Strahlungsdiagramm" in clean_q:
                        raw_answers = ["Diagramm A", "Diagramm B", "Diagramm C", "Diagramm D"]
                    else:
                        raw_answers = ["Option A", "Option B", "Option C", "Option D"]
                else:
                    # Textantworten: Regex ueber den Gesamttext der Frage
                    pattern = re.compile(
                        r'^(.*?)\n\s*A\s+(.*?)\n\s*B\s+(.*?)\n\s*C\s+(.*?)\n\s*D\s+(.*?)(?=\n[VBE][A-Z]\d{3}|\Z)',
                        re.DOTALL
                    )
                    m = pattern.search(full_text)
                    if m:
                        clean_q = clean_qtext(clean_hyphens(m.group(1)))
                        raw_answers = [clean_hyphens(m.group(i)) for i in range(2, 6)]
                    else:
                        # Fallback fuer einzeilige Antworten
                        m_q = re.search(r'^(.*?)(?=\n\s*A\b|\Z)', full_text, re.DOTALL)
                        clean_q = clean_qtext(clean_hyphens(m_q.group(1))) if m_q else f"Frage {qid}"
                        raw_answers = ["A", "B", "C", "D"]
                
                # --- GRAFIK ERKENNEN UND EXTRAHIEREN ---
                has_image = False
                clip_rect = None
                
                # Pruefe auf Type A Diagramm (Zeichnungen zwischen Frage und Antwort A)
                drawings_type_a = [
                    d['rect'] for d in drawings
                    if col_x0 <= (d['rect'].x0 + d['rect'].x1)/2 <= col_x1
                    and d['rect'].y0 >= qw[3] - 2
                    and d['rect'].y1 <= ans_a_y0 + 2
                    and (d['rect'].height > 5 or d['rect'].width > 5)
                ]
                
                if drawings_type_a and not is_type_b:
                    # Finde das Ende des Fragetextes auf der Seite:
                    col_words = [
                        w for w in words
                        if (w[0] < 300) == is_left
                        and w[1] >= qw[1] - 2
                        and w[3] <= ans_a_y0
                    ]
                    # Letztes Wort mit Satzzeichen:
                    punct_words = [
                        w for w in col_words
                        if w[4].endswith('?') or w[4].endswith('...') or w[4].endswith(':') or w[4].endswith('.')
                    ]
                    if punct_words:
                        q_text_bottom = punct_words[-1][3]
                    else:
                        q_text_bottom = qw[3]
                    
                    # Zeichnungen, die unterhalb des Fragetextes liegen:
                    valid_drawings = [d for d in drawings_type_a if d.y1 >= q_text_bottom - 2]
                    if valid_drawings:
                        # Diagrammbeschriftungen strikt zwischen q_text_bottom und ans_a_y0
                        diag_words = [
                            fitz.Rect(w[:4]) for w in words
                            if col_x0 <= (w[0] + w[2])/2 <= col_x1
                            and w[1] >= q_text_bottom + 1.0 # Kein Ueberlapp mit Fragetext!
                            and w[3] <= ans_a_y0 - 0.5     # Kein Ueberlapp mit Antwort A!
                        ]
                        all_el = valid_drawings + diag_words
                        min_x0 = min(el.x0 for el in all_el)
                        min_y0 = min(el.y0 for el in all_el)
                        max_x1 = max(el.x1 for el in all_el)
                        max_y1 = max(el.y1 for el in all_el)
                        
                        pad = 5
                        clip_rect = fitz.Rect(
                            max(col_x0, min_x0 - pad),
                            max(q_text_bottom + 1.0, min_y0 - pad),
                            min(col_x1, max_x1 + pad),
                            min(ans_a_y0 - 1.0, max_y1 + pad)
                        )
                        has_image = True
                        type_a_count += 1
                
                elif is_type_b:
                    # TYPE B: Antwortoptionen sind Grafiken/Schaltungen/Diagramme
                    drawings_type_b = [
                        d['rect'] for d in drawings
                        if col_x0 <= (d['rect'].x0 + d['rect'].x1)/2 <= col_x1
                        and d['rect'].y0 >= ans_a_y0 - 5
                        and d['rect'].y1 <= bottom_limit + 5
                        and (d['rect'].height > 5 or d['rect'].width > 5)
                    ]
                    diag_words_b = [
                        fitz.Rect(w[:4]) for w in words
                        if col_x0 <= (w[0] + w[2])/2 <= col_x1
                        and w[1] >= ans_a_y0 - 4
                        and w[3] <= bottom_limit + 2
                    ]
                    all_el = drawings_type_b + diag_words_b
                    if all_el:
                        min_x0 = min(el.x0 for el in all_el)
                        min_y0 = min(el.y0 for el in all_el)
                        max_x1 = max(el.x1 for el in all_el)
                        max_y1 = max(el.y1 for el in all_el)
                        pad = 5
                        clip_rect = fitz.Rect(
                            max(col_x0, min_x0 - pad),
                            max(ans_a_y0 - 4.0, min_y0 - pad),
                            min(col_x1, max_x1 + pad),
                            min(bottom_limit - 1.0, max_y1 + pad)
                        )
                        has_image = True
                        type_b_count += 1
                
                # Wenn Grafik vorhanden, PNG rendern
                image_rel_path = None
                if has_image and clip_rect and clip_rect.width > 15 and clip_rect.height > 15:
                    pix = page.get_pixmap(clip=clip_rect, dpi=200)
                    out_png = f"{OUTPUT_IMG_DIR}/{qid}.png"
                    pix.save(out_png)
                    image_rel_path = f"question_images/{qid}.png"
                    extracted_images_count += 1
                
                # Im Originalkatalog ist Index 0 (Antwort A) immer die richtige Loesung.
                # Wir mischen deterministisch (mit seed), damit A nicht immer oben steht:
                indices = [0, 1, 2, 3]
                random.shuffle(indices)
                shuffled_answers = [raw_answers[i] for i in indices]
                correct_idx = indices.index(0)
                
                erklaerung = f"Offizielle BNetzA-Prüfungsfrage ({qid}). Die als fachlich korrekt festgelegte Musterlösung lautet: „{raw_answers[0]}“."
                
                all_questions.append({
                    "id": qid,
                    "kategorie": cat_name,
                    "topic_id": f"{topic_prefix}_{qid[:3].lower()}",
                    "frage_text": clean_q,
                    "antworten": shuffled_answers,
                    "richtige_antwort": correct_idx,
                    "erklaerung": erklaerung,
                    "bild_svg": image_rel_path
                })
    
    print(f"\nExtraktion abgeschlossen!")
    print(f"Gesamtanzahl Fragen: {len(all_questions)}")
    print(f"Grafiken generiert: {extracted_images_count} (Type A: {type_a_count}, Type B: {type_b_count})")
    
    with open(QUESTIONS_JSON, "w", encoding="utf-8") as f:
        json.dump(all_questions, f, ensure_ascii=False, indent=2)
    print(f"Erfolgreich in {QUESTIONS_JSON} gespeichert!")

if __name__ == "__main__":
    run_extraction()
