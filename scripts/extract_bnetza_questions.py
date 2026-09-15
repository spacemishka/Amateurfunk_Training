"""
Extrahiert alle offiziellen Prüfungsfragen der Klasse E aus Documentation/Pruefungsfragen.pdf
und generiert die app/src/main/assets/questions_klasse_e.json Datei.
"""
import pypdf
import re
import json
import random
import sys

def clean_text(text: str) -> str:
    if not text:
        return ""
    # Replace hyphenation at line end (e.g. "Amateur-\nfunk" -> "Amateurfunk")
    text = re.sub(r'(\w+)-\n(\w+)', r'\1\2', text)
    # Replace remaining newlines with spaces
    text = re.sub(r'\s*\n\s*', ' ', text)
    # Collapse multiple spaces
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def extract_questions():
    pdf_path = "Documentation/Pruefungsfragen.pdf"
    print(f"Lese PDF: {pdf_path} ...")
    reader = pypdf.PdfReader(pdf_path)
    
    # Vorschriften: Seite 10 - 33 (Indices 9 - 32)
    # Betrieb: Seite 34 - 50 (Indices 33 - 49)
    # Technik E: Seite 73 - 127 (Indices 72 - 126)
    
    sections = [
        ("Vorschriften", 9, 33, "top_vorschriften"),
        ("Betrieb", 33, 50, "top_betrieb"),
        ("Technik", 72, 127, "top_technik")
    ]
    
    all_questions = []
    seen_ids = set()
    random.seed(42) # Deterministic shuffle for reproducibility
    
    for cat_name, start_page, end_page, topic_prefix in sections:
        section_text_parts = []
        for p in range(start_page, end_page):
            section_text_parts.append(reader.pages[p].extract_text())
        full_text = "\n" + "\n".join(section_text_parts) + "\n"
        
        # Regex to find questions starting with ID like VA101, BA101, EA101
        pattern = re.compile(
            r'\n([VBE][A-Z]\d{3})\s+(.*?)\n\s*A\s+(.*?)\n\s*B\s+(.*?)\n\s*C\s+(.*?)\n\s*D\s+(.*?)(?=\n[VBE][A-Z]\d{3}|\n\d+(\.\d+)*|\Z)',
            re.DOTALL
        )
        
        matches = list(pattern.finditer(full_text))
        print(f"Kategorie {cat_name}: {len(matches)} Fragen gefunden.")
        
        for m in matches:
            qid = m.group(1).strip()
            if qid in seen_ids:
                continue
            
            raw_qtext = m.group(2)
            raw_a = m.group(3)
            raw_b = m.group(4)
            raw_c = m.group(5)
            raw_d = m.group(6)
            
            qtext = clean_text(raw_qtext)
            ans_a = clean_text(raw_a)
            ans_b = clean_text(raw_b)
            ans_c = clean_text(raw_c)
            ans_d = clean_text(raw_d)
            
            # Validation: All 4 answers and question text must not be empty
            if not qtext or not ans_a or not ans_b or not ans_c or not ans_d:
                continue
            
            # Answers: In official catalog, A is ALWAYS the correct answer.
            # We shuffle answers deterministically so option A isn't always at index 0.
            options = [ans_a, ans_b, ans_c, ans_d]
            indices = [0, 1, 2, 3]
            random.shuffle(indices)
            
            shuffled_answers = [options[i] for i in indices]
            correct_index = indices.index(0) # Where did original answer A land?
            
            seen_ids.add(qid)
            all_questions.append({
                "id": qid,
                "kategorie": cat_name,
                "topic_id": f"{topic_prefix}_{qid[:3].lower()}",
                "frage_text": qtext,
                "antworten": shuffled_answers,
                "richtige_antwort": correct_index,
                "erklaerung": f"Offizielle BNetzA-Prüfungsfrage ({qid}). Die als fachlich korrekt festgelegte Musterlösung lautet: „{ans_a}“.",
                "bild_svg": None
            })
            
    print(f"\nGesamtanzahl extrahierter, valider Fragen: {len(all_questions)}")
    cat_counts = {}
    for q in all_questions:
        cat_counts[q["kategorie"]] = cat_counts.get(q["kategorie"], 0) + 1
    print("Verteilung nach Kategorien:", cat_counts)
    
    output_path = "app/src/main/assets/questions_klasse_e.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(all_questions, f, ensure_ascii=False, indent=2)
    print(f"Erfolgreich gespeichert nach: {output_path}")

if __name__ == "__main__":
    extract_questions()
