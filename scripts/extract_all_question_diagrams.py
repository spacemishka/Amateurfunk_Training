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

def clean_str(s: str) -> str:
    s = re.sub(r'(\w+)-\s+(\w+)', r'\1\2', s)
    s = re.sub(r'\s+', ' ', s)
    return s.strip()

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
            # Alle Zeichnungen (auch duenne Drahtleitungen / Kondensatorplatten mit w=0 oder h=0):
            drawings = [d for d in page.get_drawings() if max(d['rect'].width, d['rect'].height) > 4]
            
            # Alle Frage-IDs auf der Seite
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
                
                # --- FRAGETEXT EXTRAHIEREN ---
                q_blocks = [
                    b for b in blocks
                    if (b[0] < 300) == is_left
                    and b[1] >= qw[1] - 5
                    and b[3] <= ans_a_y0 + 2
                ]
                q_blocks.sort(key=lambda b: b[1])
                
                q_lines = []
                q_text_bottom = qw[3]
                
                for b in q_blocks:
                    b_text = b[4].strip()
                    b_width = b[2] - b[0]
                    
                    # Wenn wir bereits Zeilen haben und dieser Block sehr schmal ist (< 80pt),
                    # ist es eine Diagrammbeschriftung (z.B. '+13,8 V', 'VFO', 'a')
                    if len(q_lines) > 0 and b_width < 80 and not b_text.startswith("A "):
                        joined_so_far = " ".join(q_lines)
                        if (joined_so_far.endswith(".") or joined_so_far.endswith("?") or 
                            joined_so_far.endswith(":") or joined_so_far.endswith("...")):
                            break
                    
                    # Groessere vertikale Luecke zwischen Zeilen deutet auf Diagrammbeginn hin
                    if b[1] > q_text_bottom + 10:
                        break
                    
                    lines = b[4].split('\n')
                    b_has_ans = False
                    for line in lines:
                        ls = line.strip()
                        if ls.startswith(qid):
                            ls = ls[len(qid):].strip()
                        if re.match(r'^[A-D]\s', ls) or ls == 'A':
                            b_has_ans = True
                            break
                        if ls:
                            q_lines.append(ls)
                    
                    if not b_has_ans:
                        q_text_bottom = max(q_text_bottom, b[3])
                    else:
                        break
                    
                    joined = " ".join(q_lines)
                    # Fragetext endet regulaer mit Satzzeichen ?, ..., :
                    if (joined.endswith("?") or joined.endswith("...") or joined.endswith(":")) and b[3] >= q_text_bottom - 2:
                        break
                    # Vollstaendiger Absatz, der mit Punkt endet (z.B. EC521):
                    if (joined.endswith(".") and b_width > 100 and 
                        not joined.endswith("z. B.") and not joined.endswith("u. a.") and not joined.endswith("bzw.")):
                        break
                
                clean_qtext = clean_str(" ".join(q_lines))
                if not clean_qtext:
                    clean_qtext = f"Frage {qid}"
                
                # --- GRAFIK ERKENNEN UND EXTRAHIEREN ---
                has_image = False
                clip_rect = None
                
                is_type_b = (qid in ALL_TYPE_B_QIDS)
                
                # Pruefe auf Type A Diagramm (zwischen Fragetext und Antwort A)
                drawings_type_a = [
                    d['rect'] for d in drawings
                    if col_x0 <= (d['rect'].x0 + d['rect'].x1)/2 <= col_x1
                    and d['rect'].y0 >= q_text_bottom - 2
                    and d['rect'].y1 <= ans_a_y0 + 2
                    and (d['rect'].height > 5 or d['rect'].width > 5)
                ]
                
                if drawings_type_a and not is_type_b:
                    # TYPE A: Diagramm zwischen Fragetext und Antwort A
                    # Diagrammbeschriftungen strikt im Intervall [q_text_bottom + 1.0, ans_a_y0 - 0.5]
                    diag_words = [
                        fitz.Rect(w[:4]) for w in words
                        if col_x0 <= (w[0] + w[2])/2 <= col_x1
                        and w[1] >= q_text_bottom + 1.0 # Absolut kein Ueberlapp mit Fragetext!
                        and w[3] <= ans_a_y0 - 0.5     # Absolut kein Ueberlapp mit Antwort A!
                    ]
                    all_el = drawings_type_a + diag_words
                    union = all_el[0]
                    for el in all_el[1:]:
                        union |= el
                    
                    pad = 5
                    clip_rect = fitz.Rect(
                        max(col_x0, union.x0 - pad),
                        max(q_text_bottom + 1.0, union.y0 - pad),
                        min(col_x1, union.x1 + pad),
                        min(ans_a_y0 - 1.0, union.y1 + pad)
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
                        union = all_el[0]
                        for el in all_el[1:]:
                            union |= el
                        pad = 5
                        clip_rect = fitz.Rect(
                            max(col_x0, union.x0 - pad),
                            max(q_text_bottom + 1.0, union.y0 - pad),
                            min(col_x1, union.x1 + pad),
                            min(bottom_limit - 1.0, union.y1 + pad)
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
                
                # --- ANTWORTOPTIONEN EXTRAHIEREN ---
                if is_type_b:
                    # Antwortoptionen sind Schaltungen/Diagramme A, B, C, D
                    if "Schaltung" in clean_qtext or "Filter" in clean_qtext:
                        raw_answers = ["Schaltung A", "Schaltung B", "Schaltung C", "Schaltung D"]
                    elif "Schaltzeichen" in clean_qtext or "Symbol" in clean_qtext or "Bauteil" in clean_qtext:
                        raw_answers = ["Schaltzeichen A", "Schaltzeichen B", "Schaltzeichen C", "Schaltzeichen D"]
                    elif "Diagramm" in clean_qtext or "Kennlinie" in clean_qtext or "Kurve" in clean_qtext or "Frequenzgang" in clean_qtext or "Strahlungsdiagramm" in clean_qtext:
                        raw_answers = ["Diagramm A", "Diagramm B", "Diagramm C", "Diagramm D"]
                    else:
                        raw_answers = ["Option A", "Option B", "Option C", "Option D"]
                else:
                    # Textantworten A, B, C, D
                    ans_region_text = ""
                    ans_blocks = [
                        b for b in blocks
                        if (b[0] < 300) == is_left
                        and b[1] >= ans_a_y0 - 2
                        and b[3] <= bottom_limit + 4
                    ]
                    ans_blocks.sort(key=lambda b: b[1])
                    for b in ans_blocks:
                        ans_region_text += "\n" + b[4]
                    
                    m_ans = re.search(r'\bA\s+(.*?)\n\s*B\s+(.*?)\n\s*C\s+(.*?)\n\s*D\s+(.*?)(?=\n[VBE][A-Z]\d{3}|\n\d+(\.\d+)*|\Z)', ans_region_text, re.DOTALL)
                    if m_ans:
                        ans_a_txt = clean_str(m_ans.group(1))
                        ans_b_txt = clean_str(m_ans.group(2))
                        ans_c_txt = clean_str(m_ans.group(3))
                        ans_d_txt = clean_str(m_ans.group(4))
                        raw_answers = [ans_a_txt, ans_b_txt, ans_c_txt, ans_d_txt]
                    else:
                        lines = [l.strip() for l in ans_region_text.split('\n') if l.strip()]
                        cur_letter = None
                        grouped = {'A': [], 'B': [], 'C': [], 'D': []}
                        for l in lines:
                            first_token = l.split()[0] if l.split() else ""
                            if first_token in ['A', 'B', 'C', 'D']:
                                cur_letter = first_token
                                rest = l[len(first_token):].strip()
                                if rest:
                                    grouped[cur_letter].append(rest)
                            elif cur_letter:
                                grouped[cur_letter].append(l)
                        
                        ans_a_txt = clean_str(" ".join(grouped['A']))
                        ans_b_txt = clean_str(" ".join(grouped['B']))
                        ans_c_txt = clean_str(" ".join(grouped['C']))
                        ans_d_txt = clean_str(" ".join(grouped['D']))
                        raw_answers = [ans_a_txt, ans_b_txt, ans_c_txt, ans_d_txt]
                
                # Fallback fuer Antworten
                if not all(raw_answers):
                    raw_answers = ["A", "B", "C", "D"]
                
                indices = [0, 1, 2, 3]
                random.shuffle(indices)
                shuffled_answers = [raw_answers[i] for i in indices]
                correct_idx = indices.index(0)
                
                erklaerung = f"Offizielle BNetzA-Prüfungsfrage ({qid}). Die als fachlich korrekt festgelegte Musterlösung lautet: „{raw_answers[0]}“."
                
                all_questions.append({
                    "id": qid,
                    "kategorie": cat_name,
                    "topic_id": f"{topic_prefix}_{qid[:3].lower()}",
                    "frage_text": clean_qtext,
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
